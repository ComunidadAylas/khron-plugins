/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2019 Comunidad Aylas
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.alextmjugador.khron.libconfig;

/**
 * Modela una interfaz que deben de implementar todas las clases de los objetos
 * que aspiren a manejar los eventos de cambio de configuración de un parámetro.
 * 
 * @param <T> El tipo del valor del parámetro de configuración.
 * @author AlexTMjugador
 */
public interface NotificableCambioConfiguracion<T> {
    /**
     * Este método es llamado por un parámetro de configuración notificado cuando se
     * ha realizado un cambio en el valor que tiene asociado, o la lectura de su
     * valor original desde memoria secundaria. El objeto que maneja este evento
     * debe comprometerse a no realizar nada que pudiera volver a generar un cambio
     * en el valor del parámetro de configuración (pues ello generaría un bucle sin
     * fin), y debe tener en cuenta que, cuando se llama este método, aún no se ha
     * actualizado el valor asociado al objeto que representa en memoria el
     * parámetro de configuración correspondiente.
     * 
     * @param antiguoValor El antiguo valor que tenía el parámetro de configuración.
     * @param nuevoValor   El nuevo valor que tendrá el parámetro de configuración.
     */
    public void onNewConfig(T antiguoValor, T nuevoValor);
}