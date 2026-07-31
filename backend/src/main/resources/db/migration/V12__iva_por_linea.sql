-- Guarda el tipo de IVA de cada línea de venta en el momento de vender
-- (igual que ya se guarda precio_unitario), para poder desglosar el informe
-- de ventas por tipo de IVA de forma fiable aunque el IVA de un producto
-- cambie más adelante. Las líneas ya existentes se rellenan con el IVA
-- actual del producto, la mejor información disponible para ellas.
ALTER TABLE linea_venta ADD COLUMN iva_porcentaje NUMERIC(4, 1);

UPDATE linea_venta lv
SET iva_porcentaje = p.iva_porcentaje
FROM producto p
WHERE p.id = lv.producto_id;

ALTER TABLE linea_venta ALTER COLUMN iva_porcentaje SET NOT NULL;
