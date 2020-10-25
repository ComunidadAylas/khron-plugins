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
 * Representa la información meteorológica de un mundo, complementaria a su tiempo atmosférico.
 *
 * @author AlexTMjugador
 */
public final class InformacionMeteorologica {
    private final float temperatura;

    InformacionMeteorologica(float temperatura) {
        this.temperatura = temperatura;
    }

    /**
     * Obtiene la temperatura de un mundo, en grados Celsius.
     *
     * @return La temperatura del mundo.
     */
    public float getTemperatura() {
        return temperatura;
    }
}