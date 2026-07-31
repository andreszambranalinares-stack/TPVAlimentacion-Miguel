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
REM OJO: el "if errorlevel 1" va ANTES del popd. Si fuera despues, el propio
REM popd (que casi siempre tiene exito) pisaria el codigo de error de "npm
REM install" y un fallo real de la instalacion pasaria desapercibido.
if not exist "frontend\node_modules" (
    echo   [2/4] Primera vez: instalando la pantalla. Esto tarda unos minutos...
    pushd frontend
    call npm install
    if errorlevel 1 (
        popd
        echo.
        echo   ERROR: no se pudo instalar la pantalla. Revisa que Node.js este instalado.
        echo %date% %time% [arrancar-tpv] ERROR: fallo npm install >> ".registros\actividad.log"
        pause
        exit /b 1
    )
    popd
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
REM
REM Usamos curl.exe (incluido de serie en Windows 10/11, en System32) en vez
REM de PowerShell: si el PC de la tienda tiene restringida la ejecución de
REM scripts de PowerShell (política de grupo, antivirus...), la comprobación
REM anterior fallaba SIEMPRE aunque el motor ya estuviera listo, y nunca se
REM abría el navegador solo. curl.exe no depende de eso y basta con que el
REM servidor responda algo (aunque sea un error 401) para saber que ya
REM está escuchando.
echo   [4/4] Esperando a que el motor y la pantalla arranquen...
echo   ^(la primera vez puede tardar varios minutos: Maven se descarga las librerías^)

REM Por si curl.exe no estuviera disponible (Windows muy antiguo o lo han
REM quitado): en vez de fallar sin explicación, esperamos un tiempo fijo
REM generoso y abrimos el navegador de todas formas.
where curl.exe >nul 2>&1
if errorlevel 1 (
    echo   ^(no encuentro curl.exe: espero 90 segundos a ciegas^)
    timeout /t 90 /nobreak >nul
    start "" "http://localhost:5173"
    echo.
    echo   IMPORTANTE: no cierres las dos ventanas negras que se han abierto.
    echo   Cuando termines la jornada, ejecuta "parar-tpv.bat".
    echo.
    timeout /t 6 /nobreak >nul
    exit /b 0
)

set /a intentos=0
:esperar
set /a intentos+=1
set /a resto=intentos %% 15
if %intentos% gtr 1 if %resto%==0 echo   ...todavía arrancando, un momento más ^(%intentos% segundos^)...
if %intentos% gtr 600 goto sinrespuesta
curl.exe -s -o nul --max-time 2 http://localhost:8080/api/auth/yo
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto esperar
)
curl.exe -s -o nul --max-time 2 http://localhost:5173
if errorlevel 1 (
    timeout /t 1 /nobreak >nul
    goto esperar
)

echo.
echo   Listo. Abriendo la aplicacion en el navegador...
echo %date% %time% [arrancar-tpv] Listo, navegador abierto >> ".registros\actividad.log"
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
echo %date% %time% [arrancar-tpv] AVISO: tardo mas de 10 minutos en responder >> ".registros\actividad.log"
start "" "http://localhost:5173"
echo.
pause
exit /b 1
