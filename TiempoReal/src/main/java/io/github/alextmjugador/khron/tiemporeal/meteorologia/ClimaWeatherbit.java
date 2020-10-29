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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.slf4j.Logger;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;

import static org.bukkit.Bukkit.getScheduler;

/**
 * Obtiene información actual del clima en la Tierra usando la API de Weatherbit.
 *
 * @author AlexTMjugador
 */
final class ClimaWeatherbit implements Clima {
    /**
     * Restringe la creación de instancias de esta clase a otras clases del paquete.
     */
    ClimaWeatherbit() {}

    @Override
    public Entry<TiempoAtmosferico, InformacionMeteorologica> calcularTiempoAtmosfericoActual(
        double latitud, double longitud
    ) throws MeteorologiaDesconocidaException {
        return calcularTiempoAtmosferico(latitud, longitud, null, false);
    }

    @Override
    public void calcularTiempoAtmosfericoActual(
        double latitud, double longitud, BiConsumer<TiempoAtmosferico, InformacionMeteorologica> callback
    ) throws MeteorologiaDesconocidaException {
        calcularTiempoAtmosferico(latitud, longitud, callback, true);
    }

    @Override
    public boolean esBloqueante() {
        return true;
    }

    @Override
    public float maximasInvocacionesPorDiaPermitidas() {
        // Asumimos el plan gratuito y accesible de Weatherbit
        return 500;
    }

    /**
     * Ejecuta el cálculo del tiempo atmosférico actual, enviando una solicitud HTTP
     * a la API de Weatherbit, de forma síncrona (ejecutando la petición en el hilo
     * actual y esperando a su resultado) o asíncrona (ejecutando un callback en el
     * hilo principal cuando el resultado esté listo).
     *
     * @param latitud   La latitud de la que se quiere calcular qué tiempo
     *                  atmosférico hace.
     * @param longitud  La longitud de la que se quiere calcular qué tiempo
     *                  atmosférico hace.
     * @param callback  La función a ejecutar cuando se complete el cálculo, para
     *                  hacer lo pertinente con él, que recibe de parámetro el
     *                  tiempo atmosférico y la información meteorológica
     *                  calculadas. La ejecución de este callback se realiza en el
     *                  contexto del hilo principal del servidor. La ejecución se
     *                  este callback no se garantiza en caso de que ocurran
     *                  errores. Este parámetro solo es relevante si el cálculo se
     *                  hace de manera asíncrona.
     * @param asincrono Si es verdadero, la petición se ejecutará de forma
     *                  asíncrona. En otro caso, se ejecutará de forma síncrona.
     * @return Si se ha ejecutado de forma síncrona, la información devuelta por la
     *         API. En caso contrario, nulo.
     * @throws MeteorologiaDesconocidaException Si ocurre alguna condición de error
     *                                          detectable en el hilo actual al
     *                                          realizar el cálculo.
     */
    private Entry<TiempoAtmosferico, InformacionMeteorologica> calcularTiempoAtmosferico(
        double latitud, double longitud, BiConsumer<TiempoAtmosferico, InformacionMeteorologica> callback, boolean asincrono
    ) throws MeteorologiaDesconocidaException {
        PluginTiempoReal plugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);
        String clave = plugin.getClaveWeatherbit();
        Entry<TiempoAtmosferico, InformacionMeteorologica> toret = null;

