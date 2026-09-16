#!/bin/sh
set -eu
cd "$(dirname "$0")"
if ! command -v javac >/dev/null 2>&1; then
    echo "Instala un JDK 17 o superior o selecciona un JDK en IntelliJ."
    exit 1
fi
mkdir -p out/clases
javac --release 17 -encoding UTF-8 -d out/clases @fuentes.txt
exec java -cp out/clases colegio.Main "$@"
