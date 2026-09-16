@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 goto sinjava
if not exist "out\clases" mkdir "out\clases"
javac --release 17 -encoding UTF-8 -d out/clases @fuentes.txt
if errorlevel 1 goto error
java -cp out/clases colegio.Main %*
goto fin
:sinjava
echo No se encontro javac. Instala un JDK 17 o superior y agregalo al PATH.
echo Tambien puedes abrir el proyecto en IntelliJ y seleccionar su JDK.
goto fin
:error
echo No se pudo compilar. Revisa el mensaje anterior y la version del JDK.
:fin
pause
