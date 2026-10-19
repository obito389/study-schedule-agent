param([switch]$Browser)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd -B package; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' } } finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'frontend')
try {
    & npm.cmd ci; if ($LASTEXITCODE -ne 0) { throw 'Dependency installation failed' }
    & npm.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed' }
    & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    if ($Browser) {
        & npx.cmd playwright install chromium; if ($LASTEXITCODE -ne 0) { throw 'Browser installation failed' }
        & npm.cmd run test:e2e; if ($LASTEXITCODE -ne 0) { throw 'Browser tests failed' }
    }
} finally { Pop-Location }
