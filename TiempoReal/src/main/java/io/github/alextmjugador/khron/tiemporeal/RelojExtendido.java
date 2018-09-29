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
package io.github.alextmjugador.khron.tiemporeal;

import io.github.alextmjugador.khron.gestorbarraaccion.PluginGestorBarraAccion;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import static org.bukkit.Bukkit.getPluginManager;
import static org.bukkit.Bukkit.getServer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
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
     * Los jugadores que están viendo un reloj en el instante de tiempo
     * presente.
     */
    private final Set<Player> JUGADORES_RELOJ;
    /**
     * Tarea que se encarga de mostrar el display de la hora a los jugadores interesados.
     */
    private BukkitTask tareaMostrarHora = null;
    /**
     * El número de ticks que han de pasar entre ejecuciones consecutivas de la
     * tarea que se encarga de mostrar el display de la hora a los jugadores
     * interesados.
     */
    private static final int TICKS_TAREA_MOSTRAR_HORA = 1 * 20;
    /**
     * El número de ciclos (ejecuciones) de la tarea de mostrar el display de la
     * hora durante los que se mostrará al jugador como mínimo,
     * independientemente de si el jugador deja de empuñar el reloj o no.
     */
    private static final byte CICLOS_DISPLAY = 4;
    /**
     * El tiempo mínimo que permanecerá un display de la hora en la pantalla de
     * un jugador, independientemente de cuándo se lo quite de la mano. Se
     * calcula automáticamente a partir de atributos anteriores, así que no se
     * debe de editar manualmente.
     */
    private static final int TIEMPO_DISPLAY = 50 * TICKS_TAREA_MOSTRAR_HORA * CICLOS_DISPLAY;

    /**
     * Crea un nuevo reloj extendido.
     *
     * @throws IllegalStateException Si no se ha creado un agente de sincronización de hora antes.
     */
    public RelojExtendido() throws IllegalStateException {
        if (AgenteSincHora.getInstancia() == null) {
            throw new IllegalStateException("Se ha intentado crear un reloj extendido asociado a un agente de sincronización de hora nulo");
        }
        this.JUGADORES_RELOJ = Collections.synchronizedSet(new LinkedHashSet<>(Math.max(getServer().getMaxPlayers() / 4, 8)));
        inicializar();
    }

    /**
     * Registra los eventos manejados por esta clase con el plugin, y hace que
     * los jugadores que empuñen un reloj inicialmente vean la hora.
     */
    private void inicializar() {
        Plugin plugin = PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class);
        
        for (Player p : getServer().getOnlinePlayers()) {
            new ComprobarReloj(p).runTask(plugin);
        }
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
        PlayerInventory pinv = p.getInventory();
        ItemStack stack = pinv.getItem(event.getNewSlot());

        if ((stack != null && stack.getType().equals(Material.WATCH)) || pinv.getItemInOffHand().getType().equals(Material.WATCH)) {
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
                    new ComprobarReloj(p).runTask(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class));
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
            new ComprobarReloj(p).runTask(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class));
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
            new ComprobarReloj(p).runTask(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class));
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
            new ComprobarReloj(p).runTask(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class));
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
                tareaMostrarHora = new MostrarHora().runTaskTimer(PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class), 0, TICKS_TAREA_MOSTRAR_HORA);
            }
            
            JUGADORES_RELOJ.add(p);
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
         *
         * @param p El jugador a comprobar si puede ver la hora.
         * @throws IllegalArgumentException Si el jugador pasado como parámetro
         * es nulo.
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
     * Tarea periódica que se encarga de mostrar y mantener actualizado el
     * display de la hora en las pantallas de los jugadores.
     */
    private class MostrarHora extends BukkitRunnable {
        /**
         * Ejecuta el mostrado y actualización del display de la hora en las
         * pantallas de los jugadores. Es una tarea que tiene el potencial de
         * volverse costosa en tiempo de CPU, pues su complejidad es del orden
         * de O(n × p) / n = número de jugadores viendo el reloj, p = número de
         * mensajes pendientes de mostrarle a cada jugador en la pila.
         */
        @Override
        public void run() {
            synchronized (JUGADORES_RELOJ) {
                Plugin plugin = PluginTiempoReal.getProvidingPlugin(PluginTiempoReal.class);
                AgenteSincHora ash = AgenteSincHora.getInstancia();

                @SuppressWarnings("unchecked")
                String textoHora = (String) Configuracion.get(ParametroConfiguracion.TextoHora.class).getValor();

                for (Player p : JUGADORES_RELOJ) {
                    assert(p != null && p.isOnline());
                    
                    World w = p.getWorld();
                    byte h = ash.getHora(w);
                    byte m = ash.getMinuto(w);
                    boolean enOverworld = w.getEnvironment().equals(Environment.NORMAL);

                    if (h >= 0 && m >= 0) {
                        String hora = new StringBuilder().append(String.format("%02d", h))
                                .append(":")
                                .append(String.format("%02d", m))
                                .toString();
                        String texto = textoHora.replaceFirst(Configuracion.REGEX_CLAVE_TEXTO_HORA, enOverworld ? hora : ChatColor.MAGIC + hora);

                        PluginGestorBarraAccion.borrarMensajes(plugin, p);
                        PluginGestorBarraAccion.mostrarMensaje(plugin, p, texto, TIEMPO_DISPLAY, (byte) -10);
                    }
                }
            }
        }
    }
}