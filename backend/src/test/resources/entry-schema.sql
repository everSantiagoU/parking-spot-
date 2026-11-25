-- Tablas ocupacion y eventos de HU-05 (V1__esquema_inicial.sql), adaptadas a H2 para las pruebas
-- del flujo de ingreso (HU-10 y HU-13). Se ejecuta después de auth-schema.sql (roles, sedes, usuarios).
-- Diferencias con PostgreSQL: TIMESTAMP WITH TIME ZONE en lugar de TIMESTAMPTZ, REGEXP_LIKE en lugar
-- del operador ~, y eventos.cobro_id sin llave foránea porque estas pruebas no crean la tabla cobros.
CREATE TABLE ocupacion (
    sede_id        BIGINT      NOT NULL,
    tipo_vehiculo  VARCHAR(10) NOT NULL,
    capacidad      INTEGER     NOT NULL,
    ocupados       INTEGER     NOT NULL DEFAULT 0,
    actualizada_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_ocupacion PRIMARY KEY (sede_id, tipo_vehiculo),
    CONSTRAINT fk_ocupacion_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
    CONSTRAINT ck_ocupacion_tipo      CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_ocupacion_capacidad CHECK (capacidad >= 0),
    CONSTRAINT ck_ocupacion_ocupados  CHECK (ocupados >= 0 AND ocupados <= capacidad)
);

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
    ocurrido_en    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_eventos_sede    FOREIGN KEY (sede_id)    REFERENCES sedes (id),
    CONSTRAINT fk_eventos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_eventos_placa  CHECK (REGEXP_LIKE(placa, '^([A-Z]{3}[0-9]{3}|[A-Z]{3}[0-9]{2}[A-Z])$')),
    CONSTRAINT ck_eventos_tipo_vehiculo CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_eventos_tipo_evento   CHECK (tipo_evento IN ('INGRESO', 'SALIDA')),
    CONSTRAINT ck_eventos_origen        CHECK (origen_lectura IN ('AUTOMATICA', 'MANUAL')),
    CONSTRAINT ck_eventos_confianza     CHECK (confianza IS NULL OR confianza BETWEEN 0 AND 100),
    CONSTRAINT ck_eventos_manual_usuario CHECK (origen_lectura <> 'MANUAL' OR usuario_id IS NOT NULL)
);

-- Cupos iniciales de V2__datos_iniciales.sql.
INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad)
SELECT id, 'CARRO', 50 FROM sedes WHERE nombre = 'Sede Principal';
INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad)
SELECT id, 'MOTO', 30 FROM sedes WHERE nombre = 'Sede Principal';
