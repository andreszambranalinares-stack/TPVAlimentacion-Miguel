@echo off
chcp 65001 >nul
title Parando Alimentacion Miguel

echo.
echo   Cerrando la aplicacion...
echo.

REM Cierra las dos ventanas que abrio arrancar-tpv.bat (y los procesos de dentro).
taskkill /fi "WINDOWTITLE eq TPV - Motor (no cerrar)*" /t /f >nul 2>&1
taskkill /fi "WINDOWTITLE eq TPV - Pantalla (no cerrar)*" /t /f >nul 2>&1

echo   Aplicacion cerrada. La base de datos sigue en marcha (no molesta).
echo.
echo   Recuerda hacer la copia de seguridad de vez en cuando:
echo   ejecuta "copia-seguridad.bat".
echo.
timeout /t 6 /nobreak >nul
