/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2018 Comunidad Aylas
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

import java.util.Set;

import org.bukkit.World;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.world.WorldUnloadEvent;

import io.github.alextmjugador.khron.libconfig.ComandosConfiguracion;
import io.github.alextmjugador.khron.libconfig.PluginConfigurable;

/**
 * Implementa un plugin que sincroniza el tiempo real del servidor con el tiempo
 * de todos los mundos del juego.
 *
 * @author AlexTMjugador
 */
public final class PluginTiempoReal extends PluginConfigurable {
    /**
     * Almacena si el plugin ha sido inicializado con éxito o no.
     */
    private static boolean inicializado = false;

    /**
     * El comando para establecer parámetros de configuración de este plugin. Debe
     * de estar registrado como tal en el fichero "plugin.yml".
     */
    private static final String COMANDO_ESTABLECER_CONFIG = "trconfig";
    /**
     * El comando para recargar parámetros de configuración de este plugin. Debe de
     * estar registrado como tal en el fichero "plugin.yml".
     */
    private static final String COMANDO_RECARGAR_CONFIG = "trrecargarconfig";

    /**
     * El parámetro de configuración que representa los mundos en los que
     * sincronizar la hora.
     */
    private static MundosSincronizacion cfgMundosSincronizacion;
    /**
     * El parámetro de configuración que representa el texto a mostrar cuando un
     * jugador empuña un reloj.
     */
    private static TextoHora cfgTextoHora;

    /**
     * Crea los objetos y eventos necesarios para sincronizar el tiempo y extender
     * la funcionalidad de relojes, además de inicializar los valores de
     * configuración del plugin.
     */
    @Override
    public void onEnable() {
        // Realizar tareas de inicialización de un plugin configurable
        super.onEnable();

        // Crear los objetos que representan parámetros de configuración e
        // inicializarlos, si hace falta
        if (cfgMundosSincronizacion == null) {
            cfgMundosSincronizacion = new MundosSincronizacion();
        }
        if (cfgTextoHora == null) {
            cfgTextoHora = new TextoHora();
        }
        leerParametrosConfiguracion(cfgMundosSincronizacion, cfgTextoHora);

        // Registrar comandos del plugin
        TabExecutor ejecutorComandos = new ComandosConfiguracion(COMANDO_ESTABLECER_CONFIG, COMANDO_RECARGAR_CONFIG,
                cfgMundosSincronizacion, cfgTextoHora);
        getCommand(COMANDO_ESTABLECER_CONFIG).setExecutor(ejecutorComandos);
        getCommand(COMANDO_ESTABLECER_CONFIG).setTabCompleter(ejecutorComandos);
        getCommand(COMANDO_RECARGAR_CONFIG).setExecutor(ejecutorComandos);
        getCommand(COMANDO_RECARGAR_CONFIG).setTabCompleter(ejecutorComandos);

        // Comenzar acciones del plugin
        RelojExtendido.get();

        inicializado = true;
    }

    /**
     * Detiene apropiadamente otras partes del plugin.
     */
    @Override
    public void onDisable() {
        if (inicializado) {
            AgenteSincHora ash = AgenteSincHora.get();

            for (World w : getCfgMundosSincronizacion()) {
                ash.onWorldUnload(new WorldUnloadEvent(w));
            }
        }
    }

    /**
     * Obtiene el valor actual del parámetro de configuración que indica el texto a
     * mostrar cuando un jugador empuña un reloj.
     * 
     * @return El devandicho valor del parámetro de configuración. Puede ser nulo si
     *         todavía no se ha inicializado la configuración del plugin.
     */
    public String getCfgTextoHora() {
        return cfgTextoHora != null ? cfgTextoHora.getValor() : null;
    }

    /**
     * Obtiene el valor actual del parámetro de configuración que indica los mundos
     * en los que sincronizar la hora con la del servidor.
     * 
     * @return El devandicho valor del parámetro de configuración. Puede ser nulo si
     *         todavía no se ha inicializado la configuración del plugin.
     */
    public Set<World> getCfgMundosSincronizacion() {
        return cfgMundosSincronizacion != null ? cfgMundosSincronizacion.getValor() : null;
    }
}