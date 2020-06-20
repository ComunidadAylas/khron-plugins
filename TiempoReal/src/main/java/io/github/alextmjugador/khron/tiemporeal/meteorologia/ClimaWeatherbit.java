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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;


import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;

import static org.bukkit.Bukkit.getLogger;
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
    public TiempoAtmosferico calcularTiempoAtmosfericoActual(
        double latitud, double longitud
    ) throws MeteorologiaDesconocidaException {
        return calcularTiempoAtmosferico(latitud, longitud, null, false);
    }

    @Override
    public void calcularTiempoAtmosfericoActual(
        double latitud, double longitud, Consumer<TiempoAtmosferico> callback
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
     * Ejecuta el cálculo del tiempo atmosférico actual, enviando una solicitud
     * HTTP a la API de Weatherbit, de forma síncrona (ejecutando la petición en
     * el hilo actual y esperando a su resultado) o asíncrona (ejecutando un
     * callback en el hilo principal cuando el resultado esté listo).
     *
     * @param latitud   La latitud de la que se quiere calcular qué tiempo
     *                  atmosférico hace.
     * @param longitud  La longitud de la que se quiere calcular qué tiempo
     *                  atmosférico hace.
     * @param callback  La función a ejecutar cuando se complete el cálculo,
     *                  para hacer lo pertinente con él, que recibe de parámetro
     *                  el tiempo atmosférico calculado. La ejecución de este
     *                  callback se realiza en el contexto del hilo principal
     *                  del servidor. La ejecución se este callback no se
     *                  garantiza en caso de que ocurran errores. Este parámetro
     *                  solo es relevante si el cálculo se hace de manera
     *                  asíncrona.
     * @param asincrono Si es verdadero, la petición se ejecutará de forma
     *                  asíncrona. En otro caso, se ejecutará de forma síncrona.
     * @return Si se ha ejecutado de forma síncrona, el tiempo atmosférico
     *         devuelto por la API. En caso contrario, nulo.
     * @throws MeteorologiaDesconocidaException Si ocurre alguna condición de
     *                                          error detectable en el hilo
     *                                          actual al realizar el cálculo.
     */
    private TiempoAtmosferico calcularTiempoAtmosferico(
        double latitud, double longitud, Consumer<TiempoAtmosferico> callback, boolean asincrono
    ) throws MeteorologiaDesconocidaException {
        PluginTiempoReal plugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);
        String clave = plugin.getClaveWeatherbit();
        TiempoAtmosferico toret = null;

        if (clave != null && clave.length() == 32) {
            if (asincrono) {
                getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        TiempoAtmosferico tiempoAtmosferico = solicitarTiempoAWeatherbit(clave, latitud, longitud);

                        getScheduler().runTask(plugin, () -> {
                            callback.accept(tiempoAtmosferico);
                        });
                    } catch (MeteorologiaDesconocidaException exc) {
                        getLogger().log(
                            Level.WARNING,
                            "Ha ocurrido un error durante la comunicación con la API de Weatherbit",
                            exc
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
     * @return El tiempo atmosférico devuelto por Weatherbit.
     * @throws MeteorologiaDesconocidaException Si no se ha podido recuperar
     *                                          información meteorológica de
     *                                          Weatherbit.
     */
    private TiempoAtmosferico solicitarTiempoAWeatherbit(
        String clave, double latitud, double longitud
    ) throws MeteorologiaDesconocidaException {
        TiempoAtmosferico tiempoAtmosferico = null;

        try {
            getLogger().fine("Solicitando información de tiempo atmosférico a Weatherbit...");

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
            conexion.setRequestProperty("User-Agent", "Khron Spigot Minecraft server");
            conexion.connect();

            int codigoRespuesta = conexion.getResponseCode();
            if (codigoRespuesta == 200) {
                try (JsonParser parser = Json.createParser(conexion.getInputStream())) {
                    String nombreObjeto = null;
                    boolean enObjetoTiempo = false;

                    while (parser.hasNext() && tiempoAtmosferico == null) {
                        Event evento = parser.next();

                        if (!enObjetoTiempo && evento == Event.KEY_NAME) {
                            nombreObjeto = parser.getString();
                        } else if (evento == Event.START_OBJECT && "weather".equals(nombreObjeto)) {
                            enObjetoTiempo = true;
                        } else if (evento == Event.END_OBJECT) {
                            enObjetoTiempo = false;
                        } else if (enObjetoTiempo && evento == Event.KEY_NAME && "code".equals(parser.getString())) {
                            if (parser.hasNext() && (evento = parser.next()) == Event.VALUE_STRING) {
                                int codigoTiempo = Integer.parseInt(parser.getString());

                                getLogger().log(
                                    Level.FINE, "Código de tiempo recibido de Weatherbit: " + codigoTiempo
                                );

                                if (codigoTiempo >= 200 && codigoTiempo < 300) {
                                    // Diferentes tipos de tormenta
                                    tiempoAtmosferico = TiempoAtmosferico.TORMENTA;
                                } else if (codigoTiempo >= 300 && codigoTiempo < 400) {
                                    // Diferentes tipos de llovizna
                                    tiempoAtmosferico = TiempoAtmosferico.LLUVIA_O_NIEVE;
                                } else if (codigoTiempo >= 500 && codigoTiempo < 600) {
                                    // Diferentes tipos de lluvia
                                    tiempoAtmosferico = TiempoAtmosferico.LLUVIA_O_NIEVE;
                                } else if (codigoTiempo >= 600 && codigoTiempo < 700) {
                                    // Diferentes tipos de nevada
                                    tiempoAtmosferico = TiempoAtmosferico.LLUVIA_O_NIEVE;
                                } else if (codigoTiempo >= 700 && codigoTiempo < 800) {
                                    // Diferentes tipos de niebla
                                    tiempoAtmosferico = TiempoAtmosferico.LLUVIA_O_NIEVE;
                                } else if (codigoTiempo >= 800 && codigoTiempo < 900) {
                                    // Diferentes tipos de cielos despejados
                                    tiempoAtmosferico = TiempoAtmosferico.DESPEJADO;
                                } else if (codigoTiempo == 900) {
                                    // Precipitación desconocida
                                    tiempoAtmosferico = TiempoAtmosferico.LLUVIA_O_NIEVE;
                                } else {
                                    throw new MeteorologiaDesconocidaException(
                                        "La API de Weatherbit ha devuelto un código de tiempo no reconocido: " + codigoTiempo
                                    );
                                }
                            } else {
                                throw new MeteorologiaDesconocidaException(
                                    "La API de Weatherbit ha devuelto una respuesta mal formada"
                                );
                            }
                        }
                    }
                }
            } else {
                throw new MeteorologiaDesconocidaException(
                    "La API de Weatherbit ha devuelto un código de respuesta HTTP no esperado: " + codigoRespuesta
                );
            }
        } catch (NumberFormatException | IOException exc) {
            throw new MeteorologiaDesconocidaException(
                "La API de Weatherbit ha devuelto un código de respuesta HTTP no esperado", exc
            );
        }

        return tiempoAtmosferico;
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