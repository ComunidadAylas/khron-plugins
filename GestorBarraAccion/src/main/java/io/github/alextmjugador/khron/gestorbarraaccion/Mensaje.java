/*
 * Copyright (C) 2017 Proyecto Khron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.alextmjugador.khron.gestorbarraaccion;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import org.bukkit.entity.Player;

/**
 * Modela un mensaje a mostrar en la barra de acción de un jugador.
 *
 * @author AlexTMjugador
 */
final class Mensaje implements Comparable {
    /**
     * El texto del mensaje. Puede contener caracteres de formato, como §.
     */
    private final String mensaje;
    /**
     * La duración de este mensaje en pantalla, en milisegundos.
     */
    private final int duracion;
    /**
     * La prioridad de este mensaje. Un valor numérico mayor significa más prioridad.
     */
    private final byte prioridad;
    /**
     * La duración predeterminada del mensaje, si se omite, en milisegundos.
     */
    public static final int DURACION_PREDET = 3000;
    /**
     * La prioridad predeterminada del mensaje, si se omite.
     */
    public static final byte PRIORIDAD_PREDET = 0;

    /**
     * Crea un mensaje a mostrar en la barra de acciones.
     *
     * @param mensaje El mensaje a mostrar. Puede contener caracteres de
     * formato, como §.
     * @param duracion La duración de ese mensaje, en milisegundos.
     * @param prioridad La prioridad de este mensaje. Un valor numérico mayor
     * significa mayor prioridad. Se mostrarán primero los mensajes de mayor
     * prioridad.
     * @throws IllegalArgumentException Si la duración es menor o igual a cero.
     */
    public Mensaje(String mensaje, int duracion, byte prioridad) throws IllegalArgumentException {
        if (duracion < 1) {
            throw new IllegalArgumentException("La duración es menor o igual a 0");
        }
        this.mensaje = mensaje;
        this.duracion = duracion;
        this.prioridad = prioridad;
    }

    /**
     * Crea un mensaje a mostrar en la barra de acciones, con la prioridad
     * predeterminada.
     *
     * @param mensaje El mensaje a mostrar. Puede contener caracteres de
     * formato, como §.
     * @param duracion La duración de ese mensaje, en milisegundos.
     * @throws IllegalArgumentException Si la duración es menor que cero.
     */
    public Mensaje(String mensaje, int duracion) throws IllegalArgumentException {
        this(mensaje, duracion, PRIORIDAD_PREDET);
    }

    /**
     * Crea un mensaje a mostrar en la barra de acciones, con la duración y
     * prioridad predeterminadas.
     *
     * @param mensaje El mensaje a mostrar. Puede contener caracteres de
     * formato, como §.
     */
    public Mensaje(String mensaje) {
        this(mensaje, DURACION_PREDET, PRIORIDAD_PREDET);
    }

    /**
     * Obtiene el texto del mensaje. Puede contener caracteres de formato, como
     * §.
     *
     * @return El texto del mensaje.
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Obtiene la duración de este mensaje en pantalla, en milisegundos.
     *
     * @return La duración del mensaje en milisegundos.
     */
    public int getDuracion() {
        return duracion;
    }

    /**
     * Obtiene la prioridad de este mensaje. Un valor numérico mayor significa
     * más prioridad.
     *
     * @return La prioridad de este mensaje.
     */
    public byte getPrioridad() {
        return prioridad;
    }

    /**
     * Muestra este mensaje a un determinado jugador.
     *
     * @param p El mensaje a mostrar.
     * @throws IllegalArgumentException Si el jugador es nulo o no está
     * conectado.
     */
    public void mostrar(Player p) throws IllegalArgumentException {
        if (p == null || !p.isOnline()) {
            throw new IllegalArgumentException("No se puede mostrar un mensaje a un jugador nulo o desconectado");
        }
        // Duración dividida entre 50 para pasar a ticks
        // Se le resta uno debido a que la API envía un mensaje en blanco un tick después de que expire la duración, lo cual da problemas visuales al jugador
        ActionBarAPI.sendActionBar(p, getMensaje(), Math.max((int) Math.ceil(getDuracion() / 50) - 1, 1));
    }

    /**
     * Compara el mensaje actual con el especificado, en lo que a prioridades se
     * refiere.
     *
     * @param t El mensaje a comparar con el actual. Debe de ser una instancia
     * de la clase {@link Mensaje}.
     * @return Un entero negativo, igual a cero o positivo si t es menor, igual
     * o mayor que el mensaje actual, respectivamente.
     */
    @Override
    public int compareTo(Object t) {
        Mensaje msg = (Mensaje) t;
        return this.getPrioridad() - msg.getPrioridad();
    }
}