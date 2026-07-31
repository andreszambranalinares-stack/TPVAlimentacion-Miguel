-- Deja constancia de qué empleado hizo cada venta, devolución, cierre de caja
-- y movimiento de stock. Nullable porque las filas ya existentes no tienen
-- ese dato (se hicieron antes de que existiera esta columna).
ALTER TABLE venta ADD COLUMN usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE devolucion ADD COLUMN usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE cierre_caja ADD COLUMN usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE movimiento_stock ADD COLUMN usuario_id BIGINT REFERENCES usuario (id);
