-- =====================================================================
-- HU-05 · Datos iniciales necesarios para operar el MVP.
-- Los valores de tarifas, franjas y cupos son iniciales: el
-- administrador los cambia desde la aplicación (HU-12).
-- Los usuarios iniciales se crean en HU-06 (necesitan hash BCrypt).
-- =====================================================================

INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN',    'Administra tarifas, franjas, cupos y consulta la operación'),
    ('OPERADOR', 'Opera los carriles de entrada y salida y la caja');

INSERT INTO sedes (nombre, direccion) VALUES
    ('Sede Principal', 'Por definir');

-- Franjas: el día operativo empieza a las 06:00 (S-02).
INSERT INTO franjas (sede_id, nombre, hora_inicio, hora_fin)
SELECT id, 'DIURNA',   TIME '06:00', TIME '18:00' FROM sedes WHERE nombre = 'Sede Principal'
UNION ALL
SELECT id, 'NOCTURNA', TIME '18:00', TIME '06:00' FROM sedes WHERE nombre = 'Sede Principal';

-- Tarifas: la moto queda en 0 (RN-01) y la nocturna es menor que la diurna (RN-02).
INSERT INTO tarifas (sede_id, tipo_vehiculo, franja, valor)
SELECT s.id, t.tipo_vehiculo, t.franja, t.valor
FROM sedes s
CROSS JOIN (VALUES
    ('CARRO', 'DIURNA',   10000.00),
    ('CARRO', 'NOCTURNA',  6000.00),
    ('MOTO',  'DIURNA',       0.00),
    ('MOTO',  'NOCTURNA',     0.00)
) AS t (tipo_vehiculo, franja, valor)
WHERE s.nombre = 'Sede Principal';

INSERT INTO ocupacion (sede_id, tipo_vehiculo, capacidad)
SELECT s.id, o.tipo_vehiculo, o.capacidad
FROM sedes s
CROSS JOIN (VALUES ('CARRO', 50), ('MOTO', 30)) AS o (tipo_vehiculo, capacidad)
WHERE s.nombre = 'Sede Principal';
