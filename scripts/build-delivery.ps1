$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $projectRoot 'frontend')
try {
    & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed' }
    & npm.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed' }
    & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
} finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd -B -Pweb package; if ($LASTEXITCODE -ne 0) { throw 'Backend tests or packaging failed' } } finally { Pop-Location }
$deliveryPath=Join-Path $projectRoot 'delivery'
New-Item -ItemType Directory -Path $deliveryPath -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $projectRoot 'backend/target/study-planner-backend-0.0.1-SNAPSHOT.jar') -Destination (Join-Path $deliveryPath 'study-planner.jar')
Copy-Item -LiteralPath (Join-Path $projectRoot 'docs/delivery/README.md') -Destination (Join-Path $deliveryPath 'README.md')
Write-Output "Delivery jar: $deliveryPath/study-planner.jar"
