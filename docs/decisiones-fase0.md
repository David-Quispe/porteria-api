# Decisiones de la Fase 0

**Estado:** propuesta de Dev A (David), pendiente de revisar con Dev B en la sesión de Fase 0.
Lo que se cambie aquí se cambia también en `V1__esquema_inicial.sql` **antes** de fusionarla en `develop`. Después ya no se edita.

## 1. Versiones

| Qué | Versión | Por qué |
| --- | --- | --- |
| Spring Boot | 4.1.x | start.spring.io ya no ofrece 3.x |
| Java (código) | 21 | `<java.version>21</java.version>`; compila igual con JDK 21 o 25 |
| springdoc-openapi | 3.x | La 2.x es para Boot 3 |
| PostgreSQL | 16 | Igual en `docker-compose.yml` y en Testcontainers |

## 2. Resultados de una lectura

| Resultado | Cuándo | ESP32 |
| --- | --- | --- |
| `AUTORIZADO` | Pasa todas las validaciones | LED verde, abre barrera |
| `NO_AUTORIZADO` | Nadie tiene esa credencial activa (o DNI no registrado) | LED rojo, buzzer |
| `INACTIVO` | La persona existe pero fue desactivada | LED rojo, buzzer |
| `VENCIDO` | Hoy está fuera de su vigencia | LED rojo, buzzer |
| `FUERA_DE_HORARIO` | Ninguna regla de acceso cubre este día y hora (solo en ENTRADA) | LED rojo, buzzer |

## 3. Secuencia de validación (A4)

0. **Lectura duplicada:** si el mismo dispositivo leyó el mismo método y valor hace menos de 5 s, se responde el resultado anterior y **no** se guarda otro registro.
1. **Identificar a la persona:**
   - NFC o QR → `CredencialService.buscarActiva(tipo, valor)`
   - DNI → `PersonaService.buscarPorDni(dni)`
   - No se encuentra → `NO_AUTORIZADO` (se guarda con `persona_id` en NULL).
2. `persona.activo = false` → `INACTIVO`.
3. Hoy fuera de `[vigencia_inicio, vigencia_fin]` → `VENCIDO`.
4. Solo si es `ENTRADA`: si existen reglas activas para su tipo y punto, y ninguna cubre el día y la hora actuales → `FUERA_DE_HORARIO`. Si no hay reglas, no hay restricción. Nadie se queda encerrado por horario: la `SALIDA` no valida reglas.
5. `AUTORIZADO`.

Salvo la lectura duplicada, todo se guarda en `registro_acceso` y publica `AccesoRegistradoEvent`.

## 4. Modelo de datos

| Tabla | Decisiones |
| --- | --- |
| `persona` | `dni` único y obligatorio. Vigencia en `vigencia_inicio` / `vigencia_fin` (NULL = sin vencimiento). Borrado lógico con `activo`. Un visitante que vuelve reutiliza su fila y se le extiende la vigencia |
| `credencial` | Solo `NFC` y `QR`; el DNI no se duplica aquí. Índice único **parcial** `(tipo, valor) WHERE activa`: un sticker desactivado puede reasignarse. "Eliminar" es un DELETE real: el historial conserva `valor_leido` y la FK pasa a NULL |
| `dispositivo` | `punto` = `PEATONAL` o `VEHICULAR`. Se guarda solo `token_hash` (SHA-256). `ultimo_ping` lo actualiza `/ping` |
| `regla_acceso` | Tipo de persona + punto (NULL = todos) + días ISO (`'1,2,3,4,5'`) + `hora_inicio`/`hora_fin`. Un horario que cruza la medianoche son dos reglas |
| `registro_acceso` | `persona_id` puede ser NULL. Siempre guarda `metodo`, `valor_leido`, `direccion` y `resultado`. Índices por fecha, por persona y para el filtro de duplicados |
| `usuario` | `rol` = `ADMIN` o `PORTERO`. Contraseña con BCrypt desde B2 |

Todos los enums se guardan como texto (`@Enumerated(EnumType.STRING)`) y la base los valida con `CHECK`. Agregar un valor nuevo requiere una migración que actualice ese `CHECK`.

**Dónde vive cada enum** (para no crear dependencias circulares):

| Enum | Módulo |
| --- | --- |
| `TipoPersona`, `TipoCredencial` | `persona` |
| `Punto` | `dispositivo` |
| `MetodoId`, `Direccion`, `Resultado` | `acceso` |
| `Rol` | `auth` |

