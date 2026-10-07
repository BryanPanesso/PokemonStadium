@echo off
rem Compila y ejecuta sin Maven (solo requiere un JDK 11+ en el PATH).
setlocal
cd /d "%~dp0"
set CP=lib\json-20230227.jar
set SRC=src\main\java\stadium

echo Compilando Pokemon Stadium Lite...
if exist out rmdir /s /q out
javac --release 11 -encoding UTF-8 -d out -cp "%CP%" %SRC%\*.java %SRC%\model\*.java %SRC%\api\*.java %SRC%\battle\*.java %SRC%\ui\*.java
if errorlevel 1 (
    echo.
    echo Error de compilacion. Verifica que tengas un JDK 11 o superior: javac -version
    pause
    exit /b 1
)

java -cp "out;%CP%" stadium.App
