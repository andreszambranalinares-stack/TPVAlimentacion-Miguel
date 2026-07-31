@echo off
chcp 65001 >nul
title Copia de seguridad - Alimentacion Miguel
cd /d "%~dp0"

echo.
echo   COPIA DE SEGURIDAD DE LA BASE DE DATOS
echo   ======================================
echo.

REM --- Localizar pg_dump (viene con PostgreSQL) -------------------------
set "PGDUMP="
for %%v in (17 16 15 14) do (
    if not defined PGDUMP (
        if exist "C:\Program Files\PostgreSQL\%%v\bin\pg_dump.exe" (
            set "PGDUMP=C:\Program Files\PostgreSQL\%%v\bin\pg_dump.exe"
        )
    )
)

if not defined PGDUMP (
    echo   ERROR: no encuentro pg_dump.exe.
    echo   Normalmente esta en C:\Program Files\PostgreSQL\16\bin
    echo   Comprueba que PostgreSQL este instalado.
    echo.
    pause
    exit /b 1
)

REM --- Nombre del archivo con fecha y hora ------------------------------
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm"') do set "SELLO=%%d"
if not exist "copias" mkdir "copias"
set "DESTINO=copias\tpv_%SELLO%.sql"

echo   Guardando en: %DESTINO%
echo.

set "PGPASSWORD=tpv"
"%PGDUMP%" -h localhost -p 5432 -U tpv -d tpv -f "%DESTINO%" --clean --if-exists
set "RESULTADO=%errorlevel%"
set "PGPASSWORD="

if not "%RESULTADO%"=="0" (
    echo.
    echo   ERROR: la copia ha fallado.
    echo   Comprueba que PostgreSQL este arrancado (abre la aplicacion y mira si funciona).
    echo.
    pause
    exit /b 1
)

echo.
echo   Copia hecha correctamente.
echo.
echo   IMPORTANTE: una copia en el mismo ordenador no te salva si se
echo   estropea el disco duro. Copia el archivo a un pendrive o a la nube.
echo.

REM --- Borrar copias de mas de 60 dias para que no se acumulen ----------
powershell -NoProfile -Command "Get-ChildItem 'copias\tpv_*.sql' | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-60) } | Remove-Item -Force" >nul 2>&1

echo   Abriendo la carpeta de copias...
start "" "%~dp0copias"
timeout /t 4 /nobreak >nul
