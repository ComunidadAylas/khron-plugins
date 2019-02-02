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

import static org.bukkit.Bukkit.getServer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.World.Environment;

import io.github.alextmjugador.khron.libconfig.ParametroConfiguracionNotificado;

/**
 * Modela un parámetro de configuración que contiene el conjunto de mundos en el
 * que este plugin sincronizará la hora.
 * 
 * @author AlexTMjugador
 */
final class MundosSincronizacion extends ParametroConfiguracionNotificado<Set<World>, List<String>> {
    /**
     * La ruta en el fichero de configuración hacia este parámetro de configuración.
     */
    private static final String RUTA_CONFIG = "Mundos en los que sincronizar la hora";
    /**
     * El identificador de este parámetro de configuración en el código y en el
     * comando asociado para cambiarlo.
     */
    private static final String ID_CONFIG = "mundosSinc";
    /**
     * El permiso necesario para ejecutar un comando que cambie el valor de este
     * parámetro de configuración.
     */
    private static final String PERMISO_CONFIG = "tiemporeal.trconfig.mundosSinc";

    public MundosSincronizacion() throws IllegalArgumentException {
        super(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class), RUTA_CONFIG, ID_CONFIG, PERMISO_CONFIG,
                AgenteSincHora.get());
    }

    @Override
    public void leer() {
        List<String> valorYaml = getPlugin().getConfig().getStringList(getRutaConfiguracion());
        Set<World> valorLeido = new HashSet<>(valorYaml.size());

        for (String nombreMundo : valorYaml) {
            World w = getServer().getWorld(nombreMundo);
            if (esMundoValido(w)) {
                valorLeido.add(w);
            }
        }

        // Como ignoramos los mundos inválidos, solo nos quedamos con un valor válido
        setValor(valorLeido, false);
    }

    @Override
    protected List<String> getValorYaml() {
        List<String> toret = new LinkedList<>();
        Set<World> valor = getValor();

        if (valor != null) {
            for (World w : valor) {
                toret.add(w.getName());
            }
        }

        return toret;
    }

    @Override
    public boolean setValor(String nuevoValor) {
        boolean toret = true;
        Set<World> nuevoConjunto;

        if (nuevoValor != null) {
            nuevoConjunto = new HashSet<>();

            for (String nombreMundo : nuevoValor.split(",")) {
                World w = getServer().getWorld(nombreMundo);
                if (esMundoValido(w)) {
                    nuevoConjunto.add(w);
                }
            }

            toret = setValor(nuevoConjunto);
        }

        return toret;
    }

    /**
     * {@inheritDoc} Los mundos del conjunto deben de ser no nulos y de entorno
     * normal.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean valorValido(Set<World> otroValor) {
        boolean toret = otroValor != null;

        if (toret) {
            Iterator<World> iter = otroValor.iterator();
            while (iter.hasNext() && toret) {
                toret = esMundoValido(iter.next());
            }
        }

        return toret;
    }

    /**
     * Comprueba si un determinado mundo es válido (no es nulo y es de entorno
     * normal).
     *
     * @param w El mundo a comprobar.
     * @return Verdadero si es válido, falso en caso contrario.
     */
    private static boolean esMundoValido(World w) {
        return w != null && w.getEnvironment().equals(Environment.NORMAL);
    }
}