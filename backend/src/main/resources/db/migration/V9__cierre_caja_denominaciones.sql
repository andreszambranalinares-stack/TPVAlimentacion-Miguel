-- Desglose opcional del efectivo contado al cerrar caja, por billete/moneda
CREATE TABLE denominacion_cierre (
    id             BIGSERIAL PRIMARY KEY,
    cierre_caja_id BIGINT        NOT NULL REFERENCES cierre_caja (id) ON DELETE CASCADE,
    valor          NUMERIC(6, 2) NOT NULL,
    cantidad       INT           NOT NULL CHECK (cantidad >= 0)
);

CREATE INDEX idx_denominacion_cierre_cierre ON denominacion_cierre (cierre_caja_id);
