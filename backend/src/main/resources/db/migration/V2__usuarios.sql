-- Usuarios de la aplicación con roles ADMIN (gestión e informes) y CAJERO (caja y stock)

CREATE TABLE usuario (
    id             BIGSERIAL PRIMARY KEY,
    nombre_usuario VARCHAR(50)  NOT NULL UNIQUE,
    hash_password  VARCHAR(100) NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    rol            VARCHAR(10)  NOT NULL CHECK (rol IN ('ADMIN', 'CAJERO')),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Usuarios iniciales: admin/admin123 y caja/caja123 (cambiar en producción)
INSERT INTO usuario (nombre_usuario, hash_password, nombre, rol)
VALUES ('admin', '$2a$10$OOA/t18vICNvNiU7RerwXukPBzz43q0XnLw701PS0iU8/QDspoq3y', 'Administrador', 'ADMIN'),
       ('caja', '$2a$10$5dBCUiyv1giShaySq0meseirK7vcpWSNxApS2na2a6no41mduL8K6', 'Cajero', 'CAJERO');
