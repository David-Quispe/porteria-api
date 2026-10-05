# API de Portería

[![CI](https://github.com/David-Quispe/porteria-api/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/David-Quispe/porteria-api/actions/workflows/ci.yml)

API en Spring Boot que identifica en portería a estudiantes, docentes, personal y visitantes con sticker NFC, código QR o DNI. Un ESP32 lee la credencial y la envía a la API. La API valida, registra el acceso, responde al circuito y avisa en tiempo real a la pantalla del portero.

- **Cómo trabajamos en equipo:** [docs/guia-equipo.md](docs/guia-equipo.md)
- **Decisiones de diseño y contrato con el ESP32:** [docs/decisiones-fase0.md](docs/decisiones-fase0.md)
- **Diagramas (arquitectura, base de datos, flujo de una lectura):** [docs/diagramas.md](docs/diagramas.md)

## Requisitos

| Herramienta | Versión |
| --- | --- |
| JDK Temurin | 21 o 25 |
| Docker Desktop | Última |
| IntelliJ IDEA | Última, con *Annotation processing* activado |

Maven no hace falta: el proyecto trae `mvnw`.

## Levantar el proyecto

```bash
git clone git@github.com:<usuario>/porteria-api.git
cd porteria-api
cp .env.example .env
docker compose up -d
./mvnw spring-boot:run
```

En IntelliJ también se puede ejecutar `PorteriaApplication` directamente. Arranca con el perfil `dev` y lee el mismo `.env` que Docker.

Para comprobar que todo funciona:

- Salud: <http://localhost:8080/actuator/health> responde `UP`.
- Swagger: <http://localhost:8080/swagger-ui.html>
- Base de datos: `localhost:5432`, usuario y contraseña del `.env`. Flyway crea el esquema al arrancar.

## Autenticación del panel (B2 y B3)

En desarrollo, Flyway crea el usuario `admin.dev` con contraseña de demostración `PorteriaDev-2026!`.
El perfil dev también carga 20 personas ficticias (cinco de cada tipo), sus credenciales,
dos dispositivos y reglas de demostración. El token de la puerta peatonal es
`porteria-dev-peatonal-2026`; el vehicular, `porteria-dev-vehicular-2026`.
Para probar el circuito, importa `postman/porteria.postman_environment.json` y las
colecciones `postman/admin.postman_collection.json` y
`postman/dispositivo.postman_collection.json`. Ejecuta primero «Iniciar sesión»;
el JWT se guarda automáticamente en el entorno.

Para el ESP32, copia `firmware/esp32-porteria/include/secrets.example.h` como
`firmware/esp32-porteria/include/secrets.h`. Configura WiFi, `API_BASE_URL` con
la IP de la máquina que ejecuta la API (no `localhost`) y `DEVICE_TOKEN` con el
token de la puerta peatonal demo o uno nuevo creado desde el panel. Después,
desde `firmware/esp32-porteria`, ejecuta `pio run -t upload` y
`pio device monitor`. El firmware v1 lee NFC; QR y DNI se pueden ensayar con
la colección Postman mientras se desarrolla el hardware v2. Consulta el
[cableado PN532](firmware/esp32-porteria/README.md) antes de conectar el lector.
Consulta [el contrato de login](docs/avance-b3.md) y [la configuración de JWT](docs/avance-b2.md).
En producción es obligatorio `JWT_SECRET`: al menos 32 bytes aleatorios codificados en Base64.
El usuario de ejemplo y la clave predeterminada pertenecen exclusivamente al perfil `dev`.

El panel puede conectarse a `ws://localhost:8080/ws` con STOMP. Debe enviar
`Authorization: Bearer <JWT>` en el frame `CONNECT` y suscribirse a `/topic/accesos`.
La API envía cada acceso después de confirmar su transacción. Un cambio de contraseña
revoca también la sesión WebSocket. `FRONTEND_ORIGIN` define el único origen web permitido.
Los límites por instancia (Bucket4j) son 5 intentos de login por IP y 30 lecturas por dispositivo
por minuto; al superarlos la API devuelve HTTP 429 y `Retry-After`.

## Producción con Docker

```bash
cp .env.prod.example .env.prod
# Edita .env.prod con secretos propios y el origen real del frontend.
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

La base no publica su puerto. Las fotos y la base usan volúmenes persistentes.
En una base vacía, configura `BOOTSTRAP_ADMIN_USERNAME` y
`BOOTSTRAP_ADMIN_PASSWORD` (12 a 72 bytes UTF-8); se crea un único ADMIN.
Una base ya inicializada no modifica las cuentas existentes. Usa HTTPS delante
de la API para proteger JWT y tokens de dispositivo en tránsito. Los límites de
peticiones residen en memoria de cada instancia; para varias réplicas necesitan
un almacén compartido o un límite en el proxy.

## Pruebas

```bash
./mvnw verify
```

Las pruebas levantan su propio PostgreSQL con Testcontainers, así que solo necesitan Docker Desktop encendido. No usan la base de `docker compose`.

En GitHub, el CI (`.github/workflows/ci.yml`) corre lo mismo en cada PR con JDK 21 y además compila el firmware.

## Estructura

```
src/main/java/pe/tecsup/porteria/
├── shared/        config, security, exception, dto (código común)
├── auth/          usuarios, login, JWT                       · Dev B
├── persona/       personas y credenciales                    · Dev B
├── dispositivo/   ESP32 registrados y sus tokens             · Dev A
├── acceso/        validación de lecturas y registro          · Dev A
└── reporte/       historial y exportación a Excel            · Dev B
src/main/resources/db/
├── migration/     esquema (todos los perfiles)
└── dev/           datos de prueba (solo perfil dev)
```

## Variables de entorno

| Variable | Perfil | Para qué |
| --- | --- | --- |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` | dev | Base local; vienen del `.env` |
| `SPRING_PROFILES_ACTIVE` | prod | Poner `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | prod | Obligatorias; sin ellas la app no arranca |
| `JWT_SECRET` | prod | Clave de firma obligatoria: al menos 32 bytes aleatorios en Base64 |
| `FRONTEND_ORIGIN` | prod | Origen exacto autorizado para CORS y WebSocket |
| `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD` | prod | Administrador inicial, obligatorio si la base está vacía |
| `SWAGGER_ENABLED` | prod | `true` para mostrar Swagger en producción (por defecto `false`) |

## Problemas frecuentes

| Síntoma | Solución |
| --- | --- |
| `docker compose up` dice que el puerto 5432 está ocupado | Ya tienes otro PostgreSQL. Cambia `DB_PORT` en `.env` (por ejemplo a `5433`) |
| `mvn verify` falla con *Could not find a valid Docker environment* | Enciende Docker Desktop |
| Lombok o MapStruct no generan código en IntelliJ | Settings → Build → Compiler → Annotation Processors → *Enable annotation processing* |
