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
package io.github.alextmjugador.khron.tiemporeal;

import org.bukkit.ChatColor;

import io.github.alextmjugador.khron.libconfig.ParametroConfiguracion;

/**
 * Modela un parámetro de configuración que contiene el texto a mostrar cuando
 * un jugador empuñe un reloj, para que vea la hora.
 * 
 * @author AlexTMjugador
 */
final class TextoHora extends ParametroConfiguracion<String, String> {
    /**
     * La palabra clave que debe de contener el texto de la hora en el reloj para
     * ser válido. Se sustituirá por la hora del mundo en el que el jugador esté.
     */
    public static final String PALABRA_CLAVE_TEXTO_HORA = "{HORA}";
    /**
     * La cadena de texto {@link PALABRA_CLAVE_TEXTO_HORA}, pero construida de
     * manera que puede ser interpretada con seguridad como una expresión regular.
     */
    public static final String REGEX_CLAVE_TEXTO_HORA = "\\{HORA\\}";

    /**
     * La ruta en el fichero de configuración hacia este parámetro de configuración.
     */
    private static final String RUTA_CONFIG = "Texto de hora en reloj";
    /**
     * El identificador de este parámetro de configuración en el código y en el
     * comando asociado para cambiarlo.
     */
    private static final String ID_CONFIG = "textoHora";
    /**
     * El permiso necesario para ejecutar un comando que cambie el valor de este
     * parámetro de configuración.
     */
    private static final String PERMISO_CONFIG = "tiemporeal.trconfig.textoHora";

    /**
     * La cadena de texto, sin los cambios aplicados por {@link procesarValor}.
     */
    private String valorSinProcesar;
    /**
     * El número máximo de caracteres que este valor puede tomar.
     */
    private final int CARACTERES_MAX = 75;

    public TextoHora() throws IllegalArgumentException {
        super(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class), RUTA_CONFIG, ID_CONFIG, PERMISO_CONFIG);
    }

    @Override
    protected String procesarValor(String nuevoValor) {
        String toret = ChatColor.translateAlternateColorCodes('&', nuevoValor);

        valorSinProcesar = nuevoValor;
        if (toret.matches(".*" + REGEX_CLAVE_TEXTO_HORA + ".*\\S+")) {
            int i = toret.indexOf(PALABRA_CLAVE_TEXTO_HORA);
            String parteAnt = toret.substring(0, i); // i no va a valer -1, así que por lo menos tenemos la cadena
                                                     // vacía
            toret = toret.replaceFirst(REGEX_CLAVE_TEXTO_HORA,
                    PALABRA_CLAVE_TEXTO_HORA + ChatColor.RESET + ChatColor.getLastColors(parteAnt));
        }

        return toret;
    }

    @Override
    public String getValorYaml() {
        return valorSinProcesar;
    }

    /**
     * {@inheritDoc} Debe de contener una sola vez la cadena de texto determinada
     * por el atributo estático {@link PALABRA_CLAVE_TEXTO_HORA}, que el plugin
     * reemplazará por la hora actual. Tampoco debe de ser mayor de
     * {@link CARACTERES_MAX} caracteres.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean valorValido(String otroValor) {
        boolean toret = false;

        if (otroValor != null && otroValor.length() <= CARACTERES_MAX) {
            int ultimoIndice = otroValor.lastIndexOf(PALABRA_CLAVE_TEXTO_HORA);
            toret = ultimoIndice != -1 && ultimoIndice == otroValor.indexOf(PALABRA_CLAVE_TEXTO_HORA);
        }

        return toret;
    }
}