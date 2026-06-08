Set-Location $PSScriptRoot

$out = Join-Path $PSScriptRoot "target\classes"
$src = Join-Path $PSScriptRoot "src\main\java"
New-Item -ItemType Directory -Force -Path $out | Out-Null

Write-Host "Compiling..." -ForegroundColor Cyan
$files = @(Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& javac -d $out -sourcepath $src @files

if ($LASTEXITCODE -ne 0) {
    Write-Host "COMPILE FAILED." -ForegroundColor Red
    exit 1
}

Write-Host "Launching GUI..." -ForegroundColor Green
& java -cp $out com.tradingengine.MainGUI
