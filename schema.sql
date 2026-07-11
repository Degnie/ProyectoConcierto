-- Esquema Oracle para ProyectoConcierto. Ejecutar una vez contra el usuario configurado en config.properties.

CREATE TABLE usuarios (
    dni              VARCHAR2(8)   PRIMARY KEY,
    nombres          VARCHAR2(100) NOT NULL,
    apellidos        VARCHAR2(100) NOT NULL,
    correo           VARCHAR2(150) UNIQUE,
    contrasena_hash  VARCHAR2(64)  NOT NULL,
    salt             VARCHAR2(32)  NOT NULL,
    rol              VARCHAR2(10)  NOT NULL CHECK (rol IN ('ADMIN', 'CLIENTE')),
    puntos           NUMBER        DEFAULT 0,
    fecha_nacimiento DATE          NOT NULL
);

CREATE TABLE conciertos (
    id       VARCHAR2(36) PRIMARY KEY,
    nombre   VARCHAR2(150) NOT NULL,
    fecha_ms NUMBER NOT NULL
);

CREATE TABLE zonas (
    id            VARCHAR2(36) PRIMARY KEY,
    concierto_id  VARCHAR2(36) NOT NULL REFERENCES conciertos(id) ON DELETE CASCADE,
    nombre        VARCHAR2(100) NOT NULL,
    capacidad     NUMBER NOT NULL,
    precio        NUMBER NOT NULL,
    version       NUMBER DEFAULT 0 NOT NULL -- bloqueo optimista real (antes vivía, sin efecto real, en Zona.java)
);

-- Solo se persisten las entradas que efectivamente se vendieron (nace junto con su venta); una
-- zona con capacidad 25000 y 40 vendidas tiene 40 filas acá, no 25000. La disponibilidad se
-- calcula como capacidad - COUNT(entradas activas de esa zona).
CREATE TABLE ventas (
    id_venta          VARCHAR2(36)  PRIMARY KEY,
    dni_cliente       VARCHAR2(8)   NOT NULL REFERENCES usuarios(dni),
    id_concierto      VARCHAR2(36)  NOT NULL REFERENCES conciertos(id),
    id_zona           VARCHAR2(36)  NOT NULL REFERENCES zonas(id),
    fecha_hora        TIMESTAMP     NOT NULL,
    monto_neto        NUMBER        NOT NULL,
    puntos_redimidos  NUMBER        DEFAULT 0 NOT NULL,
    puntos_ganados    NUMBER        DEFAULT 0 NOT NULL,
    estado            VARCHAR2(10)  NOT NULL CHECK (estado IN ('PAID', 'CANCELLED')),
    payment_txn_id    VARCHAR2(20)
);

CREATE TABLE entradas (
    id_entrada  VARCHAR2(36) PRIMARY KEY,
    id_venta    VARCHAR2(36) NOT NULL REFERENCES ventas(id_venta),
    id_zona     VARCHAR2(36) NOT NULL REFERENCES zonas(id),
    numero      NUMBER       NOT NULL,
    estado      VARCHAR2(12) NOT NULL CHECK (estado IN ('SOLD', 'CANCELLED'))
);

-- No hay credenciales de administrador hardcodeadas en el código: cree la cuenta base a mano,
-- con un salt propio y el hash PBKDF2WithHmacSHA256 (65536 iteraciones) generado por
-- Persona.hashPassword.
-- Ejemplo (reemplace :hash y :salt por los valores reales calculados en Java):
-- INSERT INTO usuarios (dni, nombres, apellidos, correo, contrasena_hash, salt, rol, puntos, fecha_nacimiento)
-- VALUES ('admin', 'Admin', 'General', 'admin@proyecto.local', :hash, :salt, 'ADMIN', 0, DATE '1990-01-01');

-- Migración incremental (proyectos ya desplegados antes de esta iteración):
-- ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_correo UNIQUE (correo);
-- ALTER TABLE zonas ADD version NUMBER DEFAULT 0 NOT NULL;
-- (crear ventas y entradas con los CREATE TABLE de arriba si no existían)
