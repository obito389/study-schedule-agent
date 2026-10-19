$ErrorActionPreference='Stop'
Push-Location (Join-Path (Split-Path -Parent $PSScriptRoot) 'frontend')
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
    }
    & npm.cmd run dev
} finally { Pop-Location }
