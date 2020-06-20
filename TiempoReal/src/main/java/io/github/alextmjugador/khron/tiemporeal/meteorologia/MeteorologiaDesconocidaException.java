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
package io.github.alextmjugador.khron.tiemporeal.meteorologia;

/**
 * Representa un estado meteorológico desconocido, fruto de la imposibilidad de
 * calcularlo.
 *
 * @author AlexTMjugador
 */
public final class MeteorologiaDesconocidaException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Crea una excepción de un estado meteorológico desconocido con un mensaje.
     *
     * @param message El mensaje de la excepción.
     */
    MeteorologiaDesconocidaException(String message) {
        super(message);
    }

    /**
     * Crea una excepción de un estado meteorológico desconocido con un mensaje
     * y causa.
     *
     * @param message El mensaje de la excepción.
     * @param cause   El lanzable que ha causado esta excepción.
     */
    MeteorologiaDesconocidaException(String message, Throwable cause) {
        super(message, cause);
    }
}