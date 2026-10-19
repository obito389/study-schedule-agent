$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'start-mysql-sandbox.ps1')
$previousUrl=$env:SPRING_DATASOURCE_URL
$previousUser=$env:SPRING_DATASOURCE_USERNAME
$previousPassword=$env:SPRING_DATASOURCE_PASSWORD
try {
    $env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3308/team_workflow_test?serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false'
    $env:SPRING_DATASOURCE_USERNAME='root'
    $env:SPRING_DATASOURCE_PASSWORD=''
    & (Join-Path $projectRoot 'backend/mvnw.cmd') -f (Join-Path $projectRoot 'backend/pom.xml') -B test
    if ($LASTEXITCODE -ne 0) { throw 'MySQL integration tests failed.' }
} finally {
    $env:SPRING_DATASOURCE_URL=$previousUrl
    $env:SPRING_DATASOURCE_USERNAME=$previousUser
    $env:SPRING_DATASOURCE_PASSWORD=$previousPassword
}
