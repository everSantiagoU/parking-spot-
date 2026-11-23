# Modelo de datos — HU-05

Base de datos PostgreSQL 16. El esquema se crea con migraciones versionadas de Flyway, ubicadas en `backend/src/main/resources/db/migration`:

| Migración | Contenido |
| --- | --- |
| `V1__esquema_inicial.sql` | Tablas, llaves, restricciones e índices. |
| `V2__datos_iniciales.sql` | Roles, sede principal, franjas, tarifas y cupos iniciales. |

> Regla de Flyway: **una migración ya aplicada nunca se modifica**. Cualquier cambio va en una migración nueva (`V3__…`, `V4__…`).

## Relaciones

| Tabla origen | Llave foránea | Tabla destino | Cardinalidad |
| --- | --- | --- | --- |
| `usuarios` | `rol_id` | `roles` | Muchos usuarios → 1 rol |
| `usuarios` | `sede_id` (opcional) | `sedes` | Muchos usuarios → 0..1 sede |
| `franjas` | `sede_id` | `sedes` | 1 sede → 2 franjas (DIURNA, NOCTURNA) |
| `tarifas` | `sede_id` | `sedes` | 1 sede → muchas tarifas (una por tipo y franja) |
| `ocupacion` | `sede_id` | `sedes` | 1 sede → 2 registros (CARRO, MOTO) |
| `cobros` | `sede_id` | `sedes` | 1 sede → muchos cobros |
| `cobros` | `tarifa_id` | `tarifas` | 1 tarifa → muchos cobros |
| `pagos` | `cobro_id` (único) | `cobros` | 1 cobro → 0..1 pago |
| `pagos` | `usuario_id` | `usuarios` | 1 usuario → muchos pagos |
| `eventos` | `sede_id` | `sedes` | 1 sede → muchos eventos |
| `eventos` | `cobro_id` (opcional) | `cobros` | 1 cobro → muchos eventos |
| `eventos` | `usuario_id` (opcional) | `usuarios` | 1 usuario → muchos eventos |

## Diccionario de datos

### roles
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| nombre | VARCHAR(20) | NOT NULL, UNIQUE, solo `ADMIN` u `OPERADOR` |
| descripcion | VARCHAR(150) | |

### sedes
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE |
| direccion | VARCHAR(200) | |
| activa | BOOLEAN | NOT NULL, por defecto `true` |
| creada_en | TIMESTAMPTZ | NOT NULL, por defecto `now()` |

### usuarios
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(100) | NOT NULL (hash BCrypt, nunca la contraseña en texto plano) |
| nombre_completo | VARCHAR(120) | NOT NULL |
| rol_id | BIGINT | NOT NULL, FK → roles |
| sede_id | BIGINT | FK → sedes (el administrador puede no tener sede) |
| activo | BOOLEAN | NOT NULL, por defecto `true` |
| creado_en | TIMESTAMPTZ | NOT NULL |

### franjas
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| sede_id | BIGINT | NOT NULL, FK → sedes |
| nombre | VARCHAR(10) | `DIURNA` o `NOCTURNA`. UNIQUE (sede_id, nombre) |
| hora_inicio | TIME | NOT NULL, inclusiva |
| hora_fin | TIME | NOT NULL, exclusiva. Distinta de `hora_inicio`; puede cruzar la medianoche |

### tarifas
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| sede_id | BIGINT | NOT NULL, FK → sedes |
| tipo_vehiculo | VARCHAR(10) | `CARRO` o `MOTO` |
| franja | VARCHAR(10) | `DIURNA` o `NOCTURNA` |
| valor | NUMERIC(12,2) | ≥ 0 (la moto puede valer 0, RN-01) |
| actualizada_en | TIMESTAMPTZ | NOT NULL |

UNIQUE (sede_id, tipo_vehiculo, franja): hay una sola tarifa vigente por combinación.

### ocupacion
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| sede_id | BIGINT | PK compuesta, FK → sedes |
| tipo_vehiculo | VARCHAR(10) | PK compuesta, `CARRO` o `MOTO` |
| capacidad | INTEGER | ≥ 0 |
| ocupados | INTEGER | Entre 0 y `capacidad` |
| actualizada_en | TIMESTAMPTZ | NOT NULL |

### cobros
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| sede_id | BIGINT | NOT NULL, FK → sedes |
| placa | VARCHAR(6) | Formato `ABC123` (carro) o `ABC12D` (moto) |
| tipo_vehiculo | VARCHAR(10) | `CARRO` o `MOTO` |
| dia_operativo | DATE | NOT NULL (06:00 a 05:59 del día siguiente) |
| franja | VARCHAR(10) | Franja aplicada (copia, RN-07) |
| tarifa_id | BIGINT | NOT NULL, FK → tarifas |
| tarifa_aplicada | NUMERIC(12,2) | Valor de la tarifa al generar el cobro (copia, RN-07) |
| valor | NUMERIC(12,2) | ≥ 0 |
| estado | VARCHAR(10) | `PENDIENTE`, `PAGADO`, `VENCIDO` o `ANULADO` |
| generado_en | TIMESTAMPTZ | NOT NULL |
| vigente_hasta | TIMESTAMPTZ | Fin del pase. Obligatorio si el estado es `PAGADO` (RN-03) |

**UNIQUE (placa, sede_id, dia_operativo):** un solo cobro por placa, sede y día operativo.

### pagos
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| cobro_id | BIGINT | NOT NULL, UNIQUE, FK → cobros (un cobro, un pago) |
| medio | VARCHAR(10) | Solo `EFECTIVO` en el MVP |
| valor | NUMERIC(12,2) | > 0 |
| usuario_id | BIGINT | NOT NULL, FK → usuarios (operador que cobró) |
| clave_idempotencia | VARCHAR(64) | NOT NULL, UNIQUE (evita pagos repetidos por reintento) |
| registrado_en | TIMESTAMPTZ | NOT NULL, hora del servidor |

### eventos
| Columna | Tipo | Restricciones |
| --- | --- | --- |
| id | BIGINT | PK, identidad |
| sede_id | BIGINT | NOT NULL, FK → sedes |
| placa | VARCHAR(6) | Mismo formato que en `cobros` |
| tipo_vehiculo | VARCHAR(10) | `CARRO` o `MOTO` |
| tipo_evento | VARCHAR(10) | `INGRESO` o `SALIDA` |
| origen_lectura | VARCHAR(10) | `AUTOMATICA` o `MANUAL` |
| confianza | NUMERIC(5,2) | Entre 0 y 100 (lectura automática) |
| resultado | VARCHAR(20) | NOT NULL (por ejemplo `REGISTRADO`, `ABIERTA`, `CERRADA`) |
| motivo | VARCHAR(200) | Explicación del resultado |
| cobro_id | BIGINT | FK → cobros (opcional) |
| usuario_id | BIGINT | FK → usuarios. Obligatorio si `origen_lectura = MANUAL` |
| ocurrido_en | TIMESTAMPTZ | NOT NULL, hora del servidor |

## Datos iniciales (V2)

- Roles `ADMIN` y `OPERADOR`.
- Sede `Sede Principal`.
- Franjas: DIURNA de 06:00 a 18:00 y NOCTURNA de 18:00 a 06:00.
- Tarifas: carro $10.000 diurna y $6.000 nocturna; moto $0 en ambas franjas.
- Cupos: 50 carros y 30 motos.

Estos valores son iniciales: el administrador los cambia en HU-12. Los usuarios iniciales se crean en HU-06, porque requieren el hash BCrypt de la contraseña.
