param([int]$Port=8082)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$backendPath=Join-Path $projectRoot 'backend'
$jarPath=Join-Path $backendPath 'target/study-planner-backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Run backend/mvnw.cmd package first' }
$runId=[guid]::NewGuid().ToString('N')
$databasePath=(Join-Path $backendPath "target/restart-$runId").Replace('\','/')
$javaTempPath=Join-Path $env:PUBLIC 'studyplanner-java-tmp'
New-Item -ItemType Directory -Path $javaTempPath -Force | Out-Null
$baseUrl="http://127.0.0.1:$Port/api/v1"
$arguments=@(('"-Djdk.net.unixdomain.tmpdir='+$javaTempPath+'"'),'-jar',('"'+$jarPath+'"'),"--server.port=$Port",('"--spring.datasource.url=jdbc:h2:file:'+$databasePath+';MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"'))
$serverProcess=$null
function Start-TestServer([int]$Attempt) {
    $process=Start-Process -FilePath 'java' -ArgumentList $arguments -WorkingDirectory $backendPath -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $backendPath "target/restart-$runId-$Attempt.log") -RedirectStandardError (Join-Path $backendPath "target/restart-$runId-$Attempt.err")
    for ($poll=0;$poll -lt 90;$poll++) {
        if ($process.HasExited) { throw "Test server exited; inspect target/restart-$runId-$Attempt.log" }
        try { $null=Invoke-RestMethod "$baseUrl/health"; return $process } catch { Start-Sleep -Milliseconds 500 }
    }
    Stop-Process -Id $process.Id
    throw 'Timed out starting test server'
}
try {
    $serverProcess=Start-TestServer 1
    $command=@{operationKey=$runId;baseRevision=0;type='CREATE_TASK';authorizationScope='schedule:write:self';payload=@{title='Restart persistence acceptance';deadline=[DateTime]::UtcNow.AddDays(2).ToString('o');durationMinutes=60;priority=3}} | ConvertTo-Json -Depth 5
    $receipt=Invoke-RestMethod "$baseUrl/actions" -Method Post -ContentType 'application/json' -Body $command
    if ($receipt.status -ne 'APPLIED') { throw 'Initial write failed' }
    & taskkill /PID $serverProcess.Id /T /F | Out-Null
    $serverProcess.WaitForExit()
    $serverProcess=Start-TestServer 2
    $snapshot=Invoke-RestMethod "$baseUrl/schedule"
    $replayed=Invoke-RestMethod "$baseUrl/actions" -Method Post -ContentType 'application/json' -Body $command
    if ($snapshot.tasks.Count -ne 1 -or $snapshot.planningRevision -ne 1 -or $receipt.executionId -ne $replayed.executionId) {
        Write-Output "Persistence diagnostics: tasks=$($snapshot.tasks.Count), revision=$($snapshot.planningRevision), sameExecution=$($receipt.executionId -eq $replayed.executionId)"
        throw 'Restart persistence or idempotent replay failed'
    }
    Write-Output 'PASS: task, revision and idempotent receipt survive backend restart.'
} finally {
    if ($null -ne $serverProcess) {
        & taskkill /PID $serverProcess.Id /T /F 2>$null | Out-Null
        $null=$serverProcess.WaitForExit(10000)
    }
}
