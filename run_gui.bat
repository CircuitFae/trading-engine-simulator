@echo off
cd /d "%~dp0"
echo Launching Trading Engine GUI...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run_gui.ps1"
if errorlevel 1 (
    echo.
    echo FAILED. Make sure JDK is installed: javac -version
    pause
    exit /b 1
)
