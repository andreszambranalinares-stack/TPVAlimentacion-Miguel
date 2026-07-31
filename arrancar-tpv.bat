@echo off
chcp 65001 >nul
title Arrancando Alimentacion Miguel
cd /d "%~dp0"

echo.
echo   ALIMENTACION MIGUEL - arrancando la aplicacion
echo   ==============================================
echo.

REM --- 1. Base de datos -------------------------------------------------
echo   [1/4] Comprobando la base de datos...
sc query "postgresql-x64-16" >nul 2>&1
if %errorlevel%==0 (
    net start "postgresql-x64-16" >nul 2>&1
) else (
    for /f "tokens=2 delims=: " %%s in ('sc query state^= all ^| findstr /i "SERVICE_NAME.*postgresql"') do (
        net start "%%s" >nul 2>&1
    )
)

REM --- 2. Dependencias de la pantalla (solo la primera vez) -------------
if not exist "frontend\node_modules" (
    echo   [2/4] Primera vez: instalando la pantalla. Esto tarda unos minutos...
    pushd frontend
    call npm install
    popd
    if errorlevel 1 (
        echo.
        echo   ERROR: no se pudo instalar la pantalla. Revisa que Node.js este instalado.
        pause
        exit /b 1
    )
) else (
    echo   [2/4] Pantalla ya preparada.
)

REM --- 3. Arrancar backend y pantalla en sus ventanas -------------------
echo   [3/4] Arrancando el motor y la pantalla...
start "TPV - Motor (no cerrar)" cmd /k "cd /d "%~dp0backend" && mvnw.cmd spring-boot:run"
start "TPV - Pantalla (no cerrar)" cmd /k "cd /d "%~dp0frontend" && npm run dev"

REM --- 4. Esperar a que el motor Y la pantalla respondan, y abrir el navegador
REM IMPORTANTE: hay que esperar a los DOS puertos (8080 motor, 5173 pantalla).
REM La pantalla arranca casi al instante, pero el motor (Java) tarda más,
REM sobre todo la primerísima vez: Maven tiene que descargarse TODAS las
REM librerías (no solo las nuevas), y eso puede tardar varios minutos según
REM la conexión a internet de la tienda. Le damos hasta 10 minutos y vamos
REM avisando cada 15 segundos para que no parezca que se ha quedado colgado.
echo   [4/4] Esperando a que el motor y la pantalla arranquen...
echo   ^(la primera vez puede tardar varios minutos: Maven se descarga las librerías^)
set /a intentos=0
:esperar
set /a intentos+=1
set /a resto=intentos %% 15
if %intentos% gtr 1 if %resto%==0 echo   ...todavía arrancando, un momento más ^(%intentos% segundos^)...
if %intentos% gtr 600 goto sinrespuesta
powershell -NoProfile -Command "try { $m = New-Object System.Net.Sockets.TcpClient; $m.Connect('localhost',8080); $m.Close(); $p = New-Object System.Net.Sockets.TcpClient; $p.Connect('localhost',5173); $p.Close(); exit 0 } catch { exit 1 }" >nul 2>&1
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto esperar
)

echo.
echo   Listo. Abriendo la aplicacion en el navegador...
start "" "http://localhost:5173"
echo.
echo   IMPORTANTE: no cierres las dos ventanas negras que se han abierto.
echo   Cuando termines la jornada, ejecuta "parar-tpv.bat".
echo.
timeout /t 6 /nobreak >nul
exit /b 0

:sinrespuesta
echo.
echo   La aplicacion tarda mucho mas de lo normal en arrancar ^(mas de 10 minutos^).
echo   Mira la ventana "TPV - Motor" a ver si muestra algun error.
echo   Probamos igualmente a abrir el navegador, por si ya esta casi lista:
start "" "http://localhost:5173"
echo.
pause
exit /b 1
