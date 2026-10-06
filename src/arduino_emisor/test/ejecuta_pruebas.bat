@echo off
REM ============================================================================
REM Compila y ejecuta las pruebas de la placa Arduino en el PC.
REM
REM No hace falta la placa: se compila solo la LOGICA (Medidor.h y Publicador.h)
REM con un doble de la biblioteca Bluefruit, asi que se puede ejecutar en cualquier
REM ordenador con g++.
REM
REM Uso:  ejecuta.bat
REM ============================================================================

setlocal

cd /d "%~dp0"

echo.
echo =============================================================
echo  PRUEBAS DE LA PLACA ARDUINO
echo =============================================================
echo.

where g++ >nul 2>nul
if errorlevel 1 (
    echo ERROR: no se encuentra g++ en el PATH.
    echo Instala MinGW-w64 o añade la carpeta de g++ a las variables de entorno.
    exit /b 1
)

set FALLOS=0

echo --- Compilando test_medidor.cpp ---
g++ -std=c++17 -Wall -Wextra -o test_medidor.exe test_medidor.cpp
if errorlevel 1 (
    echo ERROR: no se ha podido COMPILAR test_medidor.cpp
    exit /b 1
)
echo     compilado.
echo.

echo --- Compilando test_publicador.cpp ---
g++ -std=c++17 -Wall -Wextra -o test_publicador.exe test_publicador.cpp
if errorlevel 1 (
    echo ERROR: no se ha podido COMPILAR test_publicador.cpp
    exit /b 1
)
echo     compilado.
echo.

REM ---------------------------------------------------------------- Medidor
test_medidor.exe
if errorlevel 1 set FALLOS=1

REM ---------------------------------------------------------------- Publicador
test_publicador.exe
if errorlevel 1 set FALLOS=1

echo.
echo =============================================================
if "%FALLOS%"=="0" (
    echo  RESULTADO: todas las pruebas de la placa PASAN
) else (
    echo  RESULTADO: hay pruebas de la placa que HAN FALLADO
)
echo =============================================================

REM Los .exe son temporales: se borran para no ensuciar la entrega.
del test_medidor.exe test_publicador.exe 2>nul

exit /b %FALLOS%