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
package io.github.alextmjugador.khron.tiemporeal;

import static org.bukkit.Bukkit.getPluginManager;
import static org.bukkit.Bukkit.getServer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.alextmjugador.khron.gestorbarraaccion.PluginGestorBarraAccion;

/**
 * Modela un reloj extendido, que provee de funcionalidades extra al ítem de
 * reloj de Minecraft.
 *
 * @author AlexTMjugador
 */
final class RelojExtendido implements Listener {
    /**
     * Los jugadores que están viendo un reloj en el instante de tiempo presente.
     */
    private final Set<Player> JUGADORES_RELOJ;
    /**
     * Guarda una referencia al plugin que contiene esta clase; es decir, este
     * plugin.
     */
    private final PluginTiempoReal estePlugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);
    /**
     * Tarea que se encarga de mostrar el display de la hora a los jugadores
     * interesados.
     */
    private BukkitTask tareaMostrarHora = null;
    /**
     * El número de ticks que han de pasar entre ejecuciones consecutivas de la
     * tarea que se encarga de mostrar el display de la hora a los jugadores
     * interesados. Este valor está pensado para que sea el mayor que, aunque no se
     * sincronice la hora en el mundo en el que está el jugador, siempre se muestre
     * un display de hora actualizado, teniendo en cuenta que 1 minuto en Minecraft
     * = 16,6 ticks = 0,83 s.
     */
    private static final short TICKS_TAREA_MOSTRAR_HORA = 8;
    /**
     * El número de ciclos (ejecuciones) de la tarea de mostrar el display de la
     * hora durante los que se mostrará al jugador como mínimo, independientemente
     * de si el jugador deja de empuñar el reloj o no.
     */
    private static final byte CICLOS_TAREA_MOSTRAR_HORA = 4;
    /**
     * El tiempo mínimo que permanecerá un display de la hora en la pantalla de un
     * jugador, independientemente de cuándo se lo quite de la mano. Se calcula
     * automáticamente a partir de atributos anteriores, así que no se debe de
     * editar manualmente.
     */
    private static final int TIEMPO_DISPLAY = TICKS_TAREA_MOSTRAR_HORA * 20000 * CICLOS_TAREA_MOSTRAR_HORA;
    /**
     * Guarda una referencia al primer (y único) objeto creado de esta clase.
     */
    private static RelojExtendido re = null;

    /**
     * Crea un nuevo reloj extendido.
     */
    private RelojExtendido() {
        this.JUGADORES_RELOJ = new LinkedHashSet<>(Math.max(getServer().getMaxPlayers() / 4, 8));
    }

    /**
     * Crea y pone en marcha un reloj extendido, que se encarga de la muestra de la
     * hora actual y de la sincronización entre la hora del servidor y las horas de
     * los mundos deseados. Para ello, registra los eventos manejados por esta clase
     * con el plugin, y hace que los jugadores que empuñen un reloj inicialmente
     * vean la hora.
     * 
     * @return El reloj extendido descrito.
     */
    public static RelojExtendido get() {
        if (re == null) {
            re = new RelojExtendido();

            // Al poner en marcha el plugin, podría haber jugadores a los que debamos de
            // mostrar la hora
            for (Player p : getServer().getOnlinePlayers()) {
                re.new ComprobarReloj(p).runTask(re.estePlugin);
            }

            getPluginManager().registerEvents(re, re.estePlugin);
        }

        return re;
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
        PlayerInventory pinv = p != null ? p.getInventory() : null;
        ItemStack stack = pinv != null ? pinv.getItem(event.getNewSlot()) : null;

        if ((stack != null && stack.getType().equals(Material.CLOCK))
                || (pinv != null && pinv.getItemInOffHand().getType().equals(Material.CLOCK))) {
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
    public void onPlayerEvent(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        if (p != null) {
            new ComprobarReloj(p).runTask(estePlugin);
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
        Player p = event.getPlayer();

        if (p != null) {
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
    public void onPlayerEvent(PlayerKickEvent event) {
        Player p = event.getPlayer();

        if (p != null) {
            ocultarDisplayHora(p);
        }
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
            case UNKNOWN:
                // Todas estas actividades pueden provocar un cambio en el ítem que se empuña en
                // alguna mano
                new ComprobarReloj(p).runTask(estePlugin);
                break;
            default:
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
            new ComprobarReloj(p).runTask(estePlugin);
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
            new ComprobarReloj(p).runTask(estePlugin);
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
        Player p = event.getPlayer();

        if (p != null) {
            new ComprobarReloj(p).runTask(estePlugin);
        }
    }

    /**
     * Añade un jugador a la lista de jugadores a los que mostrar la hora en
     * pantalla.
     *
     * @param p El jugador a añadir.
     */
    private void mostrarDisplayHora(Player p) {
        JUGADORES_RELOJ.add(p);

        // Si somos el primer jugador en ver la hora, poner tarea en marcha
        if (JUGADORES_RELOJ.size() == 1) {
            tareaMostrarHora = new MostrarHora().runTaskTimer(estePlugin, 0, TICKS_TAREA_MOSTRAR_HORA);
        }
    }

    /**
     * Elimina un jugador de la lista de jugadores a los que mostrar la hora en
     * pantalla.
     *
     * @param p El jugador a eliminar de la lista.
     */
    private void ocultarDisplayHora(Player p) {
        JUGADORES_RELOJ.remove(p);

        // Si somos el último jugador en ver el reloj, parar la tarea
        if (tareaMostrarHora != null && JUGADORES_RELOJ.isEmpty()) {
            tareaMostrarHora.cancel();
            tareaMostrarHora = null;
        }
    }

    /**
     * Tarea que decide, en última instancia, si un jugador debe de poder ver la
     * hora de su reloj o no.
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
         * @throws IllegalArgumentException Si el jugador pasado como parámetro es nulo.
         */
        public ComprobarReloj(Player p) throws IllegalArgumentException {
            if (p == null) {
                throw new IllegalArgumentException("No se puede comprobar el reloj de un jugador nulo");
            }
            this.p = p;
        }

        /**
         * Ejecuta la comprobación y decisión de si un jugador debe de poder ver la hora
         * de su reloj o no.
         */
        @Override
        public void run() {
            PlayerInventory pinv;
            if (p.isOnline()) {
                pinv = p.getInventory();

                if (pinv.getItemInMainHand().getType().equals(Material.CLOCK)
                        || pinv.getItemInOffHand().getType().equals(Material.CLOCK)) {
                    mostrarDisplayHora(p);
                } else {
                    ocultarDisplayHora(p);
                }
            } else {
                ocultarDisplayHora(p);
            }
        }
    }

    /**
     * Tarea periódica que se encarga de mostrar y mantener actualizado el display
     * de la hora en las pantallas de los jugadores.
     */
    private class MostrarHora extends BukkitRunnable {
        /**
         * Ejecuta el mostrado y actualización del display de la hora en las pantallas
         * de los jugadores. Es una tarea que tiene el potencial de volverse costosa en
         * tiempo de CPU, pues su complejidad es del orden de O(n × p) / n = número de
         * jugadores viendo el reloj, p = número de mensajes pendientes de mostrarle a
         * cada jugador en la pila.
         */
        @Override
        public void run() {
            AgenteSincHora ash = AgenteSincHora.get();
            String textoHora = estePlugin.getCfgTextoHora();
            Iterator<Player> iter = JUGADORES_RELOJ.iterator();

            while (iter.hasNext()) {
                Player p = iter.next();
                PlayerInventory pinv = p.getInventory();

                assert (p != null && p.isOnline());

                // Aunque manejamos los eventos disponibles que implican que un jugador deje de
                // tener un reloj en la mano, hay circunstancias que no podemos detectar
                // con la API actual de Paper (p. ej., ser objeto de un /clear). Así pues, es
                // necesaria esta comprobación para evitar incongruencias en esos casos
                if (!pinv.getItemInMainHand().getType().equals(Material.CLOCK)
                        && !pinv.getItemInOffHand().getType().equals(Material.CLOCK)) {
                    try {
                        iter.remove();
                    } catch (UnsupportedOperationException exc) {
                        // Para una lógica de aplicación más simple, asumimos que la implementación
                        // de iterador usada soporta la operación remove
                        assert (false);
                    }
                } else {
                    World w = p.getWorld();
                    byte h = ash.getHora(w);
                    byte m = ash.getMinuto(w);

                    if (h >= 0 && m >= 0) {
                        String hora = String.format("%02d", h) + ":" + String.format("%02d", m);
                        String textoDisplay = textoHora.replaceFirst(TextoHora.REGEX_CLAVE_TEXTO_HORA,
                                w.getEnvironment().equals(Environment.NORMAL) ? hora : ChatColor.MAGIC + hora);

                        PluginGestorBarraAccion.borrarMensajes(estePlugin, p);
                        PluginGestorBarraAccion.mostrarMensaje(estePlugin, p, textoDisplay, TIEMPO_DISPLAY, (byte) -10);
                    }
                }
            }

            // Cancelar la tarea de muestra de hora si nos hemos quedado sin jugadores a los
            // que mostrarle la hora
            if (JUGADORES_RELOJ.isEmpty()) {
                this.cancel();
                RelojExtendido.this.tareaMostrarHora = null;
            }
        }
    }
}