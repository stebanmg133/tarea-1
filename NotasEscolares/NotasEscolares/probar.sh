#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p out/clases out/pruebas
javac --release 17 -encoding UTF-8 -d out/clases @fuentes.txt
javac --release 17 -encoding UTF-8 -cp out/clases -d out/pruebas pruebas/colegio/Pruebas.java
java -cp "out/clases:out/pruebas" colegio.Pruebas
