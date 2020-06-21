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

import java.util.function.Consumer;

/**
 * No provee información meteorológica, con el objetivo de no influir en las
 * mecánicas de clima de Minecraft.
 *
 * @author AlexTMjugador
 */
final class ClimaMinecraft implements Clima {
    @Override
    public TiempoAtmosferico calcularTiempoAtmosfericoActual(
        double latitud, double longitud
    ) throws MeteorologiaDesconocidaException {
        throw new MeteorologiaDesconocidaException("Este clima no modifica las mecánicas de Minecraft");
    }

    @Override
    public void calcularTiempoAtmosfericoActual(
        double latitud, double longitud, Consumer<TiempoAtmosferico> callback
    ) throws MeteorologiaDesconocidaException {
        throw new MeteorologiaDesconocidaException("Este clima no modifica las mecánicas de Minecraft");
    }

    @Override
    public float maximasInvocacionesPorDiaPermitidas() {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public boolean esBloqueante() {
        return false;
    }

    @Override
    public boolean simulaMeteorologia() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClimaMinecraft;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}