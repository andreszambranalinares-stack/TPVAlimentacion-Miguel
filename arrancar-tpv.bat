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

REM --- 4. Esperar a que el motor responda y abrir el navegador ----------
echo   [4/4] Esperando a que el motor arranque (puede tardar 1 minuto)...
set /a intentos=0
:esperar
set /a intentos+=1
if %intentos% gtr 120 goto sinrespuesta
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:5173' -UseBasicParsing -TimeoutSec 2; exit 0 } catch { exit 1 }" >nul 2>&1
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
echo   La aplicacion tarda mas de lo normal en arrancar.
echo   Mira la ventana "TPV - Motor" a ver si muestra algun error.
echo.
pause
exit /b 1
