-- Datos fiscales/de contacto de la tienda, para mostrarlos en el ticket
-- impreso. Fila única (id fijo = 1): no hace falta más de una tienda.
CREATE TABLE datos_tienda (
    id        BIGINT PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    telefono  VARCHAR(30),
    nif       VARCHAR(20)
);

INSERT INTO datos_tienda (id, nombre) VALUES (1, 'Alimentación Miguel');
