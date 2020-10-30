/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2020 Comunidad Aylas
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.alextmjugador.khron.tiemporeal.configuraciones;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;

/**
 * Modela un parámetro de configuración que contiene el texto a mostrar cuando
 * un jugador empuñe un reloj en una dimensión donde no hay un ciclo día-noche.
 *
 * @author AlexTMjugador
 */
public final class TextoRelojDimensionSinCiclo extends TextoReloj {
    /**
     * La ruta en el fichero de configuración hacia este parámetro de configuración.
     */
    private static final String RUTA_CONFIG = "Texto para relojes en dimensiones sin ciclo día-noche";

    /**
     * El identificador de este parámetro de configuración en el código y en el
     * comando asociado para cambiarlo.
     */
    private static final String ID_CONFIG = "textoRelojDimensionSinCiclo";

    /**
     * El permiso necesario para ejecutar un comando que cambie el valor de este
     * parámetro de configuración.
     */
    private static final String PERMISO_CONFIG = "tiemporeal.trconfig.textoRelojDimensionSinCiclo";

    public TextoRelojDimensionSinCiclo() {
        super(RUTA_CONFIG, ID_CONFIG, PERMISO_CONFIG, new PluginTiempoReal.NotificableConfigTextoRelojDimensionSinCiclo());
    }
}