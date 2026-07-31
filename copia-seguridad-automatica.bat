@echo off
REM Version silenciosa de copia-seguridad.bat: no abre ventanas ni espera
REM que se pulse ninguna tecla, pensada para que la ejecute sola la propia
REM app (parar-tpv.bat) sin interrumpir al cerrar la tienda. Si algo falla,
REM queda anotado en .registros\actividad.log para poder revisarlo luego.
cd /d "%~dp0"
if not exist ".registros" mkdir ".registros"
set "LOG=.registros\actividad.log"

set "PGDUMP="
for %%v in (17 16 15 14) do (
    if not defined PGDUMP (
        if exist "C:\Program Files\PostgreSQL\%%v\bin\pg_dump.exe" (
            set "PGDUMP=C:\Program Files\PostgreSQL\%%v\bin\pg_dump.exe"
        )
    )
)

if not defined PGDUMP (
    echo %date% %time% [copia automatica] ERROR: no se encontro pg_dump.exe >> "%LOG%"
    exit /b 1
)

for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm"') do set "SELLO=%%d"
if not exist "copias" mkdir "copias"
set "DESTINO=copias\tpv_%SELLO%.sql"

set "PGPASSWORD=tpv"
"%PGDUMP%" -h localhost -p 5432 -U tpv -d tpv -f "%DESTINO%" --clean --if-exists >nul 2>&1
set "RESULTADO=%errorlevel%"
set "PGPASSWORD="

if not "%RESULTADO%"=="0" (
    echo %date% %time% [copia automatica] ERROR: la copia ha fallado >> "%LOG%"
    exit /b 1
)

echo %date% %time% [copia automatica] OK: guardada en %DESTINO% >> "%LOG%"

powershell -NoProfile -Command "Get-ChildItem 'copias\tpv_*.sql' | Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-60) } | Remove-Item -Force" >nul 2>&1
exit /b 0
