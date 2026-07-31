#!/bin/bash
# Arranca el TPV en macOS. Doble clic desde el Finder.
cd "$(dirname "$0")" || exit 1
mkdir -p .registros
echo "$(date) [arrancar-tpv] Arrancando la aplicación" >> .registros/actividad.log

echo
echo "  ALIMENTACIÓN MIGUEL - arrancando la aplicación"
echo "  =============================================="
echo

echo "  [1/4] Comprobando la base de datos..."
if command -v brew >/dev/null 2>&1; then
  brew services start postgresql@16 >/dev/null 2>&1 || brew services start postgresql >/dev/null 2>&1 || true
fi

if [ ! -d "frontend/node_modules" ]; then
  echo "  [2/4] Primera vez: instalando la pantalla. Esto tarda unos minutos..."
  (cd frontend && npm install) || { echo "  ERROR: no se pudo instalar la pantalla."; echo "$(date) [arrancar-tpv] ERROR: fallo npm install" >> .registros/actividad.log; read -r; exit 1; }
else
  echo "  [2/4] Pantalla ya preparada."
fi

echo "  [3/4] Arrancando el motor y la pantalla..."
mkdir -p .registros
(cd backend && ./mvnw spring-boot:run > ../.registros/motor.log 2>&1) &
echo $! > .registros/motor.pid
(cd frontend && npm run dev > ../.registros/pantalla.log 2>&1) &
echo $! > .registros/pantalla.pid

# IMPORTANTE: hay que esperar a los DOS puertos (8080 motor, 5173 pantalla).
# La pantalla arranca casi al instante, pero el motor (Java) tarda más, sobre
# todo la primerísima vez: Maven tiene que descargarse TODAS las librerías
# (no solo las nuevas), y eso puede tardar varios minutos según la conexión
# a internet de la tienda. Le damos hasta 10 minutos, avisando cada 15
# segundos para que no parezca que se ha quedado colgado.
echo "  [4/4] Esperando a que el motor y la pantalla arranquen..."
echo "  (la primera vez puede tardar varios minutos: Maven se descarga las librerías)"
for i in $(seq 1 600); do
  if curl -s -o /dev/null http://localhost:8080/api/auth/yo && curl -s -o /dev/null http://localhost:5173; then
    echo
    echo "  Listo. Abriendo la aplicación en el navegador..."
    echo "$(date) [arrancar-tpv] Listo, navegador abierto" >> .registros/actividad.log
    open "http://localhost:5173"
    echo
    echo "  IMPORTANTE: no cierres esta ventana mientras uses la aplicación."
    echo "  Para cerrar todo, ejecuta parar-tpv.command."
    echo
    wait
    exit 0
  fi
  if [ $((i % 15)) -eq 0 ]; then
    echo "  ...todavía arrancando, un momento más ($i segundos)..."
  fi
  sleep 1
done

echo
echo "  La aplicación tarda mucho más de lo normal en arrancar (más de 10 minutos)."
echo "  Revisa .registros/motor.log por si hay algún error."
echo "  Probamos igualmente a abrir el navegador, por si ya está casi lista:"
echo "$(date) [arrancar-tpv] AVISO: tardo mas de 10 minutos en responder" >> .registros/actividad.log
open "http://localhost:5173"
read -r
exit 1
