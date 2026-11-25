ALTER TABLE eventos DROP CONSTRAINT ck_eventos_tipo_evento;
ALTER TABLE eventos ADD CONSTRAINT ck_eventos_tipo_evento
    CHECK (tipo_evento IN ('INGRESO', 'VALIDACION', 'SALIDA'));

ALTER TABLE eventos ADD COLUMN dia_operativo_exento DATE;
ALTER TABLE eventos ADD CONSTRAINT ck_eventos_exencion_ingreso
    CHECK (dia_operativo_exento IS NULL OR
           (tipo_evento = 'INGRESO' AND resultado = 'AUTORIZADO' AND tipo_vehiculo = 'MOTO'));

CREATE INDEX ix_eventos_movimiento_placa
    ON eventos (sede_id, placa, ocurrido_en DESC, id DESC)
    WHERE resultado = 'AUTORIZADO' AND tipo_evento IN ('INGRESO', 'SALIDA');
