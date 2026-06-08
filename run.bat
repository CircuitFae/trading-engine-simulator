@echo off
cd /d "%~dp0"
echo Compiling trading-engine-simulator...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1"
if errorlevel 1 (
    echo.
    echo COMPILE FAILED. Make sure JDK is installed: javac -version
    pause
    exit /b 1
)
echo.
pause
