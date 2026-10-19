param([switch]$MySql)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$backendPath=Join-Path $projectRoot 'backend'
Push-Location $backendPath
try {
    $jarPath=Join-Path $backendPath 'target/study-planner-backend-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jarPath)) {
        & .\mvnw.cmd -B package
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }
    }
    $javaTempPath=Join-Path $env:PUBLIC 'studyplanner-java-tmp'
    New-Item -ItemType Directory -Path $javaTempPath -Force | Out-Null
    $javaArgs=@("-Djdk.net.unixdomain.tmpdir=$javaTempPath",'-jar',$jarPath)
    if ($MySql) { $javaArgs+='--spring.profiles.active=mysql' }
    & java @javaArgs
} finally { Pop-Location }
