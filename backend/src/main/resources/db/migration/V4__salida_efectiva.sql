ALTER TABLE eventos ADD COLUMN ingreso_id BIGINT;
ALTER TABLE eventos
    ADD CONSTRAINT fk_eventos_ingreso FOREIGN KEY (ingreso_id) REFERENCES eventos (id),
    ADD CONSTRAINT uq_eventos_salida_por_ingreso UNIQUE (ingreso_id),
    ADD CONSTRAINT ck_eventos_salida_vinculada
        CHECK (ingreso_id IS NULL OR (tipo_evento = 'SALIDA' AND resultado = 'AUTORIZADO'));
