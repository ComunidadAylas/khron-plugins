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
package org.aylas.khron.libconfig;

import static org.bukkit.Bukkit.getPluginManager;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Modela un plugin configurable, que al iniciarse se asegura de que su
 * configuración predeterminada se guarda en disco, y puede inicializar sus
 * valores de configuración a partir de los que se guarden en disco.
 *
 * @author AlexTMjugador
 */
public class PluginConfigurable extends JavaPlugin {
    @Override
    public void onEnable() {
        // Necesario para la primera ejecución de un plugin
        this.saveDefaultConfig();
    }

    /**
     * Inicializa los valores de los parámetros de configuración del plugin que
     * se pasan como parámetro desde memoria secundaria. Si no es posible
     * inicializar algún parámetro porque su valor asociado es inválido, este
     * método detiene el plugin y muestra un error en la consola del servidor.
     *
     * @param params Los parámetros cuyos valores inicializar.
     * @return Verdadero si los parámetros de configuración se han leído
     *         correctamente, falso en otro caso.
     */
    protected final boolean leerParametrosConfiguracion(ParametroConfiguracion<?, ?>... params) {
        boolean toret = true;

        try {
            if (params != null) {
                for (ParametroConfiguracion<?, ?> p : params) {
                    p.leer();
                }
            }
        } catch (IllegalArgumentException | ClassCastException exc) {
            getSLF4JLogger().error(
                "La configuración del plugin es inválida. Se detiene su ejecución",
                exc
            );

            getPluginManager().disablePlugin(this);

            toret = false;
        }

        return toret;
    }
}