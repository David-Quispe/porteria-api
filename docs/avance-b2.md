# B2 - Seguridad del panel

El panel utiliza JWT firmado y sin sesión HTTP. El identificador del usuario va en el
subject; su estado y rol se consultan en PostgreSQL en cada petición. Desactivar al usuario
o cambiarle el rol se aplica también a tokens emitidos anteriormente.

## Rutas

- Health y Swagger son públicos; Swagger continúa apagado por defecto en producción.
- `POST /api/auth/login` es público (el controlador se incorpora en B3).
- `/api/auth/me` requiere un usuario activo autenticado.
- `/api/admin/**` requiere ADMIN; `/api/portero/**` permite ADMIN y PORTERO.
- Las demás rutas quedan cerradas. A3 incorporará la autenticación propia del dispositivo;
  un JWT del panel no otorga acceso al ESP32. A7 incorporará WebSocket.

Los errores de seguridad usan `ApiError`: 401 para tokens ausentes/inválidos o usuarios
inactivos, 403 para permisos insuficientes y 503 si no se puede consultar la base.

## Configuración

`JWT_SECRET` contiene una clave aleatoria de al menos 32 bytes codificada en Base64.
Es obligatoria en producción. El perfil dev tiene una clave pública de ejemplo que nunca
debe utilizarse en producción. La duración predeterminada es 8 horas (`porteria.jwt.duracion`).
Cambiar la clave invalida todos los tokens emitidos con la anterior.

`PasswordEncoder` usa BCrypt. La entidad `Usuario` nunca se devuelve al cliente:
`UsuarioAutenticado` excluye el hash de contraseña.

## Verificación

`./mvnw verify` prueba contra PostgreSQL 16: 401/403, roles, ausencia de sesión,
desactivación, cambio de rol, usuario inexistente y separación entre panel y dispositivo.
Las pruebas unitarias verifican firma, expiración y rechazo de configuración insegura.
