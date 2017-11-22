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
package io.github.alextmjugador.khron.tiemporeal;

import io.github.alextmjugador.khron.gestorbarraaccion.PluginGestorBarraAccion;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import static org.bukkit.Bukkit.getPluginManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Modela un reloj extendido, que provee de funcionalidades extra al ítem de
 * reloj de Minecraft.
 *
 * @author AlexTMjugador
 */
final class RelojExtendido implements Listener {
    /**
     * El objeto {@link Plugin} asociado a este reloj extendido.
     */
    private final Plugin plugin;
    /**
     * Los jugadores que están viendo un reloj en el instante de tiempo
     * presente. El byte se utiliza para contar el número de ciclos de
     * {@link tareaMostrarHora} que han pasado sin actualizar el display al
     * jugador.
     */
    private final Map<Player,Byte> JUGADORES_RELOJ;
    /**
     * Tarea que se encarga de mostrar el display de la hora a los jugadores interesados.
     */
    private BukkitTask tareaMostrarHora = null;
    /**
     * El agente de sincronización de hora usado por este plugin.
     */
    private final AgenteSincHora ash;
    /**
     * El número de ticks que han de pasar entre ejecuciones consecutivas de la
     * tarea que se encarga de mostrar el display de la hora a los jugadores
     * interesados.
     */
    private static final int TICKS_TAREA_MOSTRAR_HORA = 1 * 20;
    /**
     * El número de ciclos (ejecuciones) de la tarea de mostrar el display de la
     * hora que han de pasar para que jugadores que dejen de empuñar un reloj ya
     * no vean en pantalla su display asociado.
     */
    private static final byte CICLOS_TAREA_DISPLAY = 4;
    /**
     * El tiempo mínimo que permanecerá un display de la hora en la pantalla de
     * un jugador, independientemente de cuándo se lo quite de la mano. Se
     * calcula automáticamente a partir de atributos anteriores, así que no se
     * debe de editar manualmente.
     */
    private static final int TIEMPO_DISPLAY = 50 * TICKS_TAREA_MOSTRAR_HORA * CICLOS_TAREA_DISPLAY;

    /**
     * Crea un nuevo reloj extendido.
     *
     * @param plugin El plugin asociado a este reloj.
     * @param ash El agente de sincronización de hora asociado al plugin.
     * @throws IllegalArgumentException Si el parámetro plugin y/o ash es nulo.
     */
    public RelojExtendido(Plugin plugin, AgenteSincHora ash) throws IllegalArgumentException {
        if (plugin == null || ash == null) {
            throw new IllegalArgumentException("Se ha intentado crear un reloj extendido asociado a un plugin o a un agente de sincronización de hora nulo");
        }
        this.plugin = plugin;
        this.JUGADORES_RELOJ = new HashMap<>();
        this.ash = ash;
        inicializar();
    }

