@echo off
chcp 65001 >nul
title Arrancando Alimentacion Miguel
cd /d "%~dp0"
if not exist ".registros" mkdir ".registros"
echo %date% %time% [arrancar-tpv] Arrancando la aplicacion >> ".registros\actividad.log"

echo.
echo   ALIMENTACION MIGUEL - arrancando la aplicacion
echo   ==============================================
echo.

REM --- 0. Comprobar que el ordenador esta preparado ---------------------
REM Desde que la pantalla va incluida dentro del propio programa, aqui solo
REM hace falta "tpv.jar". Lo genera "preparar-pc.bat", que se ejecuta una
REM unica vez al montar el ordenador de la tienda.
if not exist "tpv.jar" (
    echo   Este ordenador todavia no esta preparado.
    echo.
    echo   Ejecuta primero "preparar-pc.bat" ^(solo hay que hacerlo una vez^).
    echo.
    echo %date% %time% [arrancar-tpv] AVISO: falta tpv.jar, hay que ejecutar preparar-pc >> ".registros\actividad.log"
    pause
    exit /b 1
)

REM --- 1. Base de datos -------------------------------------------------
echo   [1/3] Comprobando la base de datos...
sc query "postgresql-x64-16" >nul 2>&1
if %errorlevel%==0 (
    net start "postgresql-x64-16" >nul 2>&1
) else (
    for /f "tokens=2 delims=: " %%s in ('sc query state^= all ^| findstr /i "SERVICE_NAME.*postgresql"') do (
        net start "%%s" >nul 2>&1
    )
)

REM --- 2. Arrancar el programa (una sola ventana) -----------------------
echo   [2/3] Arrancando el programa...
start "TPV - Alimentacion Miguel (no cerrar)" cmd /k "java -jar "%~dp0tpv.jar""

REM --- 3. Esperar a que responda y abrir el navegador -------------------
REM Ahora solo hay UN puerto que esperar (8080): el mismo programa sirve
REM tanto los datos como la pantalla. Y ya no hay descargas que hacer
REM (eso lo dejo resuelto preparar-pc), asi que tarda segundos, no minutos.
REM
REM Usamos curl.exe (viene de serie en Windows 10/11, en System32) en vez de
REM PowerShell: si el PC de la tienda tiene restringida la ejecucion de
REM scripts de PowerShell (politica de grupo, antivirus...), la comprobacion
REM fallaria siempre aunque el programa ya estuviera listo. A curl le basta
REM con que el servidor responda algo, aunque sea un error 401.
echo   [3/3] Esperando a que el programa este listo...

where curl.exe >nul 2>&1
if errorlevel 1 (
    echo   ^(no encuentro curl.exe: espero 40 segundos a ciegas^)
    timeout /t 40 /nobreak >nul
    goto :abrir
)

set /a intentos=0
:esperar
set /a intentos+=1
set /a resto=intentos %% 15
if %intentos% gtr 1 if %resto%==0 echo   ...todavia arrancando ^(%intentos% segundos^)...
if %intentos% gtr 180 goto :sinrespuesta
curl.exe -s -o nul --max-time 2 http://localhost:8080/api/auth/yo
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto :esperar
)

:abrir
echo.
echo   Listo. Abriendo la aplicacion en el navegador...
echo %date% %time% [arrancar-tpv] Listo, navegador abierto >> ".registros\actividad.log"
start "" "http://localhost:8080"
echo.
echo   IMPORTANTE: no cierres la ventana negra que se ha abierto.
echo   Cuando termines la jornada, ejecuta "parar-tpv.bat".
echo.
timeout /t 6 /nobreak >nul
exit /b 0

:sinrespuesta
echo.
echo   El programa tarda mucho mas de lo normal en arrancar ^(mas de 3 minutos^).
echo   Mira la ventana "TPV - Alimentacion Miguel" a ver si muestra algun error.
echo   Lo mas habitual es que la base de datos no este arrancada.
echo   Probamos igualmente a abrir el navegador, por si ya esta casi lista:
echo %date% %time% [arrancar-tpv] AVISO: tardo mas de 3 minutos en responder >> ".registros\actividad.log"
start "" "http://localhost:8080"
echo.
pause
exit /b 1