**Referencias entre módulos** (propuesta de Dev A en A2, pendiente de OK de Dev B):

- Una entidad apunta a otra **de su mismo módulo** con `@ManyToOne` (por ejemplo `Credencial` → `Persona`).
- Una entidad apunta a otra **de otro módulo** solo por su id, como `Long` (por ejemplo `RegistroAcceso.personaId`). La clave foránea sigue en la base; lo que se evita es que el código de un módulo dependa de las entidades de otro.
- Para `acceso` no cambia nada: al validar ya tiene la `Persona` en memoria, así que el nombre para la respuesta y para el WebSocket sale de ahí.
- Para `reporte` (B6): filtra por `personaId` con Specifications y pide los nombres a `PersonaService` en un solo viaje (por ejemplo `buscarPorIds(Set<Long>)`), no uno por fila.

## 5. Contrato con el ESP32

Es la frontera entre la API y el firmware; cambiarlo obliga a cambiar los dos.

### `POST /api/dispositivo/lecturas`

Header: `X-Device-Token: <token>`

```json
{
  "metodo": "NFC",
  "valor": "04A1B2C3D4E5F6",
  "direccion": "ENTRADA"
}
```

- `metodo`: `NFC`, `QR` o `DNI`. Obligatorio.
- `valor`: obligatorio, máximo 100 caracteres. En NFC la API lo normaliza: mayúsculas, sin `:` ni espacios.
- `direccion`: `ENTRADA` o `SALIDA`. Obligatoria: la manda siempre el ESP32 (fija en el firmware v1, con botón en el v2).

Respuesta `200`:

```json
{
  "resultado": "AUTORIZADO",
  "abrir": true,
  "nombre": "Juan Pérez"
}
```

- `abrir` es `true` solo con `AUTORIZADO`; el firmware solo mira este campo para el LED y la barrera.
- `nombre` es `null` cuando la persona no se identificó.

Errores: `401` si el token falta, es falso o el dispositivo está desactivado; `400` si el JSON es inválido (cuerpo `ApiError`). Sin respuesta en 3 s, el ESP32 prende el LED ámbar.

### `POST /api/dispositivo/ping`

Mismo header y sin cuerpo. Actualiza `ultimo_ping` y responde:

```json
{ "ok": true, "hora": "2026-10-01T08:00:00" }
```

## 6. Token de dispositivo

- Al registrar o regenerar: 32 bytes aleatorios (`SecureRandom`) en Base64URL. Se muestran **una sola vez**.
- En la base solo va el SHA-256 en hex. BCrypt no sirve aquí, porque `DeviceTokenFilter` tiene que **buscar** el dispositivo por su token, y BCrypt da un hash distinto cada vez.

## 7. Turno del portero (A8)

Los turnos son fijos y se configuran en `application.yml`:

```yaml
porteria:
  turnos: "06:00,14:00,22:00"
```

`GET /api/portero/turno` devuelve los registros desde el inicio del turno actual.

## 8. Pendiente de decidir con Dev B

- **Visitantes:** solo el ADMIN crea personas. Propuesta: en la Fase 2, `POST /api/portero/visitantes` (módulo `persona`) crea un `VISITANTE` con vigencia de un día, y entra con su DNI sin necesitar sticker.
- **Reporte y repositorios:** se propone que `reporte` lea `RegistroAccesoRepository` solo para consultas (Specifications), como única excepción a la regla 4.3. A2 hace que ese repositorio extienda `JpaSpecificationExecutor`.
- **Referencias entre módulos por id** (§4): A2 ya lo aplica en `RegistroAcceso`. Si Dev B prefiere `@ManyToOne` hacia `Persona`, se cambia antes de A4.

## 9. Convenciones

- **Zona horaria:** la app fija `America/Lima` al arrancar (`PorteriaApplication`). Todas las fechas son `LocalDateTime` / columnas `TIMESTAMP`.
- **Migraciones nuevas:** `V<yyyyMMdd_HHmm>__descripcion.sql` (por ejemplo `V20261005_1430__indice_persona_nombre.sql`). En dev, `out-of-order` permite fusionar fechas cruzadas.
- **Datos de prueba:** en `db/dev/`, solo se ejecutan con el perfil `dev`.
