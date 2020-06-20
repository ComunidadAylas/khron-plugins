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
package io.github.alextmjugador.khron.tiemporeal.astronomia;

/**
 * Representa una posición de un objeto astronómico en coordenadas horizontales.
 *
 * @author AlexTMjugador
 */
public final class PosicionObjetoAstronomico {
    /**
     * La altura de la posición del sol en coordenadas horizontales, en radianes,
     * respecto al horizonte, en el rango [-π, π]. Los objetos visibles están en
     * [0, π/2].
     */
    private final double altura;

    /**
     * El acimut (ángulo respecto al sur) de la posición del sol en coordenadas
     * horizontales, en radianes, donde 0 se corresponde con el norte, π/2 con el
     * este y -π/2 con el oeste.
     */
    private final double acimut;

    PosicionObjetoAstronomico(double altura, double acimut) {
        this.altura = altura;
        this.acimut = acimut;
    }

    /**
     * Obtiene la altura de la posición del sol en coordenadas horizontales, en
     * radianes, respecto al horizonte, en el rango [-π/2, π/2].
     *
     * @return La altura descrita.
     */
    public double getAltura() {
        return altura;
    }

    /**
     * Obtiene El acimut (ángulo respecto al sur) de la posición del sol en
     * coordenadas horizontales, en radianes, donde 0 se corresponde con el norte,
     * π/2 con el este y -π/2 con el oeste.
     *
     * @return El acimut descrito.
     */
    public double getAcimut() {
        return acimut;
    }
}