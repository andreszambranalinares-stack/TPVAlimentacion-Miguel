-- Pedidos formales a proveedor: se crea un pedido con líneas y se va recibiendo
-- (parcial o completamente), generando movimientos ENTRADA de verdad.
CREATE TABLE pedido_proveedor (
    id           BIGSERIAL PRIMARY KEY,
    proveedor_id BIGINT      NOT NULL REFERENCES proveedor (id),
    fecha_hora   TIMESTAMP   NOT NULL DEFAULT now(),
    estado       VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'RECIBIDO_PARCIAL', 'RECIBIDO_COMPLETO', 'CANCELADO')),
    notas        VARCHAR(255)
);

CREATE INDEX idx_pedido_proveedor_proveedor ON pedido_proveedor (proveedor_id);
CREATE INDEX idx_pedido_proveedor_estado ON pedido_proveedor (estado);

CREATE TABLE linea_pedido_proveedor (
    id                     BIGSERIAL PRIMARY KEY,
    pedido_id              BIGINT         NOT NULL REFERENCES pedido_proveedor (id) ON DELETE CASCADE,
    producto_id            BIGINT         NOT NULL REFERENCES producto (id),
    cantidad_pedida        NUMERIC(10, 3) NOT NULL CHECK (cantidad_pedida > 0),
    cantidad_recibida      NUMERIC(10, 3) NOT NULL DEFAULT 0 CHECK (cantidad_recibida >= 0),
    precio_coste_unitario  NUMERIC(10, 2)
);

CREATE INDEX idx_linea_pedido_proveedor_pedido ON linea_pedido_proveedor (pedido_id);
