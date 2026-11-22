-- =====================================================================
-- HU-05 · Esquema inicial de Parking Spot
-- Tablas: roles, sedes, usuarios, franjas, tarifas, ocupacion,
--         cobros, pagos y eventos.
-- Convenciones: nombres en español y snake_case, llaves BIGINT de
-- identidad, fechas con zona horaria (TIMESTAMPTZ) y valores en pesos
-- NUMERIC(12,2).
-- =====================================================================

-- ---------------------------------------------------------------------
-- Roles del sistema (RBAC). Solo existen ADMIN y OPERADOR.
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(20)  NOT NULL,
    descripcion VARCHAR(150),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre),
    CONSTRAINT ck_roles_nombre CHECK (nombre IN ('ADMIN', 'OPERADOR'))
);

-- ---------------------------------------------------------------------
-- Sedes del parqueadero.
-- ---------------------------------------------------------------------
CREATE TABLE sedes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    direccion  VARCHAR(200),
    activa     BOOLEAN      NOT NULL DEFAULT TRUE,
    creada_en  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_sedes_nombre UNIQUE (nombre)
);

-- ---------------------------------------------------------------------
-- Usuarios. La contraseña se guarda solo como hash (BCrypt, HU-06).
-- El operador pertenece a una sede; el administrador puede no tenerla.
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    rol_id          BIGINT       NOT NULL,
    sede_id         BIGINT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT fk_usuarios_rol  FOREIGN KEY (rol_id)  REFERENCES roles (id),
    CONSTRAINT fk_usuarios_sede FOREIGN KEY (sede_id) REFERENCES sedes (id)
);

-- ---------------------------------------------------------------------
-- Franjas horarias por sede (DIURNA y NOCTURNA).
-- hora_inicio es inclusiva y hora_fin exclusiva. Una franja puede
-- cruzar la medianoche (por ejemplo, 18:00 a 06:00).
-- ---------------------------------------------------------------------
CREATE TABLE franjas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id     BIGINT      NOT NULL,
    nombre      VARCHAR(10) NOT NULL,
    hora_inicio TIME        NOT NULL,
    hora_fin    TIME        NOT NULL,
    CONSTRAINT uq_franjas_sede_nombre UNIQUE (sede_id, nombre),
    CONSTRAINT fk_franjas_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
    CONSTRAINT ck_franjas_nombre CHECK (nombre IN ('DIURNA', 'NOCTURNA')),
    CONSTRAINT ck_franjas_horas  CHECK (hora_inicio <> hora_fin)
);

-- ---------------------------------------------------------------------
-- Tarifa vigente por sede, tipo de vehículo y franja.
-- RN-01: la moto puede tener valor 0. Cuando el administrador cambia
-- una tarifa, los cobros ya generados conservan su valor (RN-07).
-- ---------------------------------------------------------------------
CREATE TABLE tarifas (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id        BIGINT        NOT NULL,
    tipo_vehiculo  VARCHAR(10)   NOT NULL,
    franja         VARCHAR(10)   NOT NULL,
    valor          NUMERIC(12,2) NOT NULL,
    actualizada_en TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_tarifas_sede_tipo_franja UNIQUE (sede_id, tipo_vehiculo, franja),
    CONSTRAINT fk_tarifas_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
    CONSTRAINT ck_tarifas_tipo   CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_tarifas_franja CHECK (franja IN ('DIURNA', 'NOCTURNA')),
    CONSTRAINT ck_tarifas_valor  CHECK (valor >= 0)
);

-- ---------------------------------------------------------------------
-- Cupos y ocupación actual por sede y tipo de vehículo.
-- La base de datos nunca permite más ocupados que la capacidad.
-- ---------------------------------------------------------------------
CREATE TABLE ocupacion (
    sede_id        BIGINT      NOT NULL,
    tipo_vehiculo  VARCHAR(10) NOT NULL,
    capacidad      INTEGER     NOT NULL,
    ocupados       INTEGER     NOT NULL DEFAULT 0,
    actualizada_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_ocupacion PRIMARY KEY (sede_id, tipo_vehiculo),
    CONSTRAINT fk_ocupacion_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
    CONSTRAINT ck_ocupacion_tipo      CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_ocupacion_capacidad CHECK (capacidad >= 0),
    CONSTRAINT ck_ocupacion_ocupados  CHECK (ocupados >= 0 AND ocupados <= capacidad)
);

