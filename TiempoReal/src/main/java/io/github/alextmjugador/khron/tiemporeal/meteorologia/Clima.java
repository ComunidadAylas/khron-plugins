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

import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Describe las operaciones relevantes para este plugin acerca de un clima, que
 * define el tiempo atmosférico de un lugar de un planeta.
 * <p>
 * Las implementaciones de esta interfaz deben de implementar los métodos
 * {@code equals(Object)} y {@code #hashCode()} de forma que consideren como
 * igual cualquier otra instancia de esa misma implementación.
 * </p>
 *
 * @author AlexTMjugador
 */
public interface Clima {
    /**
     * Obtiene el tiempo atmosférico para una determinada latitud y longitud de
     * un planeta en el instante actual.
     *
     * @param latitud  La latitud de la que se quiere calcular qué tiempo
     *                 atmosférico hace.
     * @param longitud La longitud de la que se quiere calcular qué tiempo
     *                 atmosférico hace.
     * @return El tiempo atmosférico correspondiente a las coordenadas
     *         especificadas.
     * @throws MeteorologiaDesconocidaException Si no se ha podido calcular el
     *                                          tiempo atmosférico actual para
     *                                          las coordenadas especificadas.
     */
    public Entry<TiempoAtmosferico, InformacionMeteorologica> calcularTiempoAtmosfericoActual(
        double latitud, double longitud
    ) throws MeteorologiaDesconocidaException;

    /**
     * Realiza una operación tras computar el tiempo atmosférico para una
     * determinada latitud y longitud de un planeta en el instante actual.
     *
     * @param latitud  La latitud de la que se quiere calcular qué tiempo
     *                 atmosférico hace.
     * @param longitud La longitud de la que se quiere calcular qué tiempo
     *                 atmosférico hace.
     * @param callback La función a ejecutar cuando se complete el cálculo, para
     *                 hacer lo pertinente con él, que recibe de parámetro el
     *                 tiempo atmosférico e información meteorológica
     *                 calculadas. La ejecución de este callback se realiza en
     *                 el contexto del hilo principal del servidor. La ejecución
     *                 se este callback no se garantiza en caso de que ocurran
     *                 errores.
     * @throws MeteorologiaDesconocidaException Si no se ha podido calcular el
     *                                          tiempo atmosférico actual para
     *                                          las coordenadas especificadas.
     */
    public void calcularTiempoAtmosfericoActual(
        double latitud, double longitud, BiConsumer<TiempoAtmosferico, InformacionMeteorologica> callback
    ) throws MeteorologiaDesconocidaException;

    /**
     * Obtiene las máximas invocaciones de métodos que computan resultados
     * permitidas en un día.
     *
     * @return Las máximas invocaciones de métodos que computan resultados
     *         permitidas, como un entero mayor que cero. Un valor de
     *         {@code Float.POSITIVE_INFINITY} indica que no hay un límite de
     *         invocaciones.
     */
    public float maximasInvocacionesPorDiaPermitidas();

    /**
     * Comprueba si las operaciones de cálculo ejecutadas por este objeto tienen
     * el potencial de bloquear el procesamiento de los ticks del servidor
     * durante un tiempo indefinido o excesivo. De ser ese el caso, los usuarios
     * del objeto deben de preferir usar las versiones con callback de los
     * métodos de cálculo, que ejecutarán las operaciones bloqueantes en otro
     * hilo.
     *
     * @return Verdadero si los cálculos ejecutados por este objeto pueden ser
     *         bloqueantes, falso en otro caso.
     */
    public boolean esBloqueante();

    /**
     * Obtiene si este clima pretende modificar las mecánicas de meteorología
     * predeterminadas de Minecraft. En caso negativo, las invocaciones a
     * métodos similares a
     * {@link #calcularTiempoAtmosfericoActual(double, double)} siempre lanzarán
     * una excepción.
     *
     * @return Verdadero si este clima pretende modificar las mecánicas de
     *         meteorología, falso en otro caso.
     */
    public default boolean simulaMeteorologia() {
        return true;
    }
}