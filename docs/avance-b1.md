# B1 - Personas y credenciales

Implementación de las entidades y consultas que desbloquean el flujo de acceso.
El esquema sigue siendo V1; no se modificaron migraciones.

## Contrato para acceso

- `PersonaService.buscarPorDni(dni)` devuelve una persona aunque esté inactiva o vencida.
  El flujo A4 decide entre INACTIVO, VENCIDO y AUTORIZADO; un DNI desconocido devuelve vacío.
- `CredencialService.buscarActiva(tipo, valor)` busca únicamente credenciales activas.
  Incluye su persona, disponible fuera de la transacción con `open-in-view=false`.
- La normalización NFC corresponde al flujo de lectura y de administración: ambos deben
  entregar el mismo valor canónico (hexadecimal en mayúsculas sin espacios ni dos puntos).
  El QR conserva su valor exacto y distingue mayúsculas de minúsculas.
- `PersonaService.buscarPorIds(ids)` permite resolver las personas de un reporte en lote.

`Credencial` referencia a `Persona` con `@ManyToOne` porque pertenecen al mismo módulo.
Los servicios son el punto de entrada para otros módulos. Las entidades no deben exponerse
en respuestas HTTP; B4 y B5 incorporarán los controladores y DTOs.

## Verificación

Ejecutar `./mvnw verify` con Docker disponible. Las pruebas usan PostgreSQL 16 con Testcontainers.
Cubren identificación de personas inactivas/vencidas, carga de persona junto a la credencial,
reasignación de una credencial desactivada, rechazo de credenciales activas y DNI duplicados,
vigencia invertida, distinción NFC/QR y consultas vacías o en lote.

B1 no implementa el CRUD (B4/B5), autenticación (B2/B3) ni autorización de lecturas (A4).
