-- Bloqueo optimista para evitar sobreventa con ventas o movimientos concurrentes
ALTER TABLE producto ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
