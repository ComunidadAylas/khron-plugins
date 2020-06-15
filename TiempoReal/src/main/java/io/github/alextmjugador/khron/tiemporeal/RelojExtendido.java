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

import org.bukkit.Bukkit;
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

    private static final int TIEMPO_DISPLAY = (TICKS_TAREA_MOSTRAR_HORA / 2) * 100 * CICLOS_TAREA_MOSTRAR_HORA;
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
                re.actualizarDisplayHora(p);
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
        PlayerInventory pinv = p.getInventory();
        ItemStack stack = pinv.getItem(event.getNewSlot());

        if (
            (stack != null && Material.CLOCK.equals(stack.getType())) ||
            Material.CLOCK.equals(pinv.getItemInOffHand().getType())
        ) {
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
        actualizarDisplayHora(event.getPlayer());
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
            case UNKNOWN:
                // Todas estas actividades pueden provocar un cambio en el ítem que se empuña en
                // alguna mano
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        actualizarDisplayHora(p);
                    }
                }.runTask(estePlugin);
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
            // No sabemos cuál será el resultado exacto de este evento ahora mismo,
            // así que retrasamos la comprobación al siguiente tick
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualizarDisplayHora(p);
                }
            }.runTask(estePlugin);
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

        if (p != null && Material.CLOCK.equals(event.getItem().getItemStack().getType())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualizarDisplayHora(p);
                }
            }.runTask(estePlugin);
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
        actualizarDisplayHora(event.getPlayer());
    }

    /**
     * Comprueba si a un jugador le corresponde ver la hora en pantalla.
     *
     * @param p El jugador a comprobar.
     * @return Verdadero si le corresponde, falso en otro caso.
     */
    private boolean leCorrespondeVerDisplayHora(Player p) {
        PlayerInventory pinv = p.isOnline() ? p.getInventory() : null;

        return pinv != null && !p.isDead() && (
            pinv.getItemInMainHand().getType().equals(Material.CLOCK) ||
            pinv.getItemInOffHand().getType().equals(Material.CLOCK)
        );
    }

    /**
     * Añade un jugador a la lista de jugadores a los que mostrar la hora en
     * pantalla.
     *
     * @param p El jugador a añadir.
     */
    private void mostrarDisplayHora(Player p) {
        // Si somos el primer jugador en ver la hora, poner tarea en marcha
        if (p != null && JUGADORES_RELOJ.add(p) && tareaMostrarHora == null) {
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
     * Le muestra u oculta el display de hora a un jugador, dependiendo de si le
     * corresponde verlo o no.
     *
     * @param p El jugador cuyo estado de muestra de display actualizar.
     */
    private void actualizarDisplayHora(Player p) {
        if (leCorrespondeVerDisplayHora(p)) {
            mostrarDisplayHora(p);
        } else {
            ocultarDisplayHora(p);
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

                // Aunque manejamos los eventos disponibles que implican que un jugador deje de
                // tener un reloj en la mano, hay circunstancias que no podemos detectar
                // con la API actual de Paper (p. ej., ser objeto de un /clear). Así pues, es
                // necesaria esta comprobación para evitar incongruencias en esos casos
                if (!leCorrespondeVerDisplayHora(p)) {
                    iter.remove();
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

            // Puede ocurrir que eliminemos jugadores durante la iteración.
            // En ese caso, debemos de cancelar la ejecución de la tarea si nos quedamos sin jugadores
            if (JUGADORES_RELOJ.isEmpty()) {
                cancel();
                RelojExtendido.this.tareaMostrarHora = null;
            }
        }
    }
}