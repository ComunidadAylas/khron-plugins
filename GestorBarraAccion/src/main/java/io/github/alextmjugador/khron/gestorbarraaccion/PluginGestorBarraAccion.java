/*
 * Plugins de Spigot del Proyecto Khron
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
package io.github.alextmjugador.khron.gestorbarraaccion;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Implementa un plugin que muestra mensajes en la barra de acción de manera no
 * intrusiva con otros plugins, permitiéndoles abstraerse de lo que ellos
 * realicen.
 *
 * @author AlexTMjugador
 */
public final class PluginGestorBarraAccion extends JavaPlugin implements Listener {
    /**
     * El mapa de pilas de mensajes pendientes por jugadores. Es null hasta que
     * el plugin se activa.
     */
    private static final Map<Player, PilaMensajes> MENSAJES_PENDIENTES = new HashMap<>();
    /**
     * La instancia en ejecución del plugin.
     */
    private static PluginGestorBarraAccion estePlugin = null;

    /**
     * Inicializa el atributo estático {@link estePlugin}.
     */
    @Override
    public void onEnable() {
        PluginGestorBarraAccion.estePlugin = this;
    }
    
    /**
     * Muestra un mensaje en la barra de acciones de un jugador.
     *
     * @param plugin El plugin al nombre del cual se creará el mensaje.
     * @param p El jugador al que mostrarle el mensaje.
     * @param msg El mensaje a mostrar.
     * @param duracion La duración del mensaje, en milisegundos.
     * @param prioridad La prioridad del mensaje. Ver implementación de
     * {@link Mensaje} para más información sobre su significado numérico.
     * @return Verdadero si el mensaje se ha podido mostrar, falso en caso
     * contrario.
     */
    public static boolean mostrarMensaje(Plugin plugin, Player p, String msg, int duracion, byte prioridad) {
        boolean toret = false;
        
        if (msg != null) {
            toret = mostrarArray(p, new Mensaje[]{ new Mensaje(msg, duracion, prioridad, plugin) });
        }
        
        return toret;
    }
    
    /**
     * Muestra un mensaje en la barra de acciones de un jugador, con la
     * prioridad predeterminada.
     *
     * @param plugin El plugin al nombre del cual se creará el mensaje.
     * @param p El jugador al que mostrarle el mensaje.
     * @param msg El mensaje a mostrar.
     * @param duracion La duración del mensaje, en milisegundos.
     * @return Verdadero si el mensaje se ha podido mostrar, falso en caso
     * contrario.
     */
    public static boolean mostrarMensaje(Plugin plugin, Player p, String msg, int duracion) {
        return mostrarMensaje(plugin, p, msg, duracion, Mensaje.PRIORIDAD_PREDET);
    }
    
    /**
     * Muestra uno o varios mensajes en la barra de acciones de un jugador, con
     * duración y prioridades predeterminadas.
     *
     * @param plugin El plugin al nombre del cual se creará el mensaje.
     * @param p El jugador al que mostrarle el/los mensaje/s.
     * @param msg Los mensajes a mostrar.
     * @return Verdadero si el/los mensaje/s se ha/n podido mostrar, falso en
     * caso contrario.
     */
    public static boolean mostrarMensaje(Plugin plugin, Player p, String... msg) {
        boolean toret = false;
        Mensaje[] mensajes;
        
        if (msg != null) {
            // Crear objetos Mensaje a partir de las cadenas de texto
            mensajes = new Mensaje[msg.length];
            for (int i = 0; i < msg.length; ++i) {
                mensajes[i] = new Mensaje(msg[i], plugin);
            }

            toret = mostrarArray(p, mensajes);
        }
        
        return toret;
    }
    
    /**
     * Borra todos los mensajes pendientes de mostrarle a un jugador en la barra
     * de acciones. El jugador seguirá viendo el mensaje actual hasta que su
     * duración expire. Para forzar una limpieza de la barra de acciones, llamar
     * a este método y enviar un mensaje en blanco, o bien enviar un mensaje en
     * blanco con máxima prioridad (asumiendo que no hay otros de igual
     * prioridad en la pila).
     *
     * @param p El jugador al que borrarle los mensajes pendientes.
     * @return Verdadero si se han borrado mensajes pendientes del jugador,
     * falso en caso contrario.
     */
    public static boolean borrarMensajes(Player p) {
        boolean toret = MENSAJES_PENDIENTES.containsKey(p);
        
        if (toret) {
            PilaMensajes pila = MENSAJES_PENDIENTES.get(p);
            toret = pila.mostrando();
            pila.empty();
        }
        
        return toret;
    }
    
    /**
     * Borra los mensajes pendientes de mostrarle a un jugador en la barra de
     * acciones creados por un determinado plugin. El jugador seguirá viendo el
     * mensaje actual hasta que su duración expire. Para forzar una limpieza de
     * la barra de acciones, llamar a este método y enviar un mensaje en blanco,
     * o bien enviar un mensaje en blanco con máxima prioridad (asumiendo que no
     * hay otros de igual prioridad en la pila).
     *
     * @param plugin El plugin al nombre del cual se han creado los mensajes.
     * @param p El jugador al que borrarle los mensajes pendientes.
     * @return Verdadero si se han borrado mensajes pendientes del jugador,
     * falso en caso contrario.
     */
    public static boolean borrarMensajes(Plugin plugin, Player p) {
        boolean toret = MENSAJES_PENDIENTES.containsKey(p);
        
        if (toret) {
            PilaMensajes pila = MENSAJES_PENDIENTES.get(p);
            toret = pila.empty(plugin) > 0;
        }
        
        return toret;
    }
    
    /**
     * Inserta los mensajes contenidos en el array en la pila de mensajes pendientes del jugador especificado.
     * @param p El jugador al que insertarle los mensajes en la pila.
     * @param arr El array de mensajes a insertar.
     * @return Verdadero si se han insertado y podido mostrar los mensajes de la pila, falso en caso contrario.
     */
    private static boolean mostrarArray(Player p, Mensaje[] arr) {
        boolean toret = false;
        PilaMensajes pila;
        
        if (estePlugin != null) {
            pila = MENSAJES_PENDIENTES.get(p);
            if (pila == null) {
                pila = new PilaMensajes();
                MENSAJES_PENDIENTES.put(p, pila);
            }

            try {
                pila.push(Arrays.asList(arr));
                if (!pila.mostrando()) {
                    pila.mostrar(p, estePlugin);
                }
                toret = true;
            } catch (IllegalArgumentException | EmptyStackException exc) {
                // Ignorar detalles del error
            }
        }
        
        return toret;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        borrarJugadorEvento(e);
    }
    
    @EventHandler
    public void onPlayerKick(PlayerKickEvent e) {
        borrarJugadorEvento(e);
    }
    
    /**
     * Reacciona a un evento de expulsión o desconexión de un jugador, parando y
     * borrando su pila de mensajes pendientes.
     *
     * @param e El evento del jugador en cuestión.
     */
    private void borrarJugadorEvento(PlayerEvent e) {
        Player p = e.getPlayer();
        PilaMensajes pila = MENSAJES_PENDIENTES.get(p);
        if (pila != null) {
            pila.parar();
            MENSAJES_PENDIENTES.remove(p);
        }
    }
}