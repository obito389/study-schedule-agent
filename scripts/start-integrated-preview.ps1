param(
    [int]$Port = 8082,
    [int]$DatabasePort = 3308,
    [string]$Database = 'team_workflow_preview'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'backend/target/study-planner-backend-0.0.1-SNAPSHOT.jar'
$runtimePath = Join-Path $projectRoot '.runtime'
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Build frontend and backend with -Pweb first.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is in use; refusing to replace its server." }
New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:$DatabasePort/$Database`?serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME = 'root'
$arguments = @('-Djdk.net.unixdomain.tmpdir=C:/Users/Public', '-jar', ('"' + $jarPath + '"'), "--server.port=$Port", '--spring.datasource.password=')
$server = Start-Process -FilePath 'java' -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimePath 'preview.stdout.log') -RedirectStandardError (Join-Path $runtimePath 'preview.stderr.log')
$server.Id | Set-Content -LiteralPath (Join-Path $runtimePath 'preview.pid')
for ($attempt=0; $attempt -lt 60; $attempt++) {
    if ($server.HasExited) { throw 'Preview exited. Inspect .runtime/preview.stdout.log.' }
    try { $health=Invoke-RestMethod "http://127.0.0.1:$Port/api/v1/health"; Write-Output "Preview ready: http://127.0.0.1:$Port/ (PID $($server.Id))"; return } catch { Start-Sleep -Milliseconds 500 }
}
throw 'Preview did not become ready. Inspect .runtime logs; the process was left available for diagnosis.'
