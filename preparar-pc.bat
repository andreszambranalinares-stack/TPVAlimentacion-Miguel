@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title Preparar este ordenador - Alimentacion Miguel
cd /d "%~dp0"
if not exist ".registros" mkdir ".registros"
echo %date% %time% [preparar-pc] Inicio de la preparacion >> ".registros\actividad.log"

echo.
echo   ALIMENTACION MIGUEL - preparar este ordenador
echo   =============================================
echo.
echo   Esto solo hay que hacerlo UNA VEZ, el dia que se monta el
echo   ordenador de la tienda. Despues, para el dia a dia, basta con
echo   "arrancar-tpv" y "parar-tpv".
echo.
echo   Voy a comprobar 3 cosas y a dejarlo todo listo.
echo.
pause

REM ======================================================================
REM  1. JAVA
REM ======================================================================
echo.
echo   [1/3] Comprobando Java...

java -version >nul 2>&1
if errorlevel 1 goto :faltaJava

REM La version sale por la salida de ERRORES (de ahi el 2^>^&1), con este
REM formato:   openjdk version "21.0.1" 2023-10-17
REM Cogemos la tercera palabra ^("21.0.1"^) y le quitamos las comillas.
REM
REM OJO: hay que filtrar por la palabra "version" en vez de coger la primera
REM linea sin mas. Si el ordenador tiene definida la variable
REM JAVA_TOOL_OPTIONS ^(algunos instaladores y antivirus lo hacen^), Java
REM imprime antes un "Picked up JAVA_TOOL_OPTIONS:..." y la version pasa a la
REM segunda linea.
set "VERJAVA="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    if not defined VERJAVA set "VERJAVA=%%~v"
)
for /f "tokens=1 delims=." %%m in ("!VERJAVA!") do set "JAVAMAYOR=%%m"

if not defined JAVAMAYOR goto :faltaJava
if !JAVAMAYOR! LSS 21 goto :javaViejo

echo         Java !VERJAVA! - correcto.

REM ======================================================================
REM  2. POSTGRESQL: servicio en marcha + base de datos creada
REM ======================================================================
echo.
echo   [2/3] Comprobando la base de datos ^(PostgreSQL^)...

set "PSQL="
for %%v in (17 16 15 14) do (
    if not defined PSQL (
        if exist "C:\Program Files\PostgreSQL\%%v\bin\psql.exe" (
            set "PSQL=C:\Program Files\PostgreSQL\%%v\bin\psql.exe"
        )
    )
)
if not defined PSQL goto :faltaPostgres

echo         PostgreSQL encontrado.

REM Arrancar el servicio por si estuviera parado (si ya corre, no pasa nada)
for /f "tokens=2 delims=: " %%s in ('sc query state^= all ^| findstr /i "SERVICE_NAME.*postgresql"') do (
    net start "%%s" >nul 2>&1
)

REM Primero probamos si la base de datos YA esta lista. Si es que si, no
REM hace falta molestar al usuario pidiendole la contrasena de postgres.
set "PGPASSWORD=tpv"
"!PSQL!" -h localhost -U tpv -d tpv -c "SELECT 1;" >nul 2>&1
if not errorlevel 1 (
    set "PGPASSWORD="
    echo         La base de datos ya estaba creada - correcto.
    goto :compilar
)
set "PGPASSWORD="

echo.
echo         Falta crear la base de datos de la tienda.
echo         Te va a pedir la contrasena del usuario "postgres":
echo         es la que escribiste al INSTALAR PostgreSQL.
echo.

"!PSQL!" -h localhost -U postgres -c "CREATE USER tpv WITH PASSWORD 'tpv';" 2>nul
"!PSQL!" -h localhost -U postgres -c "CREATE DATABASE tpv OWNER tpv;" 2>nul

REM Comprobamos de verdad que ha funcionado, conectandonos como el usuario tpv
set "PGPASSWORD=tpv"
"!PSQL!" -h localhost -U tpv -d tpv -c "SELECT 1;" >nul 2>&1
set "RESULTADOBD=!errorlevel!"
set "PGPASSWORD="

if not "!RESULTADOBD!"=="0" goto :falloBaseDatos
echo         Base de datos creada - correcto.

REM ======================================================================
REM  3. COMPILAR EL PROGRAMA
REM ======================================================================
:compilar
echo.
echo   [3/3] Preparando el programa de la tienda...
echo         ^(la primera vez tarda VARIOS MINUTOS: se descarga todo lo
echo          necesario de internet. Dejalo trabajar, no cierres nada.^)
echo.

