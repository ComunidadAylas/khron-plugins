/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2018 Comunidad Aylas
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
package org.aylas.khron.tiemporeal.configuraciones;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aylas.khron.tiemporeal.astronomia.ArcoDiurnoSolar;
import org.aylas.khron.tiemporeal.astronomia.FactoriaArcoDiurnoSolar;
import org.aylas.khron.tiemporeal.meteorologia.Clima;
import org.aylas.khron.tiemporeal.meteorologia.FactoriaClima;

/**
 * Alberga los datos que definen cómo se deben simular características de tiempo
 * de un mundo.
 *
 * @author AlexTMjugador
 */
public final class ParametrosSimulacionMundo {
    private static final Pattern PATRON_COORDENADAS = Pattern.compile(
        "([+-]?[0-9]+(?:\\.[0-9]+)?)\\s+([+-]?[0-9]+(?:\\.[0-9]+)?)|([0-9]+(?:\\.[0-9]+)?)\\s*[\\u00b0\\u00ba]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*'\\s*([0-9]+(?:\\.[0-9]+)?)\\s*''\\s*([NS])\\s+([0-9]+(?:\\.[0-9]+)?)\\s*[\\u00b0\\u00ba]\\s*([0-9]+(?:\\.[0-9]+)?)\\s*'\\s*([0-9]+(?:\\.[0-9]+)?)\\s*''\\s*([EO])"
    );

    private final ZoneId franjaHoraria;
    private final ArcoDiurnoSolar arcoDiurnoSolar;
    private final Clima clima;
    private final double latitudSpawn;
    private final double longitudSpawn;
    private final float radio;

    /**
     * Crea un nuevo conjunto de datos que definen cómo se deben simular
     * características de tiempo de un mundo a partir de su representación
     * textual.
     *
     * @param parametros La representación textual de los parámetros.
     * @return La instancia buscada.
     * @throws IllegalArgumentException Si la cadena es nula o inválida.
     */
    public static ParametrosSimulacionMundo desdeString(String parametros) {
        ParametrosSimulacionMundo toret = null;

        try {
            String[] campos = parametros.split(", ", 5);

            if (campos.length == 5) {
                ZoneId zonaHoraria = ZoneId.of(campos[0].trim());
                ArcoDiurnoSolar arcoDiurnoSolar = FactoriaArcoDiurnoSolar.crearPorNombre(campos[1].trim());
                Clima clima = FactoriaClima.crearPorNombre(campos[2].trim());
                double[] coordenadasGeograficas = parsearCoordenadas(campos[3].trim());
                float radio = Float.valueOf(campos[4]);

                if (radio > 0) {
                    toret = new ParametrosSimulacionMundo(
                        zonaHoraria, arcoDiurnoSolar, clima, coordenadasGeograficas[0], coordenadasGeograficas[1], radio
                    );
                } else {
                    throw new NumberFormatException("El radio no puede ser 0 o negativo");
                }
            } else {
                throw new IllegalArgumentException("Número inesperado de parámetros");
            }
        } catch (NoSuchElementException | DateTimeException | NumberFormatException | NullPointerException exc) {
            throw new IllegalArgumentException(exc);
        }

        return toret;
    }

    /**
     * Crea un nuevo conjunto de datos que definen cómo se deben simular
     * características de tiempo de un mundo a partir de su representación
     * textual.
     *
     * @param franjaHoraria   La franja horaria del mundo.
     * @param arcoDiurnoSolar El arco diurno solar para este mundo.
     * @param clima           El clima a usar para este mundo.
     * @param latitudSpawn    La latitud del punto de aparición.
     * @param longitudSpawn   La longitud del punto de aparición.
     * @param radio           El radio del mundo, en kilómetros.
     * @throws IllegalArgumentException Si algún parámetro es nulo.
     */
    private ParametrosSimulacionMundo(
        ZoneId franjaHoraria, ArcoDiurnoSolar arcoDiurnoSolar, Clima clima,
        double latitudSpawn, double longitudSpawn, float radio
    ) {
        if (franjaHoraria == null || arcoDiurnoSolar == null || clima == null) {
            throw new IllegalArgumentException(
                "No se pueden crear datos de simulación de un ciclo diurno con parámetros nulos"
            );
        }

        this.franjaHoraria = franjaHoraria;
        this.arcoDiurnoSolar = arcoDiurnoSolar;
        this.clima = clima;
        this.latitudSpawn = latitudSpawn;
        this.longitudSpawn = longitudSpawn;
        this.radio = radio;
    }

