-- Devoluciones: permiten anular parcial o totalmente una venta ya cobrada,
-- reponiendo stock y dejando rastro igual que cualquier otro movimiento.
ALTER TABLE movimiento_stock DROP CONSTRAINT movimiento_stock_tipo_check;
ALTER TABLE movimiento_stock ADD CONSTRAINT movimiento_stock_tipo_check
    CHECK (tipo IN ('ENTRADA', 'SALIDA', 'AJUSTE', 'MERMA', 'DEVOLUCION'));

CREATE TABLE devolucion (
    id         BIGSERIAL PRIMARY KEY,
    venta_id   BIGINT         NOT NULL REFERENCES venta (id),
    fecha_hora TIMESTAMP      NOT NULL DEFAULT now(),
    total      NUMERIC(10, 2) NOT NULL,
    total_iva  NUMERIC(10, 2) NOT NULL,
    motivo     VARCHAR(255)
);

CREATE INDEX idx_devolucion_venta ON devolucion (venta_id);
CREATE INDEX idx_devolucion_fecha_hora ON devolucion (fecha_hora);

CREATE TABLE linea_devolucion (
    id             BIGSERIAL PRIMARY KEY,
    devolucion_id  BIGINT         NOT NULL REFERENCES devolucion (id) ON DELETE CASCADE,
    linea_venta_id BIGINT         NOT NULL REFERENCES linea_venta (id),
    cantidad       NUMERIC(10, 3) NOT NULL CHECK (cantidad > 0),
    importe        NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_linea_devolucion_devolucion ON linea_devolucion (devolucion_id);
CREATE INDEX idx_linea_devolucion_linea_venta ON linea_devolucion (linea_venta_id);
