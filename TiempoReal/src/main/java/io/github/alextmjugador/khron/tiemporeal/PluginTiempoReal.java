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

import java.util.Map;

import org.bukkit.command.TabExecutor;

import io.github.alextmjugador.khron.libconfig.ComandosConfiguracion;
import io.github.alextmjugador.khron.libconfig.PluginConfigurable;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.ClaveWeatherbit;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.MapaParametrosSimulacionMundo;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.ParametrosSimulacionMundo;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.TextoRelojDigital;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.TextoRelojDigitalDimensionSinCiclo;
import io.github.alextmjugador.khron.tiemporeal.relojes.RelojDigitalBasico;

import static org.bukkit.Bukkit.getPluginManager;

/**
 * Implementa un plugin que sincroniza el tiempo real del servidor con el tiempo
 * de todos los mundos del juego.
 *
 * @author AlexTMjugador
 */
public final class PluginTiempoReal extends PluginConfigurable {
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
     * Almacena si el plugin ha sido inicializado con éxito o no.
     */
    private boolean inicializado = false;

    /**
     * Los parámetros de simulación del ciclo diurno de cada mundo.
     */
    private MapaParametrosSimulacionMundo parametrosSimulacionMundo;

    /**
     * El parámetro de configuración que representa el texto a mostrar cuando un
     * jugador empuña un reloj digital.
     */
    private TextoRelojDigital textoRelojDigital;

    /**
     * El parámetro de configuración que indica el texto a mostrar cuando un jugador
     * empuña un reloj digital, en una dimensión que no tenga un ciclo día-noche.
     */
    private TextoRelojDigitalDimensionSinCiclo textoRelojDigitalDimensionSinCiclo;

    /**
     * El parámetro de configuración que contiene la clave a usar para
     * autenticarse contra la API de Weatherbit.
     */
    private ClaveWeatherbit claveWeatherbit;

    /**
     * Crea los objetos y eventos necesarios para sincronizar el tiempo y extender
     * la funcionalidad de relojes, además de inicializar los valores de
     * configuración del plugin.
     */
    @Override
    public void onEnable() {
        // Realizar tareas de inicialización de un plugin configurable
        super.onEnable();

        // Crear los objetos que representan parámetros de configuración e inicializarlos
        this.parametrosSimulacionMundo = new MapaParametrosSimulacionMundo();
        this.textoRelojDigital = new TextoRelojDigital();
        this.textoRelojDigitalDimensionSinCiclo = new TextoRelojDigitalDimensionSinCiclo();
        this.claveWeatherbit = new ClaveWeatherbit();

        boolean configuracionLeida = leerParametrosConfiguracion(
            parametrosSimulacionMundo, textoRelojDigital, textoRelojDigitalDimensionSinCiclo,
            claveWeatherbit
        );

        if (configuracionLeida) {
            // Registrar comandos del plugin
            TabExecutor ejecutorComandos = new ComandosConfiguracion(
                COMANDO_ESTABLECER_CONFIG, COMANDO_RECARGAR_CONFIG,
                parametrosSimulacionMundo, textoRelojDigital, textoRelojDigitalDimensionSinCiclo,
                claveWeatherbit
            );
            getCommand(COMANDO_ESTABLECER_CONFIG).setExecutor(ejecutorComandos);
            getCommand(COMANDO_ESTABLECER_CONFIG).setTabCompleter(ejecutorComandos);
            getCommand(COMANDO_RECARGAR_CONFIG).setExecutor(ejecutorComandos);
            getCommand(COMANDO_RECARGAR_CONFIG).setTabCompleter(ejecutorComandos);

            // Comenzar simulación de ciclos diurnos
            SimuladorTiempo.get().comenzarSimulacion();

            // Registrar eventos
            getPluginManager().registerEvents(SimuladorTiempo.get(), this);
            getPluginManager().registerEvents(RelojDigitalBasico.get(), this);

            inicializado = true;
        }
    }

    /**
     * Detiene apropiadamente otras partes del plugin.
     */
    @Override
    public void onDisable() {
        if (inicializado) {
            SimuladorTiempo.get().detenerSimulacion();
            RelojDigitalBasico.get().ocultarTodosLosDisplay();
        }
    }

    /**
     * Obtiene los parámetros de simulación del tiempo de los mundos.
     *
     * @return Los devandichos parámetros. Pueden ser nulos solo si todavía no
     *         se ha inicializado la configuración del plugin.
     */
    public Map<String, ParametrosSimulacionMundo> getParametrosSimulacionMundo() {
        return parametrosSimulacionMundo == null ? null : parametrosSimulacionMundo.getValor();
    }

    /**
     * Obtiene el valor actual del parámetro de configuración que indica el texto a
     * mostrar cuando un jugador empuña un reloj digital.
     *
     * @return El devandicho valor del parámetro de configuración. Puede ser nulo si
     *         todavía no se ha inicializado la configuración del plugin.
     */
    public String getTextoRelojDigital() {
        return textoRelojDigital == null ? null : textoRelojDigital.getValor();
    }

    /**
     * Obtiene el valor actual del parámetro de configuración que indica el texto a
     * mostrar cuando un jugador empuña un reloj digital, en una dimensión que no
     * tenga un ciclo día-noche.
     *
     * @return El devandicho valor del parámetro de configuración. Puede ser nulo si
     *         todavía no se ha inicializado la configuración del plugin.
     */
    public String getTextoRelojDigitalDimensionSinCiclo() {
        return textoRelojDigitalDimensionSinCiclo == null ? null : textoRelojDigitalDimensionSinCiclo.getValor();
    }

    /**
     * Obtiene el valor actual del parámetro de configuración que indica la
     * clave a usar para autenticarse contra la API de Weatherbit.
     *
     * @return La devandicha clave.
     */
    public String getClaveWeatherbit() {
        return claveWeatherbit == null ? null : claveWeatherbit.getValor();
    }
}