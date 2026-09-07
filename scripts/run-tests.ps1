# pro-api-test 批量运行入口（Karate Runner 时代）
param(
    [string]$Env      = "sit",
    [string]$JavaHome = "C:\Program Files\Java\jdk-23.0.1",
    [string]$Tag      = ""
)
if ($env:KARATE_ENV) { $Env = $env:KARATE_ENV }
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
Write-Host "[pro-api-test] karate.env=$Env  java=$(& java -version 2>&1 | Select-Object -First 1)"
$mvnArgs = @("-pl", "pro-api-test-aos", "-am", "clean", "test", "-Dkarate.env=$Env", "-Dsurefire.failIfNoSpecifiedTests=false")
if (-not [string]::IsNullOrWhiteSpace($Tag)) { $mvnArgs += "-Dtest=*Runner" }
$reportDir = Join-Path $root "target\karate-reports"
if (Test-Path $reportDir) { Remove-Item -Recurse -Force $reportDir -ErrorAction SilentlyContinue }
New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
& mvn @mvnArgs
$exitCode = $LASTEXITCODE
Get-ChildItem -Path $root -Directory -Recurse -Filter "karate-reports" -ErrorAction SilentlyContinue | ForEach-Object {
    $src = $_.FullName
    $dst = Join-Path $reportDir ("{0}\{1}" -f $_.Parent.Name, $_.Name)
    New-Item -ItemType Directory -Force -Path $dst | Out-Null
    Copy-Item -Path (Join-Path $src "*") -Destination $dst -Recurse -Force -ErrorAction SilentlyContinue
}
Write-Host ""
Write-Host "karate reports => $reportDir"
Write-Host "surefire   reports => pro-api-test-aos\target\surefire-reports\"
exit $exitCode