pushd backend
call mvnw.cmd -Pcompleto clean package -DskipTests
set "RESULTADOCOMP=!errorlevel!"
popd

if not "!RESULTADOCOMP!"=="0" goto :falloCompilacion

REM Dejamos el programa con un nombre fijo y facil, en la carpeta principal,
REM para que "arrancar-tpv" no dependa del numero de version del archivo.
copy /y "backend\target\tpv-backend-0.1.0-SNAPSHOT.jar" "tpv.jar" >nul
if errorlevel 1 goto :falloCompilacion

REM --- Accesos directos en el escritorio, ya que estamos --------------
echo.
echo         Creando los accesos directos en el escritorio...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0crear-accesos-directos.ps1" >nul 2>&1
if errorlevel 1 (
    echo         AVISO: no he podido crear los accesos directos.
    echo         No es grave: puedes crearlos luego con "crear-accesos-directos.bat".
) else (
    echo         Accesos directos creados.
)

echo.
echo   ==========================================================
echo     LISTO. El ordenador ya esta preparado.
echo   ==========================================================
echo.
echo   A partir de ahora, en el dia a dia, usa los accesos directos
echo   del escritorio:
echo.
echo     - Al abrir la tienda:   "Arrancar Alimentacion Miguel"
echo     - Al cerrar la tienda:  "Parar Alimentacion Miguel"
echo.
echo   IMPORTANTE, antes del primer dia de verdad:
echo     1. Entra con admin / admin123 y CAMBIA las contrasenas
echo        desde el apartado "Empleados".
echo     2. Sube tu catalogo real desde "Productos" - "Importar CSV".
echo     3. Prueba la impresora y el lector de codigo de barras.
echo.
echo %date% %time% [preparar-pc] Preparacion completada correctamente >> ".registros\actividad.log"
pause
exit /b 0


REM ======================================================================
REM  MENSAJES DE ERROR
REM ======================================================================
:faltaJava
echo.
echo   ERROR: no tienes Java instalado ^(o no lo encuentro^).
echo.
echo   Que hacer:
echo     1. Entra en   https://adoptium.net
echo     2. Descarga "Temurin 21 ^(LTS^)" para Windows.
echo     3. Instalalo dando a Siguiente en todo.
echo     4. Cierra esta ventana y vuelve a ejecutar "preparar-pc".
echo.
echo %date% %time% [preparar-pc] ERROR: falta Java >> ".registros\actividad.log"
pause
exit /b 1

:javaViejo
echo.
echo   ERROR: tienes Java !VERJAVA!, pero hace falta Java 21 o superior.
echo.
echo   Que hacer:
echo     1. Entra en   https://adoptium.net
echo     2. Descarga e instala "Temurin 21 ^(LTS^)" para Windows.
echo     3. Cierra esta ventana y vuelve a ejecutar "preparar-pc".
echo.
echo %date% %time% [preparar-pc] ERROR: Java !VERJAVA! es anterior a la 21 >> ".registros\actividad.log"
pause
exit /b 1

:faltaPostgres
echo.
echo   ERROR: no encuentro PostgreSQL ^(la base de datos^).
echo.
echo   Que hacer:
echo     1. Entra en   https://www.postgresql.org/download/windows/
echo     2. Descarga el instalador y ejecutalo.
echo     3. Cuando te pida una contrasena para el usuario "postgres",
echo        APUNTALA: te la pedira este script dentro de un momento.
echo     4. En el resto de pasos, dale a Siguiente sin cambiar nada.
echo     5. Cierra esta ventana y vuelve a ejecutar "preparar-pc".
echo.
echo %date% %time% [preparar-pc] ERROR: falta PostgreSQL >> ".registros\actividad.log"
pause
exit /b 1

:falloBaseDatos
echo.
echo   ERROR: no he podido dejar lista la base de datos.
echo.
echo   Lo mas habitual es que la contrasena de "postgres" no fuera
echo   la correcta. Vuelve a ejecutar "preparar-pc" e introducela otra vez.
echo.
echo %date% %time% [preparar-pc] ERROR: no se pudo preparar la base de datos >> ".registros\actividad.log"
pause
exit /b 1

:falloCompilacion
echo.
echo   ERROR: no he podido preparar el programa.
echo.
echo   Lo mas habitual es que se haya cortado internet a mitad
echo   ^(hace falta para descargar lo necesario la primera vez^).
echo   Comprueba la conexion y vuelve a ejecutar "preparar-pc".
echo.
echo %date% %time% [preparar-pc] ERROR: fallo la compilacion >> ".registros\actividad.log"
pause
exit /b 1
