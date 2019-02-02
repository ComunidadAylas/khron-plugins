#!/bin/bash
# Plugins de Paper del Proyecto Khron
# Copyright (C) 2019 Comunidad Aylas
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

function mostrarError {
	echo "! Ha ocurrido un error realizando las operaciones necesarias. Se aborta la ejecución del script."
	exit
}

echo "> Ejecutando objetivo de Maven de generación de Javadoc..."
mvn javadoc:aggregate
if [ $? -eq 0 ]; then
	echo "El Javadoc debería de haber sido generado en la carpeta javadoc."
	echo "************"
	echo "* ATENCIÓN *"
	echo "************"
	echo "Se realizarán comandos de git para cambiarse a la rama de la página de Javadoc. Estos comando pueden descartar cambios hechos en el repositorio local, y en general provocar pérdida de información".
	echo "¿Quieres continuar con la ejecución del script? (S/N)"
	read r
	if [ "$r" = 's' -o "$r" = 'S' ]; then
		echo "> Cambiando a la rama gh-pages..."
		git checkout gh-pages
		if [ $? -ne 0 ]; then
			mostrarError
		fi
		echo "> Eliminando ficheros ajenos al VCS de la rama del repositorio..."
		git clean -f -x -e "/javadoc/"
		if [ $? -ne 0 ]; then
			mostrarError
		fi
		echo "> Copiando nuevo Javadoc..."
		mv javadoc/* .
		if [ $? -ne 0 ]; then
			mostrarError
		fi
		# Este directorio debería de quedar vacío tras la ejecución exitosa del mv anterior
		rmdir javadoc
		echo
		echo "Javadoc generado y colocado en gh-pages con éxito. Genera una confirmación (commit) y publica los cambios (push) cuando todo esté listo."
	fi
fi
