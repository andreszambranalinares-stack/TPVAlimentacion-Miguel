#!/bin/bash
# Arranca el TPV en macOS. Doble clic desde el Finder.
cd "$(dirname "$0")" || exit 1

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
  (cd frontend && npm install) || { echo "  ERROR: no se pudo instalar la pantalla."; read -r; exit 1; }
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
# todo la primera vez que hay dependencias nuevas que descargar. Si solo se
# espera a la pantalla, el navegador se abre antes de que el motor esté listo
# y salen errores "ECONNREFUSED" en la consola hasta que termina de arrancar.
echo "  [4/4] Esperando a que el motor y la pantalla arranquen (puede tardar 1-2 minutos)..."
for _ in $(seq 1 180); do
  if curl -s -o /dev/null http://localhost:8080/api/auth/yo && curl -s -o /dev/null http://localhost:5173; then
    echo
    echo "  Listo. Abriendo la aplicación en el navegador..."
    open "http://localhost:5173"
    echo
    echo "  IMPORTANTE: no cierres esta ventana mientras uses la aplicación."
    echo "  Para cerrar todo, ejecuta parar-tpv.command."
    echo
    wait
    exit 0
  fi
  sleep 1
done

echo
echo "  La aplicación tarda más de lo normal en arrancar."
echo "  Revisa .registros/motor.log por si hay algún error."
read -r
exit 1
