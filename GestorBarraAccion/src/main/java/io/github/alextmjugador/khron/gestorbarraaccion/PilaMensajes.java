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

import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Representa una pila de mensajes a mostrar en la barra de acciones de un
 * jugador, ordenada de mayor a menor prioridad (de manera que el mensaje de
 * mayor prioridad está en la cima de la pila). Solamente soporta la operación
 * de añadir elementos, pues quitarlos se hace automáticamente (véase el método
 * mostrar). Debido a la ordenación de los elementos, se comporta como una
 * estructura LIFO si y solo si los elementos insertados tienen mayor prioridad
 * que los presentes en la pila.
 *
 * @author AlexTMjugador
 */
final class PilaMensajes {
    /**
     * La pila que contiene los mensajes a mostrar.
     */
    private final Stack<Mensaje> pila;
    /**
     * La tarea de Bukkit encargada de mostrar los mensajes de la pila.
     */
    private BukkitTask tareaMostrarSig;
    
    /**
     * Crea una nueva pila de mensajes vacía.
     */
    public PilaMensajes() {
        this.pila = new Stack<>();
        this.tareaMostrarSig = null;
    }

    /**
     * Inserta un mensaje en la pila.
     *
     * @param msg El mensaje a insertar en la pila.
     * @throws IllegalArgumentException Si el mensaje es nulo.
     */
    public void push(Mensaje msg) throws IllegalArgumentException {
        if (msg == null) {
            throw new IllegalArgumentException("No se puede insertar un mensaje nulo a la pila");
        }
        
        synchronized (pila) {
            // Algoritmo eficiente para obtener posición de insertado basado en búsqueda binaria
            int i = 0; // Tras el while que sigue, toma el valor de la posición donde insertar el nuevo objeto
            int j = pila.size() - 1;

            while (i <= j) {
                int k = (i + j) / 2;
                if (pila.get(k).compareTo(msg) < 0) {  // (La operación get se ejecuta en tiempo constante, pues el stack se implementa usando un vector)
                    // El mensaje en k tiene menos prioridad que el nuevo. El nuevo debe insertarse después
                    i = k + 1;
                } else {
                    // El mensaje en k tiene mayor o igual prioridad que el nuevo. Debe de insertarse antes
                    j = k - 1;
                }
            }

            pila.add(i, msg); // (Se ejecuta en tiempo lineal)
        }
    }

    /**
     * Inserta los mensajes contenidos en una lista en la pila.
     *
     * @param mensajes La lista de mensajes a insertar en la pila.
     * @throws IllegalArgumentException Si la lista o algún mensaje es nulo.
     */
    public void push(List<Mensaje> mensajes) throws IllegalArgumentException {
        if (mensajes == null) {
            throw new IllegalArgumentException("No se pueden insertar elementos de una lista nula a la pila");
        }
        for (Mensaje msg : mensajes) {
            push(msg);
        }
    }
    
    /**
     * Vacía la pila y para cualquier tarea para visualizar mensajes de ella.
     */
    public void empty() {
        synchronized (pila) {
            parar();
            pila.removeAllElements();
        }
    }
    
    /**
     * Vacía la pila de mensajes generados por un determinado plugin. No afecta
     * a la visualización de los mensajes que puedan quedar.
     *
     * @param plugin El plugin al nombre del cual se han creado los mensajes.
     * @return El número de mensajes generados por el plugin especificado que han sido borrados de la pila.
     */
    public int empty(Plugin plugin) {
        int toret = 0;
        Iterator<Mensaje> iter;
        
        if (plugin != null) {
            synchronized (pila) {
                iter = pila.iterator();
                while (iter.hasNext()) { // (Complejidad lineal)
                    Mensaje actual = iter.next();
                    if (actual.getPlugin().equals(plugin)) {
                        iter.remove();
                        ++toret;
                    }
                }
            }
        }
        
        return toret;
    }
    
    /**
     * Muestra todos los mensajes en esta pila. Si ya se están mostrando,
     * sobreescribe la tarea interna que se encarga de ello. Esto muestra el
     * siguiente mensaje antes de tiempo si el jugador especificado es el mismo
     * que el de la anterior llamada a este método. Si no es el mismo, solamente
     * se le mostrarán los siguientes mensajes al nuevo jugador, empezando en el
     * momento presente con el siguiente.
     *
     * @param jugador El jugador al que mostrar los mensajes.
     * @param plugin El plugin responsable por mostrar los mensajes.
     * @throws EmptyStackException Si la pila está vacía; es decir, no hay
     * mensajes que mostrar.
     * @throws IllegalArgumentException Si el jugador es nulo o está
     * desconectado, o bien si el plugin es nulo o no se está ejecutando en el
     * servidor.
     */
    public void mostrar(Player jugador, Plugin plugin) throws EmptyStackException, IllegalArgumentException {
        synchronized (pila) {
            if (pila.isEmpty()) {
                throw new EmptyStackException();
            }
        }
        if (jugador == null || !jugador.isOnline()) {
            throw new IllegalArgumentException("No se pueden mostrar pilas de mensajes a un jugador nulo o desconectado");
        }
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalArgumentException("No se puede mostrar mensajes desde un plugin que no se ejecuta");
        }
        if (mostrando()) {
            // Cancelar tarea actual
            parar();
        }

        tareaMostrarSig = new MostrarSig(jugador, plugin).runTask(plugin);
    }
    
    /**
     * Comprueba si se está mostrando la pila o no.
     *
     * @return Verdadero si la pila se está mostrando a un jugador, falso si no.
     */
    public boolean mostrando() {
        return tareaMostrarSig != null;
    }

    /**
     * Para cualquier tarea para visualizar futuros mensajes de esta pila. El
     * jugador seguirá viendo los mensajes que le hayan sido enviados, hasta que
     * su duración expire.
     */
    public void parar() {
        if (mostrando()) {
            // Cancela la tarea para mostrar el siguiente mensaje, y borra su referencia
            tareaMostrarSig.cancel();
            tareaMostrarSig = null;
        }
    }

    /**
     * Modela una tarea para mostrar todos los mensajes en la pila al jugador deseado.
     */
    private class MostrarSig extends BukkitRunnable {
        /**
         * El jugador al que mostrar los mensajes.
         */
        private final Player jugador;
        /**
         * El plugin responsable por mostrar los mensajes.
         */
        private final Plugin plugin;

        /**
         * Crea una nueva tarea para mostrar todos los mensajes en la pila al
         * jugador deseado.
         *
         * @param jugador El jugador al que mostrar los mensajes.
         * @param plugin El plugin responsable por mostrar los mensajes.
         */
        public MostrarSig(Player jugador, Plugin plugin) {
            this.jugador = jugador;
            this.plugin = plugin;
        }

        /**
         * Ejecuta la muestra de los mensajes de la pila al jugador deseado.
         */
        @Override
        public void run() {
            try {
                // Toma el siguiente mensaje a mostrar
                synchronized (pila) {
                    Mensaje msg;
                    if (pila.isEmpty()) {
                        tareaMostrarSig = null;
                    } else {
                        msg = pila.pop();
                        // Se lo muestra al jugador y programa una tarea para el siguiente, si hay más
                        msg.mostrar(jugador);
                        tareaMostrarSig = new MostrarSig(jugador, plugin).runTaskLater(plugin, (int) msg.getDuracion() / 50); // Dividir entre 50 = / 1000 y * 20
                    }
                }
            } catch (IllegalArgumentException exc) {
                // Si se produce algún error mostrando el mensaje, no intentar mostrar más
                parar();
            }
        }
    }
}