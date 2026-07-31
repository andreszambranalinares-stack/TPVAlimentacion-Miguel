#!/bin/bash
# Versión silenciosa de copia-seguridad.command: no abre ninguna ventana ni
# espera que se pulse ninguna tecla, pensada para que la ejecute sola la
# propia app (parar-tpv.command) al cerrar la tienda. Si algo falla, queda
# anotado en .registros/actividad.log para poder revisarlo luego.
cd "$(dirname "$0")" || exit 1
mkdir -p .registros
LOG=".registros/actividad.log"

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "$(date) [copia automática] ERROR: no se encontró pg_dump" >> "$LOG"
  exit 1
fi

mkdir -p copias
DESTINO="copias/tpv_$(date +%Y-%m-%d_%H-%M).sql"

if ! PGPASSWORD=tpv pg_dump -h localhost -p 5432 -U tpv -d tpv -f "$DESTINO" --clean --if-exists >/dev/null 2>&1; then
  echo "$(date) [copia automática] ERROR: la copia ha fallado" >> "$LOG"
  exit 1
fi

echo "$(date) [copia automática] OK: guardada en $DESTINO" >> "$LOG"

find copias -name 'tpv_*.sql' -mtime +60 -delete 2>/dev/null
exit 0
