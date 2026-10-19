<#
.SYNOPSIS
    Verifica la imagen Docker de la API de punta a punta (Fase 5, riesgo R07).

.DESCRIPTION
    1. docker build -t challenge-api .            (comando del enunciado)
    2. docker run -p 8080:8080 challenge-api      (comando del enunciado)
    3. HEALTHCHECK del contenedor en "healthy"
    4. GET /actuator/health responde UP
    5. El contenedor no corre como root
    6. POST /orders responde 201 y su log sale en JSON con el requestId enviado
    7. Tamaño de la imagen
    8. Karate (30 escenarios) contra el contenedor con docker compose

    Requisitos: Git y Docker en ejecución. No requiere Java.
    Al final muestra un resumen y lo guarda en target/verificacion-docker.txt.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\scripts\verificar-docker.ps1
#>

$ErrorActionPreference = 'Continue'
Set-Location (Split-Path -Parent $PSScriptRoot)

$Image = 'challenge-api'
$Container = 'challenge-api-verificacion'
$RequestId = 'verificacion-docker-1'
$Results = New-Object System.Collections.Generic.List[object]

function Add-Result([string]$Check, [bool]$Ok, [string]$Detail) {
    $status = 'FALLA'
    if ($Ok) { $status = 'OK' }
    $Results.Add([pscustomobject]@{ Verificacion = $Check; Resultado = $status; Detalle = $Detail })
}

function Show-Summary {
    $table = $Results | Format-Table -AutoSize -Wrap | Out-String
    $header = "Verificacion Docker - $(Get-Date -Format 'yyyy-MM-dd HH:mm') - commit $(git rev-parse --short HEAD)"
    Write-Host ''
    Write-Host $header -ForegroundColor Cyan
    Write-Host $table
    New-Item -ItemType Directory -Force -Path target | Out-Null
    ($header + [Environment]::NewLine + $table) | Out-File -Encoding utf8 target/verificacion-docker.txt
    Write-Host 'Resumen guardado en target/verificacion-docker.txt' -ForegroundColor Cyan
}

function Remove-VerificationContainer {
    $existing = docker ps -aq --filter "name=^$Container$"
    if ($existing) { docker rm -f $Container | Out-Null }
}

Write-Host '== Docker' -ForegroundColor Cyan
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host 'No se encontró el comando docker: instala Docker Desktop y abre una terminal nueva.' -ForegroundColor Red
    exit 1
}
docker version --format 'Cliente {{.Client.Version}} / Servidor {{.Server.Version}}'
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Docker no responde: abre Docker Desktop y espera a que el motor esté en ejecución.' -ForegroundColor Red
    exit 1
}

# 1. Construir la imagen
Write-Host "== 1. docker build -t $Image ." -ForegroundColor Cyan
docker build -t $Image .
Add-Result 'docker build -t challenge-api .' ($LASTEXITCODE -eq 0) "exit $LASTEXITCODE"
if ($LASTEXITCODE -ne 0) { Show-Summary; exit 1 }

# 2. Ejecutar el contenedor
Write-Host '== 2. docker run -p 8080:8080 challenge-api' -ForegroundColor Cyan
Remove-VerificationContainer
docker run -d --name $Container -p 8080:8080 $Image | Out-Null
Add-Result 'docker run -p 8080:8080 challenge-api' ($LASTEXITCODE -eq 0) "exit $LASTEXITCODE"

# 3. Esperar el HEALTHCHECK
Write-Host '== 3. Esperando HEALTHCHECK (máx. 2 min)' -ForegroundColor Cyan
$health = ''
for ($i = 0; $i -lt 60; $i++) {
    $health = docker inspect --format '{{.State.Health.Status}}' $Container
    if ($health -eq 'healthy' -or $health -eq 'unhealthy') { break }
    Start-Sleep -Seconds 2
}
Add-Result 'HEALTHCHECK del contenedor' ($health -eq 'healthy') "estado: $health"

# 4. Endpoint de salud desde el host
try {
    $h = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 10
    Add-Result 'GET /actuator/health' ($h.status -eq 'UP') "status: $($h.status)"
} catch {
    Add-Result 'GET /actuator/health' $false $_.Exception.Message
}

# 5. Usuario sin privilegios
$uid = docker exec $Container id -u
Add-Result 'Contenedor sin usuario root' (($uid -ne $null) -and ($uid.Trim() -ne '0')) "uid: $uid"

# 6. POST y log JSON con requestId
$body = '{"cliente":"Juan Perez","productos":[{"id":"P001","cantidad":2,"valorUnitario":100000}]}'
try {
    $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri 'http://localhost:8080/orders' `
        -ContentType 'application/json' -Headers @{ 'X-Request-Id' = $RequestId } -Body $body -TimeoutSec 10
    $order = $r.Content | ConvertFrom-Json
    Add-Result 'POST /orders' ($r.StatusCode -eq 201 -and $order.total -eq 200000) "HTTP $($r.StatusCode), total $($order.total)"
} catch {
    Add-Result 'POST /orders' $false $_.Exception.Message
}
Start-Sleep -Seconds 1
$logLine = docker logs $Container | Select-String -SimpleMatch 'Orden creada' | Select-Object -Last 1
$jsonOk = $false
$detail = 'no se encontró la línea "Orden creada"'
if ($logLine) {
    try {
        $entry = $logLine.Line | ConvertFrom-Json
        $jsonOk = ($entry.requestId -eq $RequestId)
        $detail = "requestId en el log: $($entry.requestId)"
    } catch {
        $detail = 'la línea no es JSON: ' + $logLine.Line.Substring(0, [Math]::Min(80, $logLine.Line.Length))
    }
}
Add-Result 'Log JSON con requestId' $jsonOk $detail

# 7. Tamaño de la imagen
$size = docker image inspect --format '{{.Size}}' $Image
Add-Result 'Tamaño de la imagen' ($size -ne $null) ('{0:N0} MB' -f ([double]$size / 1MB))

Remove-VerificationContainer

# 8. Karate contra el contenedor
Write-Host '== 8. Karate contra el contenedor (la primera vez descarga dependencias de Maven)' -ForegroundColor Cyan
docker compose --profile acceptance run --rm karate | Tee-Object -Variable karateOutput
$karateExit = $LASTEXITCODE
$summaryLine = $karateOutput | Select-String -Pattern 'scenarios:\s+\d+\s+\|\s+passed:\s+\d+\s+\|\s+failed:\s+\d+' | Select-Object -Last 1
$karateDetail = "exit $karateExit"
if ($summaryLine) { $karateDetail = ($summaryLine.Line -replace '\s+', ' ').Trim() }
Add-Result 'Karate contra el contenedor' ($karateExit -eq 0) $karateDetail
docker compose --profile acceptance down | Out-Null

Show-Summary
if ($Results | Where-Object { $_.Resultado -eq 'FALLA' }) { exit 1 }
exit 0