        if (clave != null && clave.length() == 32) {
            if (asincrono) {
                getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        Entry<TiempoAtmosferico, InformacionMeteorologica> tiempoAtmosferico = solicitarTiempoAWeatherbit(
                            clave, latitud, longitud
                        );

                        getScheduler().runTask(plugin, () -> {
                            callback.accept(tiempoAtmosferico.getKey(), tiempoAtmosferico.getValue());
                        });
                    } catch (MeteorologiaDesconocidaException exc) {
                        PluginTiempoReal.getPlugin(PluginTiempoReal.class).getSLF4JLogger().warn(
                            "Ha ocurrido un error durante la comunicación con la API de Weatherbit", exc
                        );
                    }
                });
            } else {
                toret = solicitarTiempoAWeatherbit(clave, latitud, longitud);
            }
        } else {
            throw new MeteorologiaDesconocidaException("Es necesaria una clave para usar la API de Weatherbit");
        }

        return toret;
    }

    /**
     * Envía una solicitud HTTP a la API de Weatherbit para obtener información
     * meteorológica acerca del tiempo atmosférico de un lugar de la Tierra.
     *
     * @param clave    La clave a usar para autenticarse ante Weatherbit.
     * @param latitud  La latitud de la que obtener su información
     *                 meteorológica.
     * @param longitud La longitud de la que obtener su información
     *                 meteorológica.
     * @return La información devuelta por Weatherbit.
     * @throws MeteorologiaDesconocidaException Si no se ha podido recuperar
     *                                          información meteorológica de
     *                                          Weatherbit.
     */
    private Entry<TiempoAtmosferico, InformacionMeteorologica> solicitarTiempoAWeatherbit(
        String clave, double latitud, double longitud
    ) throws MeteorologiaDesconocidaException {
        Entry<TiempoAtmosferico, InformacionMeteorologica> toret = null;
        Logger loggerPlugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class).getSLF4JLogger();

        try {
            loggerPlugin.trace(
                "Solicitando información meteorológica a Weatherbit para lat = {}, lon = {}...",
                latitud, longitud
            );

            // Establecer configuración de la conexión a la API y realizarla
            StringBuilder urlConsulta = new StringBuilder();
            urlConsulta.append("https://api.weatherbit.io/v2.0/current?key=");
            urlConsulta.append(clave);
            urlConsulta.append("&lat=");
            urlConsulta.append(Double.toString(Math.toDegrees(latitud)));
            urlConsulta.append("&lon=");
            urlConsulta.append(Double.toString(Math.toDegrees(longitud)));

            HttpURLConnection conexion = (HttpURLConnection) new URL(urlConsulta.toString()).openConnection();
            conexion.setAllowUserInteraction(false);
            conexion.setUseCaches(false);
            conexion.setRequestProperty("Accept", "application/json");
            conexion.setRequestProperty("User-Agent", "Khron Minecraft server");
            conexion.connect();

            InputStream streamConexion = conexion.getInputStream();
            if (!streamConexion.markSupported()) {
                streamConexion = new BufferedInputStream(streamConexion);
            }
            streamConexion.mark(2048); // Normalmente las respuestas ocupan ~650 bytes

            int codigoRespuesta = conexion.getResponseCode();
            if (codigoRespuesta == 200) {
                try (JsonParser parser = Json.createParser(streamConexion)) {
                    String ultimaClave = null;
                    boolean enObjetoTiempo = false;
                    TiempoAtmosferico tiempoAtmosferico = null;
                    Float temperatura = null;

                    while (parser.hasNext() && toret == null) {
                        Event evento = parser.next();

                        if (evento == Event.KEY_NAME) {
                            ultimaClave = parser.getString();
                        } else if (evento == Event.START_OBJECT && "weather".equals(ultimaClave)) {
                            enObjetoTiempo = true;
                        } else if (evento == Event.END_OBJECT) {
                            enObjetoTiempo = false;
                        } else if (enObjetoTiempo && evento == Event.VALUE_NUMBER && "code".equals(ultimaClave)) {
                            int codigoTiempo = Integer.parseInt(parser.getString());

                            loggerPlugin.trace(
                                "Código de tiempo recibido: {}",
                                codigoTiempo
                            );

                            if (codigoTiempo >= 200 && codigoTiempo < 300) {
                                // Diferentes tipos de tormenta
                                tiempoAtmosferico = TiempoAtmosferico.TORMENTA;
                            } else if (codigoTiempo >= 300 && codigoTiempo < 400) {
                                // Diferentes tipos de llovizna
                                tiempoAtmosferico = TiempoAtmosferico.PRECIPITACIONES;
                            } else if (codigoTiempo >= 500 && codigoTiempo < 600) {
                                // Diferentes tipos de lluvia
                                tiempoAtmosferico = TiempoAtmosferico.PRECIPITACIONES;
                            } else if (codigoTiempo >= 600 && codigoTiempo < 700) {
                                // Diferentes tipos de nevada
                                tiempoAtmosferico = TiempoAtmosferico.PRECIPITACIONES;
                            } else if (codigoTiempo >= 700 && codigoTiempo < 800) {
                                // Diferentes tipos de niebla
                                tiempoAtmosferico = TiempoAtmosferico.PRECIPITACIONES;
                            } else if (codigoTiempo >= 800 && codigoTiempo < 900) {
                                // Diferentes tipos de cielos despejados
                                tiempoAtmosferico = TiempoAtmosferico.DESPEJADO;
                            } else if (codigoTiempo == 900) {
                                // Precipitación desconocida
                                tiempoAtmosferico = TiempoAtmosferico.PRECIPITACIONES;
                            } else {
                                throw new MeteorologiaDesconocidaException(
                                    "La API de Weatherbit ha devuelto un código de tiempo no reconocido: " + codigoTiempo
                                );
                            }
                        } else if (evento == Event.VALUE_NUMBER && "temp".equals(ultimaClave)) {
                            temperatura = parser.getBigDecimal().floatValue();

                            loggerPlugin.trace(
                                "Temperatura recibida: {}",
                                temperatura
                            );
                        } else if (evento == Event.VALUE_STRING && "city_name".equals(ultimaClave)) {
                            loggerPlugin.trace(
                                "Ciudad correspondiente a las coordenadas: {}", parser.getString()
                            );
                        } else if (evento == Event.VALUE_STRING && "timezone".equals(ultimaClave)) {
                            loggerPlugin.trace(
                                "Franja horaria correspondiente a las coordenadas: {}", parser.getString()
                            );
                        } else if (evento == Event.VALUE_STRING && "country_code".equals(ultimaClave)) {
                            loggerPlugin.trace(
                                "Código de país correspondiente a las coordenadas: {}", parser.getString()
                            );
                        } else if (evento == Event.VALUE_STRING && "state_code".equals(ultimaClave)) {
                            loggerPlugin.trace(
                                "Código de estado correspondiente a las coordenadas: {}", parser.getString()
                            );
                        }

                        // Crear el valor a devolver si corresponde
                        if (temperatura != null && tiempoAtmosferico != null) {
                            toret = new AbstractMap.SimpleImmutableEntry<>(
                                tiempoAtmosferico, new InformacionMeteorologica(temperatura)
                            );
                        }
                    }

                    if (toret == null) {
                        String respuesta;
                        try {
                            streamConexion.reset();

                            Writer writer = new StringWriter(2048);
                            try (Reader reader = new InputStreamReader(streamConexion)) {
                                reader.transferTo(writer);
                            }

                            respuesta = writer.toString();
                        } catch (IOException exc) {
                            respuesta = "Error obteniendo la respuesta: " + exc.getMessage();
                        }

                        throw new MeteorologiaDesconocidaException(
                            "La API de Weatherbit ha dado una respuesta que no contenía toda la información buscada: " +
                            Objects.toString(tiempoAtmosferico) + ", " + Objects.toString(temperatura) + "\n" +
                            "Respuesta original:\n" + respuesta
                        );
                    }
                }
            } else {
                throw new MeteorologiaDesconocidaException(
                    "La API de Weatherbit ha devuelto un código de respuesta HTTP no esperado: " + codigoRespuesta
                );
            }
        } catch (IOException exc) {
            throw new MeteorologiaDesconocidaException(
                "La API de Weatherbit ha devuelto una respuesta no esperada", exc
            );
        }

        return toret;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClimaWeatherbit;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}