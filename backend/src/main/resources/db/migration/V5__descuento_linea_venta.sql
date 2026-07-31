-- Descuento porcentual opcional por línea de venta (uso reservado a ADMIN)
ALTER TABLE linea_venta ADD COLUMN descuento_porcentaje NUMERIC(5, 2) NOT NULL DEFAULT 0
    CHECK (descuento_porcentaje >= 0 AND descuento_porcentaje <= 100);
