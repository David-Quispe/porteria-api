# B3 - Login y usuario autenticado

Esta tarea depende de B2. Implementa `POST /api/auth/login` y `GET /api/auth/me`.
Los controladores validan y devuelven DTOs; el hash de contraseña no se expone.

## Probar en desarrollo

1. Levantar PostgreSQL con `docker compose up -d` y la API con `./mvnw spring-boot:run`.
2. Importar `postman/admin.postman_collection.json`.
3. Ejecutar **Iniciar sesión** y luego **Consultar sesión**. La colección guarda el JWT automáticamente.

El perfil `dev` crea `admin.dev` con la contraseña pública de demostración `PorteriaDev-2026!`.
La migración está en `db/dev`; no se ejecuta en producción. Si ese username ya existe,
se conserva su contraseña y su estado. No se debe habilitar el perfil dev en un despliegue público.

El login recibe `username` y `password`. Devuelve `accessToken`, `tokenType` (Bearer),
`expiresIn` (segundos) y `usuario` (id, username, nombre y rol).
Enviar `Authorization: Bearer <accessToken>` para consultar `/api/auth/me`.
Ambas respuestas llevan `Cache-Control: no-store`.

Se devuelve 400 por campos inválidos o JSON malformado, y 401 por contraseña incorrecta,
usuario inexistente o desactivado. Las contraseñas de más de 72 bytes UTF-8 se rechazan;
no se truncan al límite de BCrypt. El mensaje de rechazo no revela si el usuario existe.

## Verificación y pendientes

`./mvnw verify` comprueba el recorrido login → JWT → me, rechazos y validaciones,
además de arrancar el perfil prod contra otra base para verificar que no crea el usuario de ejemplo.

La creación del primer administrador de producción se completará con el despliegue.
El límite de intentos de login corresponde a D2. El CRUD de usuarios corresponde a B9.
