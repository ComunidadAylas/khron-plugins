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
package io.github.alextmjugador.khron.tiemporeal.relojes;

import static org.bukkit.Bukkit.getServer;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;

/**
 * Modela un reloj, que muestra información de tiempo a quienes lo observan.
 *
 * @author AlexTMjugador
 */
public abstract class Reloj implements Listener {
    /**
     * El número de ticks que han de pasar entre ejecuciones consecutivas de la
     * tarea que se encarga de mostrar el display de tiempo a los jugadores
     * interesados. Este valor está pensado para que sea el mayor que, aunque no se
     * sincronice la hora en el mundo en el que está el jugador, siempre se muestre
     * un display de hora actualizado, teniendo en cuenta que 1 minuto en Minecraft
     * = 16,6 ticks = 0,83 s.
     */
    protected static final short TICKS_TAREA_DISPLAY = 8;

    /**
     * Los jugadores que están viendo un reloj en el instante de tiempo presente.
     */
    private final Set<Player> jugadoresReloj = new LinkedHashSet<>(
        (int) (Math.max(getServer().getMaxPlayers() / 4, 8) / 0.75)
    );

    /**
     * Tarea que se encarga de mostrar el display a los jugadores
     * interesados.
     */
    private BukkitTask tareaMostrarDisplay = null;

    /**
     * Crea un nuevo reloj observable por un jugador.
     */
    protected Reloj() {
        // Al poner en marcha el reloj, podría haber jugadores a los que debamos de
        // mostrar el display
        for (Player p : getServer().getOnlinePlayers()) {
            actualizarDisplay(p);
        }
    }

