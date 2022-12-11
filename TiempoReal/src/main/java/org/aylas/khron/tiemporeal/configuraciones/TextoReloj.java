/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2019 Comunidad Aylas
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

import org.aylas.khron.tiemporeal.PluginTiempoReal;
import org.bukkit.ChatColor;

import org.aylas.khron.libconfig.NotificableCambioConfiguracion;
import org.aylas.khron.libconfig.ParametroConfiguracionNotificado;

/**
 * Modela un parámetro de configuración que contiene el texto a mostrar cuando
 * un jugador empuñe un reloj, para que vea su pantalla.
 *
 * @author AlexTMjugador
 */
public class TextoReloj extends ParametroConfiguracionNotificado<String, String> {
    /**
     * La palabra clave que debe de contener el valor del parámetro de configuración
     * para ser válido. Se sustituirá por el display del reloj.
     */
    public static final String DISPLAY = "{DISPLAY}";

    /**
     * La ruta en el fichero de configuración hacia este parámetro de configuración.
     */
    private static final String RUTA_CONFIG = "Texto para relojes";

    /**
     * El identificador de este parámetro de configuración en el código y en el
     * comando asociado para cambiarlo.
     */
    private static final String ID_CONFIG = "textoReloj";

    /**
     * El permiso necesario para ejecutar un comando que cambie el valor de este
     * parámetro de configuración.
     */
    private static final String PERMISO_CONFIG = "tiemporeal.trconfig.textoReloj";

    /**
     * El número máximo de caracteres que el valor puede tomar.
     */
    private static final int CARACTERES_MAX = 75;

    /**
     * La cadena de texto representativa del valor de configuración, sin los cambios
     * aplicados por {@link procesarValor}.
     */
    private String valorSinProcesar;

    public TextoReloj() {
        this(RUTA_CONFIG, ID_CONFIG, PERMISO_CONFIG, new PluginTiempoReal.NotificableConfigTextoReloj());
    }

    protected TextoReloj(
        String rutaConfig, String idConfig, String permisoConfig, NotificableCambioConfiguracion<String> notificable
    ) {
        super(
            PluginTiempoReal.getPlugin(PluginTiempoReal.class), rutaConfig,
            idConfig, permisoConfig, notificable
        );
    }

    @Override
    protected final String procesarValor(String nuevoValor) {
        String toret = ChatColor.translateAlternateColorCodes('&', nuevoValor);
        int i = toret.indexOf(DISPLAY);

        valorSinProcesar = nuevoValor;

        // Si hay texto después del display, restaurar colores previos al display
        if (i + DISPLAY.length() < nuevoValor.length()) {
            String parteAnt = toret.substring(0, i);

            toret = toret.replace(
                DISPLAY,
                DISPLAY + ChatColor.RESET + ChatColor.getLastColors(parteAnt)
            );
        }

        return toret;
    }

    @Override
    public final String getValorYaml() {
        return valorSinProcesar;
    }

    /**
     * {@inheritDoc} Debe de contener una sola vez la cadena de texto determinada
     * por el atributo estático {@link DISPLAY}, que el plugin reemplazará por la
     * hora actual, al final. Tampoco debe de ser mayor de {@link CARACTERES_MAX}
     * caracteres.
     *
     * @return {@inheritDoc}
     */
    @Override
    public final boolean valorValido(String otroValor) {
        boolean toret = false;

        if (otroValor != null && otroValor.length() <= CARACTERES_MAX) {
            int indicePrimeraAparicion = otroValor.indexOf(DISPLAY);

            toret = indicePrimeraAparicion >= 0 &&
                indicePrimeraAparicion == otroValor.lastIndexOf(DISPLAY) &&
                indicePrimeraAparicion == otroValor.length() - DISPLAY.length();
        }

        return toret;
    }
}