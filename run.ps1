Set-Location $PSScriptRoot

$out = Join-Path $PSScriptRoot "target\classes"
$src = Join-Path $PSScriptRoot "src\main\java"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$files = @(Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
if ($files.Count -eq 0) {
    Write-Host "No Java source files found under $src" -ForegroundColor Red
    exit 1
}

& javac -d $out -sourcepath $src @files
if ($LASTEXITCODE -ne 0) {
    Write-Host "`nCOMPILE FAILED. Install JDK and ensure javac is on PATH." -ForegroundColor Red
    exit 1
}

Write-Host "`nRunning com.tradingengine.Main...`n"
& java -cp $out com.tradingengine.Main
exit $LASTEXITCODE
