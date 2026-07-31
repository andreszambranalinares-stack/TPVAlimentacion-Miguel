-- Email y dirección del proveedor: hacen falta para poder mandarle un
-- pedido en PDF por correo y para que el propio PDF muestre a quién va.
ALTER TABLE proveedor ADD COLUMN email VARCHAR(150);
ALTER TABLE proveedor ADD COLUMN direccion VARCHAR(255);