    /**
     * Registra los eventos manejados por esta clase con el plugin.
     */
    private void inicializar() {
        getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerEvent(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        ItemStack stack = p.getInventory().getItem(event.getNewSlot());

        if (stack != null && stack.getType().equals(Material.WATCH)) {
            mostrarDisplayHora(p);
        } else {
            ocultarDisplayHora(p);
        }
    }
    
    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerEvent(PlayerQuitEvent event) {
        ocultarDisplayHora(event.getPlayer());
    }
    
    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerEvent(PlayerKickEvent event) {
        ocultarDisplayHora(event.getPlayer());
    }
    
    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryInteractEvent(InventoryClickEvent event) {
        Player p = (event.getWhoClicked() instanceof Player) ? (Player) event.getWhoClicked() : null;
        
        if (p != null) {
            switch (event.getAction()) {
                case COLLECT_TO_CURSOR: // Recoger ítems de un determinado tipo al cursor
                case HOTBAR_MOVE_AND_READD: // Se mueve stack a la hotbar
                case HOTBAR_SWAP: // Se intercambian slots. Uno de ellos está en hotbar
                case MOVE_TO_OTHER_INVENTORY: // Se mueve stack a otro inventario
                case PICKUP_ALL: // Se mueven al cursor uno o más ítems de un stack en el inventario
                case PICKUP_HALF:
                case PICKUP_ONE:
                case PICKUP_SOME:
                case PLACE_ALL: // Se mueven al inventario uno o más ítems del cursor
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR: // Se intercambia cursor con slot de inventario
                    // Todas estas actividades pueden provocar un cambio en el ítem que se empuña en alguna mano
                    new ComprobarReloj(p).runTask(plugin);
                    break;
            }
        }
    }
    
    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryInteractEvent(InventoryDragEvent event) {
        Player p = (event.getWhoClicked() instanceof Player) ? (Player) event.getWhoClicked() : null;
        
        if (p != null) {
            new ComprobarReloj(p).runTask(plugin);
        }
    }
    
    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryInteractEvent(EntityPickupItemEvent event) {
        Player p = (event.getEntity() instanceof Player) ? (Player) event.getEntity() : null;
        
        if (p != null) {
            new ComprobarReloj(p).runTask(plugin);
        }
    }
    
    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryInteractEvent(PlayerDropItemEvent event) {
        Player p = (event.getPlayer() instanceof Player) ? (Player) event.getPlayer() : null;
        
        if (p != null) {
            new ComprobarReloj(p).runTask(plugin);
        }
    }
    
    /**
     * Añade un jugador a la lista de jugadores a los que mostrar la hora en
     * pantalla.
     *
     * @param p El jugador a añadir.
     */
    private void mostrarDisplayHora(Player p) {
        synchronized (JUGADORES_RELOJ) {
            // Si somos el primer jugador en ver la hora, poner tarea en marcha
            if (JUGADORES_RELOJ.isEmpty()) {
                tareaMostrarHora = new MostrarHora().runTaskTimer(plugin, 0, TICKS_TAREA_MOSTRAR_HORA);
            }
            
            JUGADORES_RELOJ.putIfAbsent(p, (byte) 0);
        }
    }

    /**
     * Elimina un jugador de la lista de jugadores a los que mostrar la hora en
     * pantalla.
     *
     * @param p El jugador a eliminar de la lista.
     */
    private void ocultarDisplayHora(Player p) {
        synchronized (JUGADORES_RELOJ) {
            JUGADORES_RELOJ.remove(p);
            
            // Si somos el último jugador en ver el reloj, parar la tarea
            if (tareaMostrarHora != null && JUGADORES_RELOJ.isEmpty()) {
                tareaMostrarHora.cancel();
                tareaMostrarHora = null;
            }
        }
    }
    
    /**
     * Tarea que decide, en última instancia, si un jugador debe de poder ver la hora de su reloj o no.
     */
    private class ComprobarReloj extends BukkitRunnable {
        /**
         * El jugador a comprobar si puede ver la hora.
         */
        private final Player p;

        /**
         * Crea una nueva tarea que decide si un jugador puede ver la hora.
         * @param p El jugador a comprobar si puede ver la hora.
         * @throws IllegalArgumentException Si el jugador pasado como parámetro es nulo.
         */
        public ComprobarReloj(Player p) throws IllegalArgumentException {
            if (p == null) {
                throw new IllegalArgumentException("No se puede comprobar el reloj de un jugador nulo");
            }
            this.p = p;
        }

        /**
         * Ejecuta la comprobación y decisión de si un jugador debe de poder ver la hora de su reloj o no.
         */
        @Override
        public void run() {
            PlayerInventory pinv;
            if (p.isOnline()) {
                pinv = p.getInventory();
                
                if (pinv.getItemInMainHand().getType().equals(Material.WATCH)
                        || pinv.getItemInOffHand().getType().equals(Material.WATCH)) {
                    mostrarDisplayHora(p);
                } else {
                    ocultarDisplayHora(p);
                }
            }
        }
    }
    
    /**
     * Tarea periódica que se encarga de mostrar y mantener actualizado el display de la hora en las pantallas de los jugadores.
     */
    private class MostrarHora extends BukkitRunnable {
        /**
         * Ejecuta el mostrado y actualización del display de la hora en las pantallas de los jugadores.
         */
        @Override
        public void run() {
            synchronized (JUGADORES_RELOJ) {
                if (ash.getHora() >= 0 && ash.getMinuto() >= 0) {
                    String hora = new StringBuilder().append(String.format("%02d", ash.getHora()))
                            .append(":")
                            .append(String.format("%02d", ash.getMinuto()))
                            .toString();

                    StringBuilder texto = new StringBuilder();
                    texto.append("§bEl reloj marca las §l");
                    texto.append(hora);

                    for (Entry<Player,Byte> entrada : JUGADORES_RELOJ.entrySet()) {
                        if (entrada.getValue() == 0) {
                            // Si es 0, acaba de coger el reloj o han pasado los ciclos mínimos para volverle a mostrar el display
                            PluginGestorBarraAccion.mostrarMensaje(entrada.getKey(), texto.toString(), TIEMPO_DISPLAY, (byte) -10);
                        }
                        // Incrementar el contador de ciclos
                        entrada.setValue((byte) ((entrada.getValue() + 1) % CICLOS_TAREA_DISPLAY));
                    }
                }
            }
        }
    }
}