-- Cierre de caja diario: arqueo del efectivo contra lo vendido

CREATE TABLE cierre_caja (
    id               BIGSERIAL PRIMARY KEY,
    fecha            DATE           NOT NULL UNIQUE,
    numero_ventas    INT            NOT NULL,
    total_ventas     NUMERIC(10, 2) NOT NULL,
    total_efectivo   NUMERIC(10, 2) NOT NULL,
    total_tarjeta    NUMERIC(10, 2) NOT NULL,
    efectivo_contado NUMERIC(10, 2) NOT NULL,
    diferencia       NUMERIC(10, 2) NOT NULL,
    notas            VARCHAR(255),
    fecha_hora       TIMESTAMP      NOT NULL DEFAULT now()
);
