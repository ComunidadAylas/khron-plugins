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

import java.time.Instant;

import org.bukkit.World;

/**
 * Representa un arco diurno solar de Minecraft, que no pretende cambiar la
 * mecánica natural de ciclo día-noche de Minecraft.
 *
 * @author AlexTMjugador
 */
public final class ArcoDiurnoSolarMinecraft implements ArcoDiurnoSolar {
    @Override
    public long getTiempoJugador(Instant instante, World mundo, double latitud, double longitud) {
        return Long.MIN_VALUE;
    }

    @Override
    public long getTiempoMundo(Instant instante, double latitud, double longitud) {
        return Long.MIN_VALUE;
    }

    @Override
    public boolean simulaPlaneta() {
        return false;
    }
}