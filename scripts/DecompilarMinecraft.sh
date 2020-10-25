#!/bin/sh
# Plugins de Paper del Proyecto Khron
# Copyright (C) 2020 Comunidad Aylas
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.

# El URL base que se usará para construir el URL de descarga de la
# bifurcación de Enigma de Fabric
readonly URL_DESCARGA_ENIGMA=https://maven.fabricmc.net/cuchaz/enigma-cli/0.21.5%2Bbuild.226/enigma-cli-0.21.5%2Bbuild.226-all.jar
# Los URL de descarga del JAR y sus mapeos de ofuscación de Proguard.
# Pueden deducirse a partir de la respuesta dada por
# https://launchermeta.mojang.com/mc/game/version_manifest.json,
# donde el JSON de cada versión de "versions" sigue el formato definido en
# https://minecraft.gamepedia.com/Client.json
readonly URL_DESCARGA_JAR_SERVER=https://launcher.mojang.com/v1/objects/f02f4473dbf152c23d7d484952121db0b36698cb/server.jar
readonly URL_DESCARGA_MAPEOS_PROGUARD_SERVER=https://launcher.mojang.com/v1/objects/e75ff1e729aec4a3ec6a94fe1ddd2f5a87a2fd00/server.txt
readonly URL_DESCARGA_JAR_CLIENT=https://launcher.mojang.com/v1/objects/1321521b2caf934f7fc9665aab7e059a7b2bfcdf/client.jar
readonly URL_DESCARGA_MAPEOS_PROGUARD_CLIENT=https://launcher.mojang.com/v1/objects/faac5028fbca3859db970cc4ca041aeec55f6d9d/client.txt
# El ejecutable de Java a usar. Si se deja en blanco, se deducirá
# a partir de la variable de entorno PATH
readonly EJECUTABLE_JAVA=
# El directorio donde este script creará ficheros
readonly DIRECTORIO_FICHEROS="scripts/$1-decompilado"

descargarFichero() {
	echo "> Descargando $3..."

	if [ -f "$1" ]; then
		codigo_http=$(curl -L -z "$1" -o "$1" -w "%{http_code}" "$2")
	else
		codigo_http=$(curl -L -o "$1" -w "%{http_code}" "$2")
	fi && [ "$codigo_http" -ge 200 ] && [ "$codigo_http" -lt 400 ]
}

argumento_mayusculas=$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')
eval url_jar='$'URL_DESCARGA_JAR_"$argumento_mayusculas"
eval url_mapeos='$'URL_DESCARGA_MAPEOS_PROGUARD_"$argumento_mayusculas"

# shellcheck disable=SC2154
if [ -z "$url_jar" ] || [ -z "$url_mapeos" ]; then
	echo "! \"$1\" no es un artefacto de Minecraft válido." >&2
	exit 1
else
	mkdir -p "$DIRECTORIO_FICHEROS/codigo-fuente-$1" && \
	descargarFichero "$DIRECTORIO_FICHEROS/enigma-cli.jar" $URL_DESCARGA_ENIGMA 'Enigma' && \
	descargarFichero "$DIRECTORIO_FICHEROS/$1.jar" "$url_jar" "JAR vanilla de Minecraft ($1)" && \
	descargarFichero "$DIRECTORIO_FICHEROS/$1.txt" "$url_mapeos" "mapeos de Mojang del JAR ($1)" && \
	echo '> Convirtiendo mapeos de Mojang en formato Proguard a formato Enigma...' && \
	"${EJECUTABLE_JAVA:-java}" -jar "$DIRECTORIO_FICHEROS/enigma-cli.jar" convert-mappings \
	'Proguard' "$DIRECTORIO_FICHEROS/$1.txt" 'enigma_zip' "$DIRECTORIO_FICHEROS/$1_enigma.zip" && \
	echo '> Decompilando...' && \
	"${EJECUTABLE_JAVA:-java}" -jar "$DIRECTORIO_FICHEROS/enigma-cli.jar" decompile 'CFR' \
	"$DIRECTORIO_FICHEROS/$1.jar" "$DIRECTORIO_FICHEROS/codigo-fuente-$1" "$DIRECTORIO_FICHEROS/$1_enigma.zip"
fi
