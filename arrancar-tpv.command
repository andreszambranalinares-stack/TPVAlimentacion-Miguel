#!/bin/bash
# Arranca el TPV en macOS. Doble clic desde el Finder.
cd "$(dirname "$0")" || exit 1
mkdir -p .registros
echo "$(date) [arrancar-tpv] Arrancando la aplicación" >> .registros/actividad.log

echo
echo "  ALIMENTACIÓN MIGUEL - arrancando la aplicación"
echo "  =============================================="
echo

# --- 0. Comprobar que el ordenador está preparado ---------------------
# Desde que la pantalla va incluida dentro del propio programa, aquí solo
# hace falta "tpv.jar". Lo genera preparar-pc.command, que se ejecuta una
# única vez al montar el ordenador de la tienda.
if [ ! -f "tpv.jar" ]; then
  echo "  Este ordenador todavía no está preparado."
  echo
  echo "  Ejecuta primero preparar-pc.command (solo hay que hacerlo una vez)."
  echo
  echo "$(date) [arrancar-tpv] AVISO: falta tpv.jar, hay que ejecutar preparar-pc" >> .registros/actividad.log
  read -r
  exit 1
fi

# --- 1. Base de datos -------------------------------------------------
echo "  [1/3] Comprobando la base de datos..."
if command -v brew >/dev/null 2>&1; then
  brew services start postgresql@16 >/dev/null 2>&1 || brew services start postgresql >/dev/null 2>&1 || true
fi

# --- 2. Arrancar el programa ------------------------------------------
echo "  [2/3] Arrancando el programa..."
java -jar tpv.jar > .registros/programa.log 2>&1 &
echo $! > .registros/programa.pid

# --- 3. Esperar a que responda y abrir el navegador -------------------
# Ahora solo hay UN puerto que esperar (8080): el mismo programa sirve
# tanto los datos como la pantalla. Y ya no hay descargas que hacer (eso lo
# dejó resuelto preparar-pc), así que tarda segundos, no minutos.
echo "  [3/3] Esperando a que el programa esté listo..."
for i in $(seq 1 180); do
  if curl -s -o /dev/null --max-time 2 http://localhost:8080/api/auth/yo; then
    echo
    echo "  Listo. Abriendo la aplicación en el navegador..."
    echo "$(date) [arrancar-tpv] Listo, navegador abierto" >> .registros/actividad.log
    open "http://localhost:8080"
    echo
    echo "  IMPORTANTE: no cierres esta ventana mientras uses la aplicación."
    echo "  Para cerrar todo, ejecuta parar-tpv.command."
    echo
    wait
    exit 0
  fi
  if [ $((i % 15)) -eq 0 ]; then
    echo "  ...todavía arrancando ($i segundos)..."
  fi
  sleep 1
done

echo
echo "  El programa tarda mucho más de lo normal en arrancar (más de 3 minutos)."
echo "  Revisa .registros/programa.log por si hay algún error."
echo "  Lo más habitual es que la base de datos no esté arrancada."
echo "  Probamos igualmente a abrir el navegador, por si ya está casi lista:"
echo "$(date) [arrancar-tpv] AVISO: tardo mas de 3 minutos en responder" >> .registros/actividad.log
open "http://localhost:8080"
read -r
exit 1
