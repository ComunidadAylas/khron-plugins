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
package org.aylas.khron.tiemporeal.configuraciones;

import java.util.regex.Pattern;

import org.aylas.khron.libconfig.ParametroConfiguracion;
import org.aylas.khron.tiemporeal.PluginTiempoReal;

/**
 * Alberga la clave a usar para operar con el API de Weatherbit.
 *
 * @author AlexTMjugador
 */
public final class ClaveWeatherbit extends ParametroConfiguracion<String, String> {
    /**
     * La ruta en el fichero de configuración hacia este parámetro de configuración.
     */
    private static final String RUTA_CONFIG = "Clave de la API de Weatherbit";

    /**
     * El identificador de este parámetro de configuración en el código y en el
     * comando asociado para cambiarlo.
     */
    private static final String ID_CONFIG = "claveWeatherbit";

    /**
     * El permiso necesario para ejecutar un comando que cambie el valor de este
     * parámetro de configuración.
     */
    private static final String PERMISO_CONFIG = "tiemporeal.trconfig.claveWeatherbit";

    /**
     * La expresión regular que siguen las claves válidas del API de Weatherbit.
     */
    private static final Pattern PATRON_CLAVE = Pattern.compile("[a-f0-9]{32}");

    public ClaveWeatherbit() {
        super(PluginTiempoReal.getPlugin(PluginTiempoReal.class), RUTA_CONFIG, ID_CONFIG, PERMISO_CONFIG);
    }

    @Override
    public boolean valorValido(String otroValor) {
        // Permitir un valor nulo o en blanco si no se desea especificar una clave
        // (aunque las partes del plugin que la usen no funcionarán)
        return otroValor == null || otroValor.isEmpty() || PATRON_CLAVE.matcher(otroValor).matches();
    }
}