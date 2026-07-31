-- Esquema inicial del TPV de alimentación

CREATE TABLE categoria (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE proveedor (
    id       BIGSERIAL PRIMARY KEY,
    nombre   VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    contacto VARCHAR(150)
);

CREATE TABLE producto (
    id             BIGSERIAL PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    codigo_barras  VARCHAR(50) UNIQUE,
    categoria_id   BIGINT REFERENCES categoria (id),
    precio_venta   NUMERIC(10, 2) NOT NULL CHECK (precio_venta >= 0),
    precio_coste   NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (precio_coste >= 0),
    iva_porcentaje NUMERIC(4, 1)  NOT NULL CHECK (iva_porcentaje IN (0, 4, 10, 21)),
    stock_actual   NUMERIC(10, 3) NOT NULL DEFAULT 0,
    stock_minimo   NUMERIC(10, 3) NOT NULL DEFAULT 0,
    unidad_medida  VARCHAR(10)    NOT NULL CHECK (unidad_medida IN ('UNIDAD', 'KG', 'LITRO')),
    activo         BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_producto_nombre ON producto (nombre);
CREATE INDEX idx_producto_categoria ON producto (categoria_id);

CREATE TABLE venta (
    id          BIGSERIAL PRIMARY KEY,
    fecha_hora  TIMESTAMP      NOT NULL DEFAULT now(),
    total       NUMERIC(10, 2) NOT NULL,
    total_iva   NUMERIC(10, 2) NOT NULL,
    metodo_pago VARCHAR(10)    NOT NULL CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA'))
);

CREATE INDEX idx_venta_fecha_hora ON venta (fecha_hora);

CREATE TABLE linea_venta (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT         NOT NULL REFERENCES venta (id) ON DELETE CASCADE,
    producto_id     BIGINT         NOT NULL REFERENCES producto (id),
    cantidad        NUMERIC(10, 3) NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(10, 2) NOT NULL,
    subtotal        NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_linea_venta_venta ON linea_venta (venta_id);
CREATE INDEX idx_linea_venta_producto ON linea_venta (producto_id);

CREATE TABLE movimiento_stock (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT         NOT NULL REFERENCES producto (id),
    tipo        VARCHAR(10)    NOT NULL CONSTRAINT movimiento_stock_tipo_check
                    CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE', 'MERMA')),
    cantidad    NUMERIC(10, 3) NOT NULL,
    motivo      VARCHAR(255),
    fecha_hora  TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_movimiento_stock_producto ON movimiento_stock (producto_id);
CREATE INDEX idx_movimiento_stock_fecha ON movimiento_stock (fecha_hora);
