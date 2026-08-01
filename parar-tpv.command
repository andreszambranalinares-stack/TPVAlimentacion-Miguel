#!/bin/bash
# Cierra el TPV en macOS.
cd "$(dirname "$0")" || exit 1
mkdir -p .registros
echo "$(date) [parar-tpv] Cerrando la aplicación" >> .registros/actividad.log

echo
echo "  Cerrando la aplicación..."

# Mata un proceso y todos sus descendientes (hijos, nietos...), de abajo hacia
# arriba. Se conserva aunque ahora el programa sea un único proceso Java: las
# versiones anteriores lanzaban el motor con mvnw y la pantalla con npm, que
# quedaban como "nietos", y matar solo el proceso del .pid los dejaba vivos.
matar_arbol() {
  local pid="$1"
  for hijo in $(pgrep -P "$pid" 2>/dev/null); do
    matar_arbol "$hijo"
  done
  kill "$pid" 2>/dev/null
}

# "programa" es el proceso actual. "motor" y "pantalla" son los de la versión
# anterior: si alguien actualiza con el TPV arrancado, seguirían vivos y sin
# esto se quedarían colgados para siempre.
for nombre in programa motor pantalla; do
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
