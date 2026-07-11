-- Esquema Oracle para ProyectoConcierto. Ejecutar una vez contra el usuario configurado en config.properties.

CREATE TABLE usuarios (
    dni              VARCHAR2(8)   PRIMARY KEY,
    nombres          VARCHAR2(100) NOT NULL,
    apellidos        VARCHAR2(100) NOT NULL,
    correo           VARCHAR2(150),
    contrasena_hash  VARCHAR2(64)  NOT NULL,
    salt             VARCHAR2(32)  NOT NULL,
    rol              VARCHAR2(10)  NOT NULL CHECK (rol IN ('ADMIN', 'CLIENTE')),
    puntos           NUMBER        DEFAULT 0
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
    precio        NUMBER NOT NULL
);

-- No hay credenciales de administrador hardcodeadas en el código: cree la cuenta base a mano,
-- con un salt propio y el hash SHA-256 de (password + salt) generado por Persona.hashPassword.
-- Ejemplo (reemplace :hash y :salt por los valores reales calculados en Java):
-- INSERT INTO usuarios (dni, nombres, apellidos, correo, contrasena_hash, salt, rol, puntos)
-- VALUES ('admin', 'Admin', 'General', 'admin@proyecto.local', :hash, :salt, 'ADMIN', 0);
