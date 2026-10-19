param([int]$Port=3308)
$ErrorActionPreference='Stop'
$runtimePath=Join-Path $env:PUBLIC 'studyplanner-team-integration-20261020'
$dataPath=Join-Path $runtimePath 'mysql-data'
$binPath=Join-Path $env:ProgramFiles 'MySQL/MySQL Server 8.0/bin'
$mysql=Join-Path $binPath 'mysql.exe'
$mysqld=Join-Path $binPath 'mysqld.exe'
if (-not (Test-Path -LiteralPath $mysqld)) { throw 'Install MySQL 8.0 binaries first; existing services will not be changed.' }
$listener=Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    $owner=Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($owner.CommandLine -notlike "*$dataPath*") { throw "Port $Port belongs to another database; refusing to use it." }
} else {
    New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null
    if (-not (Test-Path -LiteralPath $dataPath)) {
        New-Item -ItemType Directory -Path $dataPath | Out-Null
        & $mysqld --no-defaults --initialize-insecure "--datadir=$dataPath" --console
        if ($LASTEXITCODE -ne 0) { throw 'Sandbox initialization failed.' }
    }
    $arguments=@('--no-defaults',"--datadir=$dataPath","--port=$Port",'--bind-address=127.0.0.1','--mysqlx=0','--skip-log-bin','--console')
    $server=Start-Process -FilePath $mysqld -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimePath 'mysql.stdout.log') -RedirectStandardError (Join-Path $runtimePath 'mysql.stderr.log')
    $server.Id | Set-Content -LiteralPath (Join-Path $runtimePath 'mysql.pid')
    for ($attempt=0;$attempt -lt 60;$attempt++) {
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { break }
        if ($server.HasExited) { throw 'Sandbox server exited. Inspect its logs.' }
        Start-Sleep -Milliseconds 500
    }
}
# A disposable, loopback-only sandbox. Never point this helper at an existing service.
& $mysql --no-defaults --host=127.0.0.1 "--port=$Port" --user=root --password= --execute='CREATE DATABASE IF NOT EXISTS team_workflow_test CHARACTER SET utf8mb4; CREATE DATABASE IF NOT EXISTS team_workflow_preview CHARACTER SET utf8mb4;'
if ($LASTEXITCODE -ne 0) { throw 'Sandbox database setup failed.' }
Write-Output "MySQL sandbox ready on 127.0.0.1:$Port; existing services and databases were not modified."
