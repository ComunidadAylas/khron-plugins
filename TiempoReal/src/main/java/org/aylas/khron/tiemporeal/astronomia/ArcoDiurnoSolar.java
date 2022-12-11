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
package org.aylas.khron.tiemporeal.astronomia;

import java.time.Instant;

import org.bukkit.World;

/**
 * Modela las operaciones relevantes para este plugin acerca del arco diurno
 * descrito por un sol en un planeta.
 *
 * @author AlexTMjugador
 */
public interface ArcoDiurnoSolar {
    /**
     * Obtiene el número de ticks desde el comienzo del primer día a enviar a
     * los jugadores de un determinado mundo, en el formato aceptado por
     * {@link World#setFullTime(long)}, a partir del instante de tiempo, latitud
     * y longitud relevantes para un arco diurno.
     * <p>
     * Este valor es puramente cosmético. Afecta a las fases lunares, dificultad
     * regional y tiempo del día que computa el cliente, pero no a la lógica del
     * lado del servidor que dependa del tiempo del día, actualizado antes de
     * invocarse este método. El valor de luz de cada bloque no depende de la
     * hora del día ni en el cliente ni en el servidor, siendo el efecto de
     * oscurecimiento que ocurre durante las noches visual, también computado en
     * el cliente.
     * </p>
     * <p>
     * Se permite cualquier valor en el intervalo [0, 2^63 - 1).
     * </p>
     *
     * @param instante El instante de tiempo terrestre para el que se desea
     *                 calcular este tiempo.
     * @param mundo    El mundo en el que se considerará que está el jugador,
     *                 del que se puede obtener el número de ticks desde el
     *                 primer amanecer relevante.
     * @param latitud  La latitud del punto de un planeta para el que se desea
     *                 calcular este tiempo.
     * @param longitud La longitud del punto de un plaeta para el que se desea
     *                 calcular este tiempo.
     * @return El devandicho tiempo.
     * @throws IllegalArgumentException Si algún parámetro es inválido.
     */
    public long getTiempoJugador(Instant instante, World mundo, double latitud, double longitud);

    /**
     * Obtiene el número de ticks desde el comienzo del día actual a establecer
     * en un mundo, vía {@link World#setTime(long)}, a partir del instante de
     * tiempo, latitud y longitud relevantes para un arco diurno.
     * <p>
     * Este valor afecta a las mecánicas del juego, como la aparición de
     * criaturas y la dificultad regional real.
     * </p>
     * <p>
     * Los valores de retorno más esperables están en el intervalo [0, 24000), y
     * se interpretan de acuerdo al método de Bukkit mencionado anteriormente.
     * En particular, la aplicación de un valor de retorno menor que el anterior
     * provocará que el tiempo del mundo avance un día. Por ejemplo, si se ha
     * aplicado el valor 23999 y luego se pretende aplicar el 0, el tiempo del
     * mundo usado para calcular dificultad regional y demás mecánicas de juego
     * será 24000.
     * </p>
     *
     * @param instante El instante de tiempo terrestre para el que se desea
     *                 calcular este tiempo.
     * @param latitud  La latitud del punto de un planeta para el que se desea
     *                 calcular este tiempo.
     * @param longitud La longitud del punto de un planeta para el que se desea
     *                 calcular este tiempo.
     * @return El devandicho tiempo.
     * @throws IllegalArgumentException Si algún parámetro es inválido.
     */
    public long getTiempoMundo(Instant instante, double latitud, double longitud);

    /**
     * Comprueba si este simulador de arco diurno simula un planeta. En tal
     * caso, el simulador tendrá en cuenta las coordenadas geográficas pasadas a
     * otros métodos, y dará diferentes resultados dependiendo de cuáles sean.
     * En caso contrario, el valor de las coordenadas geográficas es ignorado e
     * irrelevante, no se simula la posición de ningún astro real y, por tanto,
     * no tiene sentido obtener información de tiempo del objeto.
     *
     * @return Verdadero si se simula un planeta, falso en caso contrario.
     */
    public default boolean simulaPlaneta() {
        return true;
    }
}