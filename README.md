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
- Base de datos: `localhost:5432`, usuario y contraseña del `.env`. Flyway crea las 6 tablas al arrancar.

## Autenticación del panel (B2 y B3)

En desarrollo, Flyway crea el usuario `admin.dev` con contraseña de demostración `PorteriaDev-2026!`.
Importa `postman/admin.postman_collection.json` para probar login y `/api/auth/me`.
Consulta [el contrato de login](docs/avance-b3.md) y [la configuración de JWT](docs/avance-b2.md).
En producción es obligatorio `JWT_SECRET`: al menos 32 bytes aleatorios codificados en Base64.
El usuario de ejemplo y la clave predeterminada pertenecen exclusivamente al perfil `dev`.

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
| `SWAGGER_ENABLED` | prod | `true` para mostrar Swagger en producción (por defecto `false`) |

## Problemas frecuentes

| Síntoma | Solución |
| --- | --- |
| `docker compose up` dice que el puerto 5432 está ocupado | Ya tienes otro PostgreSQL. Cambia `DB_PORT` en `.env` (por ejemplo a `5433`) |
| `mvn verify` falla con *Could not find a valid Docker environment* | Enciende Docker Desktop |
| Lombok o MapStruct no generan código en IntelliJ | Settings → Build → Compiler → Annotation Processors → *Enable annotation processing* |