    /**
     * Detiene la muestra de display de relojes para todos los jugadores.
     */
    public void ocultarTodosLosDisplay() {
        if (tareaMostrarDisplay != null) {
            tareaMostrarDisplay.cancel();
            tareaMostrarDisplay = null;
        }

        jugadoresReloj.clear();
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerJoinEvent event) {
        actualizarDisplay(event.getPlayer());
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerQuitEvent event) {
        ocultarDisplay(event.getPlayer());
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerKickEvent event) {
        ocultarDisplay(event.getPlayer());
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerDropItemEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                actualizarDisplay(event.getPlayer());
            }
        }.runTask(PluginTiempoReal.getPlugin(PluginTiempoReal.class));
    }

    /**
     * Oculta el display a jugadores que mueren.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerDeathEvent event) {
        ocultarDisplay(event.getEntity());
    }

    /**
     * Decide si volver a mostrar el display a jugadores que reviven.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerRespawnEvent event) {
        actualizarDisplay(event.getPlayer());
    }

    /**
     * Decide si mostrar u ocultar la hora a jugadores que realicen algún evento
     * relacionado con empuñar o guardar un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerItemHeldEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                actualizarDisplay(event.getPlayer());
            }
        }.runTask(PluginTiempoReal.getPlugin(PluginTiempoReal.class));
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onInventoryEvent(InventoryClickEvent event) {
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
                        actualizarDisplay(p);
                    }
                }.runTask(PluginTiempoReal.getPlugin(PluginTiempoReal.class));
                break;
            default:
                break;
            }
        }
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onInventoryEvent(InventoryDragEvent event) {
        Player p = (event.getWhoClicked() instanceof Player) ? (Player) event.getWhoClicked() : null;

        if (p != null) {
            // No sabemos cuál será el resultado exacto de este evento ahora mismo,
            // así que retrasamos la comprobación al siguiente tick
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualizarDisplay(p);
                }
            }.runTask(PluginTiempoReal.getPlugin(PluginTiempoReal.class));
        }
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con su inventario.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onEntityEvent(EntityPickupItemEvent event) {
        Player p = (event.getEntity() instanceof Player) ? (Player) event.getEntity() : null;

        if (p != null && lePermiteStackVerReloj(p, event.getItem().getItemStack())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    actualizarDisplay(p);
                }
            }.runTask(PluginTiempoReal.getPlugin(PluginTiempoReal.class));
        }
    }

    /**
     * Le muestra u oculta el display de un reloj a un jugador, dependiendo de si le
     * corresponde verlo o no.
     *
     * @param p El jugador cuyo estado de muestra de display actualizar.
     */
    public final void actualizarDisplay(Player p) {
        if (leCorrespondeVerDisplay(p)) {
            mostrarDisplay(p);
        } else {
            ocultarDisplay(p);
        }
    }

    /**
     * Comprueba si el stack de ítems empuñado especificado le permite a un jugador
     * observar el reloj.
     *
     * @param jugador El jugador que tiene el stack de ítems. No es nulo.
     * @param stack El stack de ítems a comprobar. No es nulo.
     * @return Verdadero si el jugador puede ver el reloj en virtud del stack de
     *         ítems empuñado, falso en otro caso.
     */
    protected abstract boolean lePermiteStackVerReloj(Player jugador, ItemStack stack);

    /**
     * Obtiene el texto del display del reloj a mostrarle a un jugador en la barra
     * de acción.
     *
     * @param jugador El jugador al que mostrarle el display. No es nulo.
     * @param mundo   El mundo en el que se encuentra el jugador. No es nulo.
     * @return El texto a mostrar, no nulo.
     */
    protected abstract String obtenerTextoDisplay(Player jugador, World mundo);

    /**
     * Comprueba si a un jugador le corresponde ver el display en pantalla.
     *
     * @param p El jugador a comprobar.
     * @return Verdadero si le corresponde, falso en otro caso.
     */
    private boolean leCorrespondeVerDisplay(Player p) {
        PlayerInventory pinv = p.isOnline() ? p.getInventory() : null;

        return pinv != null && !p.isDead() && (
            lePermiteStackVerReloj(p, pinv.getItemInMainHand()) ||
            lePermiteStackVerReloj(p, pinv.getItemInOffHand())
        );
    }

    /**
     * Añade un jugador a la lista de jugadores a los que mostrar el display en
     * pantalla.
     *
     * @param p El jugador a añadir.
     */
    private void mostrarDisplay(Player p) {
        // Si somos el primer jugador en ver la hora, poner tarea en marcha
        if (p != null && jugadoresReloj.add(p) && tareaMostrarDisplay == null) {
            tareaMostrarDisplay = new MostrarHora().runTaskTimer(
                PluginTiempoReal.getPlugin(PluginTiempoReal.class), 0, TICKS_TAREA_DISPLAY
            );
        }
    }

    /**
     * Elimina un jugador de la lista de jugadores a los que mostrar el display del
     * reloj en pantalla.
     *
     * @param p El jugador a eliminar de la lista.
     */
    private void ocultarDisplay(Player p) {
        jugadoresReloj.remove(p);

        // Si somos el último jugador en ver el reloj, parar la tarea
        if (tareaMostrarDisplay != null && jugadoresReloj.isEmpty()) {
            tareaMostrarDisplay.cancel();
            tareaMostrarDisplay = null;
        }
    }

    /**
     * Tarea periódica que se encarga de mostrar y mantener actualizado el display
     * de la hora en las pantallas de los jugadores.
     *
     * @author AlexTMjugador
     */
    private final class MostrarHora extends BukkitRunnable {
        /**
         * Ejecuta el mostrado y actualización del display de la hora en las pantallas
         * de los jugadores.
         */
        @Override
        public void run() {
            Iterator<Player> iter = jugadoresReloj.iterator();
            while (iter.hasNext()) {
                Player p = iter.next();

                // Aunque manejamos los eventos disponibles que implican que un jugador deje de
                // tener un reloj en la mano, hay circunstancias que no podemos detectar
                // con la API actual de Paper (p. ej., ser objeto de un /clear). Así pues, es
                // necesaria esta comprobación para evitar incongruencias en esos casos
                if (!leCorrespondeVerDisplay(p)) {
                    iter.remove();
                } else {
                    // TODO: usar un nuevo gestor de barra de acción simple que junte varios mensajes
                    // de la barra de acción en uno solo y mande ese; el anterior gestor era
                    // demasiado complejo, dependía de una biblioteca algo fea y no funcionaba
                    // como esperaba
                    p.sendActionBar(obtenerTextoDisplay(p, p.getWorld()));
                }
            }

            // Puede ocurrir que eliminemos jugadores durante la iteración.
            // En ese caso, debemos de cancelar la ejecución de la tarea si nos quedamos sin jugadores
            if (jugadoresReloj.isEmpty()) {
                cancel();
                tareaMostrarDisplay = null;
            }
        }
    }
}