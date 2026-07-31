# Crea en el escritorio los accesos directos para arrancar, parar y hacer
# copia de seguridad del TPV, con el icono de la aplicacion.
# Se ejecuta a traves de "crear-accesos-directos.bat" (doble clic).

$aqui = Split-Path -Parent $MyInvocation.MyCommand.Path
$icono = Join-Path $aqui 'icono-tpv.ico'
$escritorio = [Environment]::GetFolderPath('Desktop')
$shell = New-Object -ComObject WScript.Shell

function Crear-Acceso {
    param([string]$nombre, [string]$script)
    $acceso = $shell.CreateShortcut((Join-Path $escritorio "$nombre.lnk"))
    $acceso.TargetPath = Join-Path $aqui $script
    $acceso.WorkingDirectory = $aqui
    $acceso.IconLocation = $icono
    $acceso.Save()
}

Write-Host ""
Write-Host "  Creando los accesos directos en el escritorio..."
Write-Host ""

Crear-Acceso 'Arrancar Alimentacion Miguel' 'arrancar-tpv.bat'
Crear-Acceso 'Parar Alimentacion Miguel' 'parar-tpv.bat'
Crear-Acceso 'Copia de seguridad - Alimentacion Miguel' 'copia-seguridad.bat'

Write-Host "  Listo. Se han creado 3 accesos directos en el escritorio:"
Write-Host "    - Arrancar Alimentacion Miguel"
Write-Host "    - Parar Alimentacion Miguel"
Write-Host "    - Copia de seguridad - Alimentacion Miguel"
Write-Host ""
Write-Host "  Ya puedes usarlos directamente, sin entrar en esta carpeta."
Write-Host ""
