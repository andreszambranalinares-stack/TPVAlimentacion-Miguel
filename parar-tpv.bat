@echo off
chcp 65001 >nul
title Parando Alimentacion Miguel
cd /d "%~dp0"
if not exist ".registros" mkdir ".registros"
echo %date% %time% [parar-tpv] Cerrando la aplicacion >> ".registros\actividad.log"

echo.
echo   Cerrando la aplicacion...
echo.

REM Cierra las dos ventanas que abrio arrancar-tpv.bat (y los procesos de dentro,
REM como el motor Java o el servidor de la pantalla, gracias a /t).
REM IMPORTANTE: el filtro WINDOWTITLE de taskkill exige el titulo EXACTO;
REM con un "*" al final (como tenia esta linea antes) no encuentra nada
REM y no cierra nada, aunque no avise de ningun error.
set "CERRADO=0"

taskkill /fi "WINDOWTITLE eq TPV - Motor (no cerrar)" /t /f >nul 2>&1
if %errorlevel%==0 set "CERRADO=1"

taskkill /fi "WINDOWTITLE eq TPV - Pantalla (no cerrar)" /t /f >nul 2>&1
if %errorlevel%==0 set "CERRADO=1"

if "%CERRADO%"=="1" (
    echo   Aplicacion cerrada.
    echo %date% %time% [parar-tpv] Aplicacion cerrada correctamente >> ".registros\actividad.log"
) else (
    echo   No se ha encontrado ninguna ventana abierta de la aplicacion.
    echo   Si aun asi ves alguna ventana negra abierta, cierrala pulsando la X.
    echo %date% %time% [parar-tpv] AVISO: no se encontro ninguna ventana abierta >> ".registros\actividad.log"
)
echo   La base de datos sigue en marcha ^(no molesta^).
echo.

REM Copia de seguridad automatica al cerrar, sin que haya que acordarse.
REM Si falla, queda anotado en .registros\actividad.log (no interrumpe el cierre).
echo   Guardando una copia de seguridad...
call "%~dp0copia-seguridad-automatica.bat"
if errorlevel 1 (
    echo   AVISO: la copia de seguridad automatica ha fallado esta vez.
    echo   Puedes intentarlo a mano con "copia-seguridad.bat", o mira
    echo   ".registros\actividad.log" para ver el detalle.
) else (
    echo   Copia de seguridad guardada.
)
echo.
timeout /t 6 /nobreak >nul
