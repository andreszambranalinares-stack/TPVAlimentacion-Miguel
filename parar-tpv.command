#!/bin/bash
# Cierra el TPV en macOS.
cd "$(dirname "$0")" || exit 1

echo
echo "  Cerrando la aplicación..."

# Mata un proceso y todos sus descendientes (hijos, nietos...), de abajo hacia
# arriba. Hace falta porque el motor Java (lanzado por mvnw) y el servidor de
# la pantalla (lanzado por npm) son "nietos" del proceso guardado en el .pid,
# y matar solo ese proceso o solo sus hijos directos los deja huérfanos y vivos.
matar_arbol() {
  local pid="$1"
  for hijo in $(pgrep -P "$pid" 2>/dev/null); do
    matar_arbol "$hijo"
  done
  kill "$pid" 2>/dev/null
}

for nombre in motor pantalla; do
  if [ -f ".registros/$nombre.pid" ]; then
    pid=$(cat ".registros/$nombre.pid")
    matar_arbol "$pid"
    rm -f ".registros/$nombre.pid"
  fi
done

echo "  Aplicación cerrada. La base de datos sigue en marcha (no molesta)."
echo
echo "  Recuerda hacer la copia de seguridad de vez en cuando:"
echo "  ejecuta copia-seguridad.command."
echo
sleep 4
