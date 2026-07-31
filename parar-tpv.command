#!/bin/bash
# Cierra el TPV en macOS.
cd "$(dirname "$0")" || exit 1
mkdir -p .registros
echo "$(date) [parar-tpv] Cerrando la aplicación" >> .registros/actividad.log

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
echo "$(date) [parar-tpv] Aplicación cerrada" >> .registros/actividad.log
echo

# Copia de seguridad automática al cerrar, sin que haya que acordarse.
# Si falla, queda anotado en .registros/actividad.log (no interrumpe el cierre).
echo "  Guardando una copia de seguridad..."
if "$(dirname "$0")/copia-seguridad-automatica.command"; then
  echo "  Copia de seguridad guardada."
else
  echo "  AVISO: la copia de seguridad automática ha fallado esta vez."
  echo "  Puedes intentarlo a mano con copia-seguridad.command, o mira"
  echo "  .registros/actividad.log para ver el detalle."
fi
echo
sleep 4
