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

import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Representa un tiempo atmosférico en un mundo de Minecraft.
 *
 * @author AlexTMjugador
 */
public enum TiempoAtmosferico {
    DESPEJADO {
        @Override
        public void aplicarAMundo(World w) {
            super.aplicarAMundo(w);

            w.setStorm(false);
            w.setThundering(false);
            w.setWeatherDuration(Integer.MAX_VALUE);
        }

        @Override
        public void aplicarAJugador(Player p) {
            super.aplicarAJugador(p);

            p.setPlayerWeather(WeatherType.CLEAR);
        }
    },
    PRECIPITACIONES {
        @Override
        public void aplicarAMundo(World w) {
            super.aplicarAMundo(w);

            w.setStorm(true);
            w.setThundering(false);
            w.setWeatherDuration(Integer.MAX_VALUE);
        }

        @Override
        public void aplicarAJugador(Player p) {
            super.aplicarAJugador(p);

            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
    },
    TORMENTA {
        @Override
        public void aplicarAMundo(World w) {
            super.aplicarAMundo(w);

            w.setStorm(true);
            w.setThundering(true);
            w.setWeatherDuration(Integer.MAX_VALUE);
            w.setThunderDuration(Integer.MAX_VALUE);
        }

        @Override
        public void aplicarAJugador(Player p) {
            super.aplicarAJugador(p);

            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
    };

    /**
     * Aplica este tiempo atmosférico a un mundo, de forma que éste sea el que
     * se tenga en cuenta para las mecánicas del juego.
     *
     * @param w El mundo en el que se desea aplicar el tiempo atmosférico.
     * @throws IllegalArgumentException Si el mundo es nulo.
     */
    public void aplicarAMundo(World w) {
        if (w == null) {
            throw new IllegalArgumentException("El mundo recibido es nulo");
        }
    }

    /**
     * Aplica este tiempo atmosférico a un jugador, visible solamente en su juego.
     *
     * @param p El jugador que se desea que vea el tiempo atmosférico.
     * @throws IllegalArgumentException Si el mundo es nulo.
     */
    public void aplicarAJugador(Player p) {
        if (p == null) {
            throw new IllegalArgumentException("El jugador recibido es nulo");
        }
    }

    /**
     * Deshace los efectos de la aplicación de tiempos atmosféricos en un mundo.
     *
     * @param w El mundo en el que deshacer los efectos.
     */
    public static void restaurarMundo(World w) {
        if (w == null) {
            throw new IllegalArgumentException("El mundo recibido es nulo");
        }

        // Hacer que Minecraft actualice el tiempo del mundo
        w.setWeatherDuration(0);
        w.setThunderDuration(0);
    }

    /**
     * Deshace los efectos de tiempos atmosféricos específicos para un jugador,
     * de forma que vuelva a ver el mismo tiempo atmosférico que el servidor.
     *
     * @param p El jugador para el que restaurar los tiempos atmosféricos
     *          específicos.
     */
    public static void restaurarJugador(Player p) {
        if (p == null) {
            throw new IllegalArgumentException("El jugador recibido es nulo");
        }

        p.resetPlayerWeather();
    }
}