    /**
     * Obtiene la franja horaria del mundo.
     *
     * @return La franja horaria del mundo.
     */
    public ZoneId getFranjaHoraria() {
        return franjaHoraria;
    }

    /**
     * Obtiene el arco diurno solar del mundo.
     *
     * @return El arco diurno solar del mundo, no nulo.
     */
    public ArcoDiurnoSolar getArcoDiurnoSolar() {
        return arcoDiurnoSolar;
    }

    /**
     * Obtiene el clima del mundo.
     *
     * @return El clima del mundo, no nulo.
     */
    public Clima getClima() {
        return clima;
    }

    /**
     * Obtiene la latitud del punto de aparición.
     *
     * @return La latitud del punto de aparición.
     */
    public double getLatitudSpawn() {
        return latitudSpawn;
    }

    /**
     * Obtiene la longitud del punto de aparición.
     *
     * @return La longitud del punto de aparición.
     */
    public double getLongitudSpawn() {
        return longitudSpawn;
    }

    /**
     * Obtiene el radio del mundo, en kilómetros.
     *
     * @return El radio del mundo, en kilómetros.
     */
    public float getRadio() {
        return radio;
    }

    /**
     * Interpreta el texto especificado como coordenadas geográficas,
     * devolviendo el resultado en radianes.
     *
     * @param coordenadas El texto a interpretar como coordenadas geográficas.
     * @return Un array con las coordenadas geográficas interpretadas, donde la
     *         primera es la latitud y la segunda la longitud.
     * @throws IllegalArgumentException Si el parámetro es nulo o inválido.
     */
    private static double[] parsearCoordenadas(String coordenadas) {
        Matcher encajadorPatronCoordenadas = PATRON_COORDENADAS.matcher(coordenadas);
        double[] toret = new double[2];

        if (coordenadas == null || !encajadorPatronCoordenadas.matches()) {
            throw new IllegalArgumentException("Las coordenadas especificadas no son válidas");
        }

        try {
            if (encajadorPatronCoordenadas.group(1) != null) {
                // Coordenadas geométricas en el formato lógico y racional: radianes
                toret[0] = Double.valueOf(encajadorPatronCoordenadas.group(1)) % (Math.PI / 2);
                toret[1] = Double.valueOf(encajadorPatronCoordenadas.group(2)) % Math.PI;
            } else {
                // Coordenadas geométricas en el formato histórico que le gusta a la gente,
                // que realmente tampoco lo encuentro mejor y es más complicado de interpretar
                double gradosLatitud = Double.valueOf(encajadorPatronCoordenadas.group(3));
                double minutosLatitud = Double.valueOf(encajadorPatronCoordenadas.group(4));
                double segundosLatitud = Double.valueOf(encajadorPatronCoordenadas.group(5));
                char direccionLatitud = encajadorPatronCoordenadas.group(6).charAt(0);
                toret[0] = (0.017453292519943 * gradosLatitud +
                    0.000290888208665 * minutosLatitud +
                    0.000004848136811 * segundosLatitud) % (Math.PI / 2);
                toret[0] *= direccionLatitud == 'N' ? 1 : -1;

                double gradosLongitud = Double.valueOf(encajadorPatronCoordenadas.group(7));
                double minutosLongitud = Double.valueOf(encajadorPatronCoordenadas.group(8));
                double segundosLongitud = Double.valueOf(encajadorPatronCoordenadas.group(9));
                char direccionLongitud = encajadorPatronCoordenadas.group(10).charAt(0);
                toret[1] = (0.017453292519943 * gradosLongitud +
                    0.000290888208665 * minutosLongitud +
                    0.000004848136811 * segundosLongitud) % Math.PI;
                toret[1] *= direccionLongitud == 'E' ? 1 : -1;
            }
        } catch (NumberFormatException exc) {
            // Números muy grandes, etc.
            throw new IllegalArgumentException(exc);
        }

        return toret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(franjaHoraria.getId());

        sb.append(", ");
        sb.append(arcoDiurnoSolar.getClass().getSimpleName());
        sb.append(", ");
        sb.append(clima.getClass().getSimpleName());
        sb.append(", ");
        sb.append(Double.toString(latitudSpawn));
        sb.append(" ");
        sb.append(Double.toString(longitudSpawn));
        sb.append(", ");
        sb.append(Float.toString(radio));

        return sb.toString();
    }
}