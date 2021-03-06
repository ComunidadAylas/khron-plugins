#!/bin/sh
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

mostrarError() {
	echo "! Ha ocurrido un error realizando las operaciones necesarias. Se aborta la ejecución del script."
	exit
}

echo "> Ejecutando objetivo de Maven de generación de Javadoc..."
if mvn javadoc:aggregate; then
	echo "El Javadoc debería de haber sido generado en la carpeta javadoc."
	echo "************"
	echo "* ATENCIÓN *"
	echo "************"
	echo "Se realizarán comandos de git para cambiarse a la rama de la página de Javadoc. Estos comando pueden descartar cambios hechos en el repositorio local."
	echo "¿Quieres continuar con la ejecución del script? (S/N)"
	read -r r
	if [ "$r" = 's' ] || [ "$r" = 'S' ]; then
		echo "> Cambiando a la rama gh-pages..."
		if ! git checkout gh-pages; then
			mostrarError
		fi
		echo "> Eliminando ficheros ajenos al VCS de la rama del repositorio..."
		echo "  Ten en cuenta que es necesario que no se limpien los directorios javadoc y scripts."
		git clean -xdf -i
		echo "> Descartando cambios a ficheros controlados por el VCS..."
		if ! git checkout -- .; then
			mostrarError
		fi
		echo "> Moviendo nuevo Javadoc..."
		# Usamos cp seguido de rm porque mv no está diseñado para combinar directorios
		if ! cp -r javadoc/* .; then
			mostrarError
		fi
		if ! rm -rf javadoc; then
			mostrarError
		fi
		git add .
		echo
		echo "Javadoc generado y colocado en gh-pages con éxito. Genera una confirmación (commit) y publica los cambios (push) cuando todo esté listo."
	fi
fi
