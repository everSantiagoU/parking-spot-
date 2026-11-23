-- Contrato de roles, sedes y usuarios de HU-05 (PR #23), adaptado a H2.
CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(20)  NOT NULL,
    descripcion VARCHAR(150),
    CONSTRAINT uq_roles_nombre UNIQUE (nombre),
    CONSTRAINT ck_roles_nombre CHECK (nombre IN ('ADMIN', 'OPERADOR'))
);
CREATE TABLE sedes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    direccion  VARCHAR(200),
    activa     BOOLEAN      NOT NULL DEFAULT TRUE,
    creada_en  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    CONSTRAINT uq_sedes_nombre UNIQUE (nombre)
);
CREATE TABLE usuarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    rol_id          BIGINT       NOT NULL,
    sede_id         BIGINT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT fk_usuarios_rol  FOREIGN KEY (rol_id)  REFERENCES roles (id),
    CONSTRAINT fk_usuarios_sede FOREIGN KEY (sede_id) REFERENCES sedes (id)
);

INSERT INTO roles (nombre) VALUES ('ADMIN'), ('OPERADOR');
INSERT INTO sedes (nombre) VALUES ('Sede Principal');
