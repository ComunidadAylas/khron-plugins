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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.bukkit.World;
import org.bukkit.World.Environment;

import io.github.alextmjugador.khron.libconfig.ParametroConfiguracionNotificado;
import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;
import io.github.alextmjugador.khron.tiemporeal.SimuladorTiempo;

import static org.bukkit.Bukkit.getWorld;

/**
 * Un parámetro de configuración que contiene los mundos para los que se han
 * configurado parámetros concretos de simulación relevantes para este plugin.
 *
 * @author AlexTMjugador
 */
public final class MapaParametrosSimulacionMundo
    extends ParametroConfiguracionNotificado<Map<String, ParametrosSimulacionMundo>, List<String>>
{
    /**
     * La ruta en el fichero de configuración hacia este parámetro de configuración.
     */
    private static final String RUTA_CONFIG = "Parámetros de simulación de mundos";

    /**
     * El identificador de este parámetro de configuración en el código y en el
     * comando asociado para cambiarlo.
     */
    private static final String ID_CONFIG = "mundosSimulacionYParametros";

    /**
     * El permiso necesario para ejecutar un comando que cambie el valor de este
     * parámetro de configuración.
     */
    private static final String PERMISO_CONFIG = "tiemporeal.trconfig.mundosSimulacionYParametros";

    public MapaParametrosSimulacionMundo() {
        super(
            PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class),
            RUTA_CONFIG,
            ID_CONFIG,
            PERMISO_CONFIG,
            SimuladorTiempo.get()
        );
    }

    @Override
    public void leer() {
        List<String> mundosYParametros = getPlugin().getConfig().getStringList(RUTA_CONFIG);
        Map<String, ParametrosSimulacionMundo> valorLeido = new HashMap<>(
            (int) (mundosYParametros.size() / 0.75)
        );

        for (String mundoYParametros : mundosYParametros) {
            Entry<String, ParametrosSimulacionMundo> entrada = parsearCadenaAEntrada(mundoYParametros);

            if (entrada != null) {
                valorLeido.put(entrada.getKey(), entrada.getValue());
            } else {
                throw new IllegalArgumentException("Una entrada de parámetros de simulación de un mundo no es válida");
            }
        }

        setValor(valorLeido, false);
    }

    @Override
    protected List<String> getValorYaml() {
        List<String> toret;
        Map<String, ParametrosSimulacionMundo> valor = getValor();

        if (valor != null) {
            toret = new ArrayList<>(valor.size());

            for (Entry<String, ParametrosSimulacionMundo> entrada : valor.entrySet()) {
                toret.add(entrada.getKey() + ", " + entrada.getValue());
            }
        } else {
            toret = new ArrayList<>(0);
        }

        return toret;
    }

    @Override
    public boolean parsearValor(String nuevoValor) {
        boolean toret = false;

        if (nuevoValor != null) {
            String[] mundosYParametros = nuevoValor.split(" \\| ");
            Map<String, ParametrosSimulacionMundo> valorLeido = new HashMap<>(
                (int) (mundosYParametros.length / 0.75)
            );

            toret = true;
            for (int i = 0; i < mundosYParametros.length && toret; ++i) {
                Entry<String, ParametrosSimulacionMundo> entrada =
                    parsearCadenaAEntrada(mundosYParametros[i]);

                if (entrada != null) {
                    valorLeido.put(entrada.getKey(), entrada.getValue());
                } else {
                    toret = false;
                }
            }

            if (toret) {
                setValor(valorLeido, false);
            }
        }

        return toret;
    }

    /**
     * Interpreta una cadena de texto que contiene un nombre de mundo y sus
     * parámetros de simulación de arco diurno a la entrada del mapa en el que
     * se guardará.
     *
     * @param valor El valor de la cadena de texto.
     * @return La entrada correspondiente a la cadena de texto, o nulo si no es
     *         válida.
     */
    private Entry<String, ParametrosSimulacionMundo> parsearCadenaAEntrada(String valor) {
        Entry<String, ParametrosSimulacionMundo> toret = null;

        try {
            String[] campos = valor.split(", ", 2);

            if (campos.length == 2) {
                World w = Objects.requireNonNull(getWorld(campos[0].trim()));
                ParametrosSimulacionMundo parametrosSimulacion =
                    ParametrosSimulacionMundo.desdeString(campos[1].trim());

                if (Environment.NORMAL.equals(w.getEnvironment())) {
                    toret = new AbstractMap.SimpleImmutableEntry<>(w.getName(), parametrosSimulacion);
                }
            }
        } catch (IllegalArgumentException | NullPointerException ignorar) {}

        return toret;
    }
}
