@echo off
chcp 65001 >nul
title Creando accesos directos - Alimentacion Miguel
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0crear-accesos-directos.ps1"

echo.
pause
