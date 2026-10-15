@echo off
REM EveryDay Dashboard Quick Launcher
cd /d "%~dp0"
if exist "dist\EveryDay\EveryDay.exe" (
    echo Launching EveryDay standalone desktop application...
    start "" "dist\EveryDay\EveryDay.exe"
) else (
    echo Launching EveryDay via bundled fat JAR...
    java -jar "target\everyday-dashboard-1.0.0-all.jar"
)
