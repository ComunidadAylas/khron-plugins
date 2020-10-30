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
package io.github.alextmjugador.khron.libconfig;

import org.bukkit.plugin.Plugin;

/**
 * Modela un parámetro de configuración que notifica a un método de una
 * determinada clase cuando se produce un cambio en él.
 *
 * @param <E> El tipo de dato del parámetro de configuración.
 * @param <T> El tipo de valor almacenado en el fichero de configuración YAML
 *            para este parámetro.
 * @author AlexTMjugador
 */
public abstract class ParametroConfiguracionNotificado<E, T> extends ParametroConfiguracion<E, T> {
    /**
     * Contiene una referencia al objeto con un método que se invocará cuando cambie
     * el valor del parámetro de configuración.
     */
    private final NotificableCambioConfiguracion<E> manejadorCambio;

    /**
     * {@inheritDoc}
     *
     * @param manejadorCambio El objeto que se encargará de manejar el evento de
     *                        cambio de valor del parámetro de configuración.
     */
    public ParametroConfiguracionNotificado(
        Plugin plugin, String rutaConfiguracion, String id, String permiso,
        NotificableCambioConfiguracion<E> manejadorCambio
    ) {
        super(plugin, rutaConfiguracion, id, permiso);

        if (manejadorCambio == null) {
            throw new IllegalArgumentException(
                "El objeto que maneja el evento de cambio de valor de este parámetro de configuración es nulo"
            );
        }

        this.manejadorCambio = manejadorCambio;
    }

    @Override
    protected E procesarValor(E nuevoValor) {
        manejadorCambio.onNewConfig(getValor(), nuevoValor);
        return nuevoValor;
    }
}