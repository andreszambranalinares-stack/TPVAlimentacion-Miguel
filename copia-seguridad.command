#!/bin/bash
# Copia de seguridad de la base de datos en macOS.
cd "$(dirname "$0")" || exit 1

echo
echo "  COPIA DE SEGURIDAD DE LA BASE DE DATOS"
echo "  ======================================"
echo

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "  ERROR: no encuentro pg_dump. Comprueba que PostgreSQL esté instalado."
  read -r
  exit 1
fi

mkdir -p copias
DESTINO="copias/tpv_$(date +%Y-%m-%d_%H-%M).sql"

echo "  Guardando en: $DESTINO"
echo

if ! PGPASSWORD=tpv pg_dump -h localhost -p 5432 -U tpv -d tpv -f "$DESTINO" --clean --if-exists; then
  echo
  echo "  ERROR: la copia ha fallado."
  echo "  Comprueba que PostgreSQL esté arrancado."
  read -r
  exit 1
fi

echo
echo "  Copia hecha correctamente."
echo
echo "  IMPORTANTE: una copia en el mismo ordenador no te salva si se"
echo "  estropea el disco duro. Copia el archivo a un pendrive o a la nube."
echo

find copias -name 'tpv_*.sql' -mtime +60 -delete 2>/dev/null

open copias
sleep 4
