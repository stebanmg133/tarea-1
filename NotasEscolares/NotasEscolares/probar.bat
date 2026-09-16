@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 goto sinjava
if not exist "out\clases" mkdir "out\clases"
if not exist "out\pruebas" mkdir "out\pruebas"
javac --release 17 -encoding UTF-8 -d out/clases @fuentes.txt
if errorlevel 1 goto fin
javac --release 17 -encoding UTF-8 -cp out/clases -d out/pruebas pruebas/colegio/Pruebas.java
if errorlevel 1 goto fin
java -cp "out/clases;out/pruebas" colegio.Pruebas
goto fin
:sinjava
echo Necesitas un JDK 17 o superior en el PATH.
:fin
pause
