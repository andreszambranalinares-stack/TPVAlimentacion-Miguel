#!/bin/bash
# Cierra el TPV en macOS.
cd "$(dirname "$0")" || exit 1

echo
echo "  Cerrando la aplicación..."

for nombre in motor pantalla; do
  if [ -f ".registros/$nombre.pid" ]; then
    pid=$(cat ".registros/$nombre.pid")
    kill "$pid" 2>/dev/null
    pkill -P "$pid" 2>/dev/null
    rm -f ".registros/$nombre.pid"
  fi
done

echo "  Aplicación cerrada. La base de datos sigue en marcha (no molesta)."
echo
echo "  Recuerda hacer la copia de seguridad de vez en cuando:"
echo "  ejecuta copia-seguridad.command."
echo
sleep 4
