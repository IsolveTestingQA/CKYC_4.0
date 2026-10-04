# Open Allure report for CKYC_4.0 — static, timestamped, NOT tied to this terminal.
# Unlike `mvn allure:serve`, closing this window does not close the report: it generates a
# static report into Report Output\Allure Report\<date>\Manual_<timestamp>\ and serves it with
# a small background web server that keeps running after this script exits.
# Usage: .\open-allure.ps1

Set-Location $PSScriptRoot

if (-not (Test-Path "target\allure-results")) {
    Write-Host "ERROR: No results found. Run tests first (any TestRunner from IntelliJ, or: mvn test)" -ForegroundColor Red
    exit 1
}

$dateFolder = (Get-Date).ToString('dd_MMM_yyyy')
$stamp = (Get-Date).ToString('dd_MMM_yyyy_hh_mm_ss_tt')
$reportDir = "Report Output\Allure Report\$dateFolder\Manual_$stamp"

Write-Host "Generating static Allure report..." -ForegroundColor Yellow
mvn -q allure:report
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: mvn allure:report failed. Check your Maven/network setup." -ForegroundColor Red
    exit 1
}

$defaultOut = "target\site\allure-maven-plugin"
if (-not (Test-Path $defaultOut)) {
    Write-Host "ERROR: Expected output not found at $defaultOut" -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
Copy-Item "$defaultOut\*" $reportDir -Recurse -Force

$jwebserver = "jwebserver"
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jwebserver.exe")) {
    $jwebserver = "$env:JAVA_HOME\bin\jwebserver.exe"
}

$port = Get-Random -Minimum 9100 -Maximum 9500
Write-Host "Report ready: $reportDir" -ForegroundColor Green
Write-Host "Serving at http://127.0.0.1:$port/ — this keeps running in the background." -ForegroundColor Green
Write-Host "Safe to close this window; the report stays open until you stop the jwebserver process yourself." -ForegroundColor Yellow

Start-Process -WindowStyle Hidden -FilePath $jwebserver -ArgumentList "-b 127.0.0.1 -p $port -d `"$reportDir`""
Start-Sleep -Seconds 1
Start-Process "http://127.0.0.1:$port/"
