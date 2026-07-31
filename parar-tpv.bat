@echo off
chcp 65001 >nul
title Parando Alimentacion Miguel

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
) else (
    echo   No se ha encontrado ninguna ventana abierta de la aplicacion.
    echo   Si aun asi ves alguna ventana negra abierta, cierrala pulsando la X.
)
echo   La base de datos sigue en marcha ^(no molesta^).
echo.
echo   Recuerda hacer la copia de seguridad de vez en cuando:
echo   ejecuta "copia-seguridad.bat".
echo.
timeout /t 6 /nobreak >nul
