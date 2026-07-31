-- Packs/lotes: un producto marcado como pack vende sus propias unidades pero,
-- además, descuenta también el stock de sus componentes al venderse.
ALTER TABLE producto ADD COLUMN es_pack BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE componente_pack (
    id            BIGSERIAL PRIMARY KEY,
    pack_id       BIGINT         NOT NULL REFERENCES producto (id) ON DELETE CASCADE,
    componente_id BIGINT         NOT NULL REFERENCES producto (id),
    cantidad      NUMERIC(10, 3) NOT NULL CHECK (cantidad > 0)
);

CREATE INDEX idx_componente_pack_pack ON componente_pack (pack_id);
