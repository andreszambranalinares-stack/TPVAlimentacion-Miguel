#!/bin/bash
# Prepara este Mac para usar el TPV. Solo hay que ejecutarlo UNA VEZ.
# Doble clic desde el Finder.
cd "$(dirname "$0")" || exit 1
mkdir -p .registros
echo "$(date) [preparar-pc] Inicio de la preparación" >> .registros/actividad.log

fallo() {
  echo
  echo "$1"
  echo
  echo "$(date) [preparar-pc] ERROR: $2" >> .registros/actividad.log
  echo "  (pulsa Intro para cerrar esta ventana)"
  read -r
  exit 1
}

echo
echo "  ALIMENTACIÓN MIGUEL - preparar este ordenador"
echo "  ============================================="
echo
echo "  Esto solo hay que hacerlo UNA VEZ, el día que se monta el"
echo "  ordenador de la tienda. Después, para el día a día, basta con"
echo "  arrancar-tpv y parar-tpv."
echo
echo "  Voy a comprobar 3 cosas y a dejarlo todo listo."
echo "  (pulsa Intro para empezar)"
read -r

# ----------------------------------------------------------------------
# 1. JAVA
# ----------------------------------------------------------------------
echo
echo "  [1/3] Comprobando Java..."

if ! command -v java >/dev/null 2>&1; then
  fallo "  ERROR: no tienes Java instalado.

  Qué hacer:
    1. Entra en   https://adoptium.net
    2. Descarga \"Temurin 21 (LTS)\" para macOS.
       (si tu Mac es M1/M2/M3 o más reciente, elige la versión aarch64)
    3. Instálalo dando a Siguiente en todo.
    4. Vuelve a ejecutar preparar-pc." "falta Java"
fi

# OJO: no se puede coger sin más la primera línea de "java -version". Si el
# ordenador tiene definida la variable JAVA_TOOL_OPTIONS (algunos instaladores
# y antivirus lo hacen), Java imprime antes un "Picked up JAVA_TOOL_OPTIONS:..."
# y la versión pasa a la segunda línea. Por eso buscamos la línea que de verdad
# lleva la versión, en vez de fiarnos del orden.
VERSION_JAVA=$(java -version 2>&1 | grep -i 'version "' | head -1 | sed -n 's/.*version "\([0-9]*\).*/\1/p')
if [ -z "$VERSION_JAVA" ] || [ "$VERSION_JAVA" -lt 21 ]; then
  fallo "  ERROR: hace falta Java 21 o superior (tienes la $VERSION_JAVA).

  Descarga \"Temurin 21 (LTS)\" en https://adoptium.net,
  instálalo y vuelve a ejecutar preparar-pc." "Java $VERSION_JAVA es anterior a la 21"
fi
echo "        Java $VERSION_JAVA - correcto."

# ----------------------------------------------------------------------
# 2. POSTGRESQL
# ----------------------------------------------------------------------
echo
echo "  [2/3] Comprobando la base de datos (PostgreSQL)..."

if ! command -v psql >/dev/null 2>&1; then
  fallo "  ERROR: no encuentro PostgreSQL (la base de datos).

  Qué hacer (con Homebrew, lo más sencillo en Mac):
    1. Abre la aplicación Terminal.
    2. Si no tienes Homebrew, pega esto y pulsa Intro:
       /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"
    3. Después pega:   brew install postgresql@16
    4. Y luego:        brew services start postgresql@16
    5. Vuelve a ejecutar preparar-pc." "falta PostgreSQL"
fi
echo "        PostgreSQL encontrado."

# Arrancar el servicio por si estuviera parado (si ya corre, no molesta)
if command -v brew >/dev/null 2>&1; then
  brew services start postgresql@16 >/dev/null 2>&1 || brew services start postgresql >/dev/null 2>&1 || true
  sleep 2
fi

# Si la base de datos ya está lista, no tocamos nada
if PGPASSWORD=tpv psql -h localhost -U tpv -d tpv -c "SELECT 1;" >/dev/null 2>&1; then
  echo "        La base de datos ya estaba creada - correcto."
else
  echo "        Falta crear la base de datos de la tienda. Creándola..."
  # En Mac, con Homebrew, el usuario que instala es superusuario de Postgres
  psql -h localhost -d postgres -c "CREATE USER tpv WITH PASSWORD 'tpv';" >/dev/null 2>&1
  psql -h localhost -d postgres -c "CREATE DATABASE tpv OWNER tpv;" >/dev/null 2>&1

  if ! PGPASSWORD=tpv psql -h localhost -U tpv -d tpv -c "SELECT 1;" >/dev/null 2>&1; then
    fallo "  ERROR: no he podido dejar lista la base de datos.

  Comprueba que PostgreSQL esté arrancado:
    brew services start postgresql@16

  y vuelve a ejecutar preparar-pc." "no se pudo preparar la base de datos"
  fi
  echo "        Base de datos creada - correcto."
fi

# ----------------------------------------------------------------------
# 3. COMPILAR EL PROGRAMA
# ----------------------------------------------------------------------
echo
echo "  [3/3] Preparando el programa de la tienda..."
echo "        (la primera vez tarda VARIOS MINUTOS: se descarga todo lo"
echo "         necesario de internet. Déjalo trabajar, no cierres nada.)"
echo

if ! (cd backend && ./mvnw -Pcompleto clean package -DskipTests); then
  fallo "  ERROR: no he podido preparar el programa.

  Lo más habitual es que se haya cortado internet a mitad
  (hace falta para descargar lo necesario la primera vez).
  Comprueba la conexión y vuelve a ejecutar preparar-pc." "falló la compilación"
fi

# Nombre fijo y fácil en la carpeta principal, para que arrancar-tpv no
# dependa del número de versión del archivo.
cp -f backend/target/tpv-backend-0.1.0-SNAPSHOT.jar tpv.jar || \
  fallo "  ERROR: el programa se compiló pero no he podido copiarlo aquí." "no se pudo copiar el jar"

echo
echo "  =========================================================="
echo "    LISTO. El ordenador ya está preparado."
echo "  =========================================================="
echo
echo "  A partir de ahora, en el día a día:"
echo
echo "    - Al abrir la tienda:   arrancar-tpv"
echo "    - Al cerrar la tienda:  parar-tpv"
echo
echo "  IMPORTANTE, antes del primer día de verdad:"
echo "    1. Entra con admin / admin123 y CAMBIA las contraseñas"
echo "       desde el apartado \"Empleados\"."
echo "    2. Sube tu catálogo real desde \"Productos\" - \"Importar CSV\"."
echo "    3. Prueba la impresora y el lector de código de barras."
echo
echo "$(date) [preparar-pc] Preparación completada correctamente" >> .registros/actividad.log
echo "  (pulsa Intro para cerrar esta ventana)"
read -r
exit 0
