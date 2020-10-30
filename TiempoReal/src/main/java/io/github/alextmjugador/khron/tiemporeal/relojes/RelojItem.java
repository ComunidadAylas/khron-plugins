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
package io.github.alextmjugador.khron.tiemporeal.relojes;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;

/**
 * Modela un reloj empuñable, que muestra información de tiempo a quienes lo
 * observan mientras lo tienen en una mano.
 *
 * @param <T> El tipo de dato del estado del reloj que se puede asociar a un
 *            jugador. Puede ser {@link Void} si no se desea asociar estado.
 * @author AlexTMjugador
 */
public abstract class RelojItem<T> extends Reloj<T> {
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
     * {@inheritDoc} Los relojes empuñables devuelven verdadero si y solo si el
     * jugador tiene un stack de ítems determinado en una de sus manos, además
     * de las condiciones estipuladas en la implementación por defecto.
     */
    protected final boolean leCorrespondeVerDisplay(Player p) {
        PlayerInventory pinv;

        return super.leCorrespondeVerDisplay(p) && (
            lePermiteStackVerReloj(p, (pinv = p.getInventory()).getItemInMainHand()) ||
            lePermiteStackVerReloj(p, pinv.getItemInOffHand())
        );
    }
}