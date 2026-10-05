# Diagramas

GitHub dibuja estos diagramas directamente (Mermaid). Para el informe se pueden exportar como imagen desde [mermaid.live](https://mermaid.live) pegando el bloque de código.

## 1. Arquitectura

```mermaid
flowchart LR
    esp["ESP32<br/>PN532 · GM65 · LEDs"]
    pantalla["Pantalla del portero"]
    admin["Panel admin"]

    subgraph api["API Spring Boot · monolito modular"]
        direction TB
        modulos["dispositivo · acceso · persona<br/>auth · reporte"]
    end

    db[("PostgreSQL 16")]

    esp -- "lecturas · HTTP + X-Device-Token" --> api
    pantalla -- "HTTP + JWT (PORTERO)" --> api
    admin -- "HTTP + JWT (ADMIN)" --> api
    api -. "WebSocket /topic/accesos" .-> pantalla
    api --> db
```

- El ESP32 se autentica con el token de su dispositivo; las personas (portero y admin), con JWT.
- La pantalla del portero recibe cada acceso al instante por WebSocket, sin recargar.
- Por dentro, la API está dividida en módulos con un dueño cada uno. Sus dependencias están en la guía, sección 4.

## 2. Modelo entidad-relación (V1)

```mermaid
erDiagram
    PERSONA ||--o{ CREDENCIAL : "tiene"
    PERSONA |o--o{ REGISTRO_ACCESO : "identificada en"
    CREDENCIAL |o--o{ REGISTRO_ACCESO : "leída en"
    DISPOSITIVO ||--o{ REGISTRO_ACCESO : "registra"

    PERSONA {
        bigint id PK
        varchar tipo "ESTUDIANTE, DOCENTE, PERSONAL, VISITANTE"
        varchar dni UK "también sirve como credencial"
        varchar nombres
        varchar apellidos
        varchar codigo UK "alumno o empleado; opcional"
        varchar area
        varchar foto_url
        date vigencia_inicio
        date vigencia_fin "NULL = sin vencimiento"
        boolean activo "borrado lógico"
    }
    CREDENCIAL {
        bigint id PK
        bigint persona_id FK
        varchar tipo "NFC, QR"
        varchar valor "único entre las activas"
        boolean activa
    }
    DISPOSITIVO {
        bigint id PK
        varchar nombre
        varchar punto "PEATONAL, VEHICULAR"
        varchar token_hash UK "SHA-256"
        boolean activo
        timestamp ultimo_ping
    }
    REGISTRO_ACCESO {
        bigint id PK
        timestamp fecha_hora
        bigint dispositivo_id FK
        bigint persona_id FK "NULL si nadie tiene la credencial"
        bigint credencial_id FK "pasa a NULL si se elimina"
        varchar metodo "NFC, QR, DNI"
        varchar valor_leido
        varchar direccion "ENTRADA, SALIDA"
        varchar resultado "6 resultados"
    }
    REGLA_ACCESO {
        bigint id PK
        varchar tipo_persona
        varchar punto "NULL = todos"
        varchar dias "1=lunes ... 7=domingo"
        time hora_inicio
        time hora_fin
        boolean activa
    }
    USUARIO {
        bigint id PK
        varchar username UK
        varchar password_hash "BCrypt"
        varchar nombre
        varchar rol "ADMIN, PORTERO"
        boolean activo
    }
```

- Las columnas `creado_en` y `actualizado_en` se omiten para que el diagrama se lea mejor.
- `regla_acceso` no tiene claves foráneas: se aplica por `tipo_persona` y `punto`.
- `usuario` son las cuentas del sistema (porteros y admins), no las personas que pasan por la puerta.
- El detalle y el porqué de cada columna están en [decisiones-fase0.md](decisiones-fase0.md) §4.

## 3. Una lectura, del sticker al LED

```mermaid
sequenceDiagram
    autonumber
    participant E as ESP32
    participant F as DeviceTokenFilter
    participant S as AccesoService
    participant P as PersonaService / CredencialService
    participant R as ReglaService
    participant DB as PostgreSQL
    participant W as Pantalla del portero

    E->>F: POST /api/dispositivo/lecturas<br/>X-Device-Token + {metodo, valor, direccion}
    F->>DB: dispositivo activo con ese SHA-256
    alt token falso o dispositivo desactivado
        F-->>E: 401
    else token válido
        F->>S: registrar lectura
        S->>DB: ¿misma lectura en los últimos 5 s?
        alt lectura duplicada
            S-->>E: mismo resultado, sin guardar otra vez
        else lectura nueva
            S->>P: identificar persona (credencial o DNI)
            Note over S: revisa INACTIVO y VENCIDO
            S->>R: ¿alguna regla cubre este día y hora? (solo ENTRADA)
            S->>DB: INSERT registro_acceso
            S--)W: AccesoRegistradoEvent por WebSocket (después del commit)
            S-->>E: {resultado, abrir, nombre}
        end
    end
    E->>E: LED verde o rojo y buzzer
```

1. El filtro identifica al dispositivo por el hash de su token; el token nunca se guarda en claro.
2. Una lectura repetida en menos de 5 s (sticker apoyado) devuelve el resultado anterior sin crear otro registro, salvo la segunda entrada tras una entrada autorizada: se registra una alerta `ENTRADA_REPETIDA` y no se abre. Sus rebotes posteriores sí se filtran.
3. Los resultados posibles y su orden de evaluación están en [decisiones-fase0.md](decisiones-fase0.md) §2 y §3.
4. El evento al WebSocket sale **después** del commit: la pantalla nunca muestra un acceso que no quedó guardado.
5. El ESP32 solo mira `abrir`. Si la API no responde en 3 s, prende el LED ámbar.
