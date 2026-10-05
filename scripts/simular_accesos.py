#!/usr/bin/env python3
"""Simula lecturas del ESP32 contra la API local con los datos ficticios de dev."""

import argparse
import json
import os
import sys
from dataclasses import dataclass
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


@dataclass(frozen=True)
class Escenario:
    nombre: str
    descripcion: str
    metodo: str
    valor: str
    dispositivo: str


ESCENARIOS = {
    "estudiante": Escenario(
        "estudiante", "Lucia Quispe · puerta peatonal · sticker NFC", "NFC", "04AA0001", "peatonal"
    ),
    "docente": Escenario(
        "docente", "Sofia Torres · puerta peatonal · código QR", "QR", "QR-DEMO-06", "peatonal"
    ),
    "vehiculo": Escenario(
        "vehiculo", "Martin Rivera · puerta vehicular · DNI del conductor", "DNI", "90000011", "vehicular"
    ),
}


class ErrorSimulacion(Exception):
    pass


def solicitar(base_url, ruta, *, metodo="GET", cuerpo=None, token=None, device_token=None):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    headers = {"Accept": "application/json"}
    if datos is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if device_token:
        headers["X-Device-Token"] = device_token
    request = Request(base_url + ruta, data=datos, headers=headers, method=metodo)
    try:
        with urlopen(request, timeout=8) as respuesta:
            contenido = respuesta.read()
            return json.loads(contenido) if contenido else None
    except HTTPError as error:
        contenido = error.read().decode("utf-8", errors="replace")
        try:
            mensaje = json.loads(contenido).get("message", contenido)
        except json.JSONDecodeError:
            mensaje = contenido
        raise ErrorSimulacion(f"HTTP {error.code} en {ruta}: {mensaje}") from error
    except URLError as error:
        raise ErrorSimulacion(
            f"No se pudo conectar a {base_url}. Levanta PostgreSQL y la API antes de simular: {error.reason}"
        ) from error


def main():
    # PowerShell puede iniciar Python con cp1252, que no representa las señales del flujo.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Simula accesos de Portería ATENDY (solo perfil dev).")
    parser.add_argument(
        "escenario", nargs="?", choices=["todos", *ESCENARIOS], default="todos",
        help="tipo de acceso; por defecto ejecuta los tres",
    )
    parser.add_argument("--direccion", choices=["ENTRADA", "SALIDA"], default="ENTRADA")
    parser.add_argument("--base-url", default="http://localhost:8080", help="API local (por defecto localhost:8080)")
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    parsed = urlparse(base_url)
    if parsed.scheme != "http" or parsed.hostname not in {"localhost", "127.0.0.1", "::1"}:
        parser.error("Este simulador usa credenciales públicas de dev y solo acepta una API HTTP local.")

    username = os.getenv("PORTERIA_DEMO_USERNAME", "admin.dev")
    password = os.getenv("PORTERIA_DEMO_PASSWORD", "PorteriaDev-2026!")
    tokens = {
        "peatonal": os.getenv("PORTERIA_TOKEN_PEATONAL", "porteria-dev-peatonal-2026"),
        "vehicular": os.getenv("PORTERIA_TOKEN_VEHICULAR", "porteria-dev-vehicular-2026"),
    }

    print("Portería ATENDY · simulador de accesos (perfil dev)")
    print(f"API: {base_url} | Dirección: {args.direccion}\n")
    try:
        login = solicitar(
            base_url, "/api/auth/login", metodo="POST", cuerpo={"username": username, "password": password}
        )
        jwt = login["accessToken"]
        print(f"1. Sesión de consulta: {login['usuario']['username']} ✓")
        ultimo = solicitar(base_url, "/api/portero/ultima", token=jwt)
        anterior_id = ultimo["id"] if ultimo else None
        seleccion = ESCENARIOS.values() if args.escenario == "todos" else [ESCENARIOS[args.escenario]]

        for numero, escenario in enumerate(seleccion, start=2):
            print(f"\n{numero}. {escenario.descripcion}")
            print(f"   ESP32 simulado → POST /api/dispositivo/lecturas ({escenario.metodo}, {args.direccion})")
            lectura = solicitar(
                base_url,
                "/api/dispositivo/lecturas",
                metodo="POST",
                cuerpo={"metodo": escenario.metodo, "valor": escenario.valor, "direccion": args.direccion},
                device_token=tokens[escenario.dispositivo],
            )
            senal = "ABRIR barrera / LED verde" if lectura["abrir"] else "NO abrir / LED rojo"
            print(f"   API → {lectura['resultado']} | {senal} | {lectura.get('nombre') or 'sin persona asociada'}")
            registro = solicitar(base_url, "/api/portero/ultima", token=jwt)
            if registro and registro["id"] != anterior_id:
                if (
                    registro["metodo"] != escenario.metodo
                    or registro["direccion"] != args.direccion
                    or registro["resultado"] != lectura["resultado"]
                    or registro["nombre"] != lectura.get("nombre")
                ):
                    raise ErrorSimulacion(
                        "La última entrada del historial pertenece a otra lectura; "
                        "vuelve a ejecutar sin otros dispositivos enviando datos a la vez."
                    )
                anterior_id = registro["id"]
                print(
                    f"   Historial → registro #{registro['id']} · {registro['fechaHora']} · "
                    f"{registro['metodo']} · {registro['resultado']} ✓"
                )
            else:
                print("   Historial → sin registro nuevo (la API deduplica la misma lectura durante 5 s)")
            if lectura["resultado"] == "FUERA_DE_HORARIO":
                print("   La regla horaria rechazó esta ENTRADA; puedes probar --direccion SALIDA.")

        print("\nAbre http://localhost:5173/turno para ver los eventos en la interfaz.")
        return 0
    except (ErrorSimulacion, KeyError, TypeError) as error:
        print(f"\nError: {error}", file=sys.stderr)
        print("Revisa que la API esté en perfil dev y que los datos/tokens demo sigan activos.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