-- ---------------------------------------------------------------------
-- Cobro del día operativo (06:00 a 05:59 del día siguiente).
-- Máximo un cobro por placa, sede y día operativo.
-- tarifa_aplicada y franja son una copia de la tarifa usada (RN-07).
-- ---------------------------------------------------------------------
CREATE TABLE cobros (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id         BIGINT        NOT NULL,
    placa           VARCHAR(6)    NOT NULL,
    tipo_vehiculo   VARCHAR(10)   NOT NULL,
    dia_operativo   DATE          NOT NULL,
    franja          VARCHAR(10)   NOT NULL,
    tarifa_id       BIGINT        NOT NULL,
    tarifa_aplicada NUMERIC(12,2) NOT NULL,
    valor           NUMERIC(12,2) NOT NULL,
    estado          VARCHAR(10)   NOT NULL DEFAULT 'PENDIENTE',
    generado_en     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    vigente_hasta   TIMESTAMPTZ,
    CONSTRAINT uq_cobros_placa_sede_dia UNIQUE (placa, sede_id, dia_operativo),
    CONSTRAINT fk_cobros_sede   FOREIGN KEY (sede_id)   REFERENCES sedes (id),
    CONSTRAINT fk_cobros_tarifa FOREIGN KEY (tarifa_id) REFERENCES tarifas (id),
    CONSTRAINT ck_cobros_placa  CHECK (placa ~ '^([A-Z]{3}[0-9]{3}|[A-Z]{3}[0-9]{2}[A-Z])$'),
    CONSTRAINT ck_cobros_tipo   CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_cobros_franja CHECK (franja IN ('DIURNA', 'NOCTURNA')),
    CONSTRAINT ck_cobros_valores CHECK (tarifa_aplicada >= 0 AND valor >= 0),
    CONSTRAINT ck_cobros_estado CHECK (estado IN ('PENDIENTE', 'PAGADO', 'VENCIDO', 'ANULADO')),
    -- RN-03: un cobro pagado siempre tiene un pase con fecha de vencimiento.
    CONSTRAINT ck_cobros_pase_pagado CHECK (estado <> 'PAGADO' OR vigente_hasta IS NOT NULL)
);

-- ---------------------------------------------------------------------
-- Pago en efectivo de un cobro.
-- Un cobro tiene máximo un pago, y la clave de idempotencia evita que
-- un reintento registre el mismo pago dos veces.
-- ---------------------------------------------------------------------
CREATE TABLE pagos (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cobro_id           BIGINT        NOT NULL,
    medio              VARCHAR(10)   NOT NULL DEFAULT 'EFECTIVO',
    valor              NUMERIC(12,2) NOT NULL,
    usuario_id         BIGINT        NOT NULL,
    clave_idempotencia VARCHAR(64)   NOT NULL,
    registrado_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_pagos_cobro              UNIQUE (cobro_id),
    CONSTRAINT uq_pagos_clave_idempotencia UNIQUE (clave_idempotencia),
    CONSTRAINT fk_pagos_cobro   FOREIGN KEY (cobro_id)   REFERENCES cobros (id),
    CONSTRAINT fk_pagos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_pagos_medio CHECK (medio IN ('EFECTIVO')),
    CONSTRAINT ck_pagos_valor CHECK (valor > 0)
);

-- ---------------------------------------------------------------------
-- Eventos de ingreso y salida (auditoría de carriles).
-- Se registran todos los intentos, abra o no la barrera.
-- ---------------------------------------------------------------------
CREATE TABLE eventos (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id        BIGINT       NOT NULL,
    placa          VARCHAR(6)   NOT NULL,
    tipo_vehiculo  VARCHAR(10)  NOT NULL,
    tipo_evento    VARCHAR(10)  NOT NULL,
    origen_lectura VARCHAR(10)  NOT NULL,
    confianza      NUMERIC(5,2),
    resultado      VARCHAR(20)  NOT NULL,
    motivo         VARCHAR(200),
    cobro_id       BIGINT,
    usuario_id     BIGINT,
    ocurrido_en    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_eventos_sede    FOREIGN KEY (sede_id)    REFERENCES sedes (id),
    CONSTRAINT fk_eventos_cobro   FOREIGN KEY (cobro_id)   REFERENCES cobros (id),
    CONSTRAINT fk_eventos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_eventos_placa  CHECK (placa ~ '^([A-Z]{3}[0-9]{3}|[A-Z]{3}[0-9]{2}[A-Z])$'),
    CONSTRAINT ck_eventos_tipo_vehiculo CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_eventos_tipo_evento   CHECK (tipo_evento IN ('INGRESO', 'SALIDA')),
    CONSTRAINT ck_eventos_origen        CHECK (origen_lectura IN ('AUTOMATICA', 'MANUAL')),
    CONSTRAINT ck_eventos_confianza     CHECK (confianza IS NULL OR confianza BETWEEN 0 AND 100),
    -- Una lectura manual siempre queda asociada al operador que la digitó.
    CONSTRAINT ck_eventos_manual_usuario CHECK (origen_lectura <> 'MANUAL' OR usuario_id IS NOT NULL)
);

-- ---------------------------------------------------------------------
-- Índices para las consultas frecuentes (caja, salida y F-09).
-- ---------------------------------------------------------------------
CREATE INDEX ix_cobros_sede_estado   ON cobros  (sede_id, estado);
CREATE INDEX ix_cobros_dia_operativo ON cobros  (dia_operativo);
CREATE INDEX ix_eventos_placa        ON eventos (placa);
CREATE INDEX ix_eventos_sede_fecha   ON eventos (sede_id, ocurrido_en DESC);
CREATE INDEX ix_eventos_cobro        ON eventos (cobro_id);
CREATE INDEX ix_usuarios_rol         ON usuarios (rol_id);
CREATE INDEX ix_pagos_usuario        ON pagos   (usuario_id);
