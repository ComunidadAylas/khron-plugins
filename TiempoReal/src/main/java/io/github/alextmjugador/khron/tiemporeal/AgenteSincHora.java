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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Bukkit.getWorlds;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Implementa un agente de sincronización de hora real con la del juego.
 *
 * @author AlexTMjugador
 */
final class AgenteSincHora implements Listener {
    /**
     * La hora actual en el mundo.
     */
    private byte horaActual = Byte.MIN_VALUE;
    
    /**
     * El minuto de la hora actual en el mundo.
     */
    private byte minutoActual = Byte.MIN_VALUE;
    
    /**
     * El objeto {@link Plugin} asociado a este agente de sincronización de hora.
     */
    private final Plugin plugin;
    
    /**
     * La gamerule que nos interesa controlar en este plugin: el ciclo
     * día-noche.
     */
    private static final String GAMERULE = "doDaylightCycle";
    
    /**
     * Error a mostrar cuando un operador o la consola intenten cambiar el
     * gamerule {@link GAMERULE}.
     */
    private static final String ERROR_GAMERULE = "No se puede cambiar el valor de esta propiedad mientras el plugin TiempoReal controle al ciclo día-noche.";
    
    /**
     * El comando que se usa para cambiar el valor del gamerule doDaylightCycle.
     */
    private static final String COMANDO_GAMERULE = "/gamerule " + GAMERULE;
    
    /**
     * Contiene el estado inicial de la gamerule {@link GAMERULE} para cada
     * mundo, antes de que este plugin lo estableciese.
     */
    private final Map<World, Boolean> ESTADO_INIC_GAMERULE = new HashMap<>(5);
    
    /**
     * Crea un nuevo agente de sincronización de hora real con la del juego.
     *
     * @param plugin El plugin sobre el que se ejecutarán las acciones de este
     * agente.
     */
    public AgenteSincHora(Plugin plugin) throws IllegalArgumentException {
        if (plugin == null) {
            throw new IllegalArgumentException("Se ha intentado crear un agente de sincronización de hora asociado a un plugin nulo");
        }
        this.plugin = plugin;
        inicializar();
    }
    
    /**
     * Registra los eventos manejados por este agente con el plugin, y crea tareas periódicas.
     */
    private void inicializar() {
        // Tarea para sincronizar el tiempo cada segundo
        new SincronizarTiempo().runTaskTimer(plugin, 0, 20);
        
        // Registrar eventos que nos conciernen
        getServer().getPluginManager().registerEvents(this, plugin);
        
        // Inicializar campos de los mundos ya cargados
        for (World w : getServer().getWorlds()) {
            onWorldInit(new WorldInitEvent(w));
        }
    }
    
    /**
     * Detiene el ciclo natural día-noche de Minecraft de un mundo que se carga.
     *
     * @param event El evento de inicialización del mundo.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldInit(WorldInitEvent event) {
        World w = event.getWorld();
        if (w.getEnvironment().equals(Environment.NORMAL)) {
            ESTADO_INIC_GAMERULE.putIfAbsent(w, w.getGameRuleValue(GAMERULE).equals("true"));
            w.setGameRuleValue(GAMERULE, "false");
        }
    }
    
    /**
     * Restablece el ciclo natural día-noche de Minecraft de un mundo que se descarga.
     * 
     * @param event El evento de descarga del mundo.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        World w = event.getWorld();
        if (w.getEnvironment().equals(Environment.NORMAL)) {
            assert(ESTADO_INIC_GAMERULE.containsKey(w));
            w.setGameRuleValue(GAMERULE, ESTADO_INIC_GAMERULE.get(w) ? "true" : "false");
        }
    }
    
    /**
     * Cancela el comando de cambiar el ciclo día-noche puesto por un jugador.
     *
     * @param event El evento de comando puesto por un jugador.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith(COMANDO_GAMERULE)) {
            event.getPlayer().sendRawMessage(ChatColor.RED + ERROR_GAMERULE);
            event.setCancelled(true);
        }
    }
    
    /**
     * Cancela el comando de cambiar el ciclo día-noche puesto por el servidor.
     *
     * @param event El evento de comando puesto por el servidor.
     */
    @EventHandler(ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().startsWith(COMANDO_GAMERULE)) {
            getServer().getLogger().info(ERROR_GAMERULE);
            event.setCancelled(true);
        }
    }

    /**
     * Obtiene la hora actual del juego, que coincide, como máximo con un pequeño retraso, con la real.
     * 
     * @return La hora actual del juego. Puede ser negativa si la hora aún no fue sincronizada.
     */
    public byte getHora() {
        return horaActual;
    }

    /**
     * Obtiene el minuto de la hora actual del juego, que coincide, como máximo con un pequeño retraso, con el de la real.
     * 
     * @return El minuto actual del juego. Puede ser negativo si la hora aún no fue sincronizada.
     */
    public byte getMinuto() {
        return minutoActual;
    }
    
    /**
     * Tarea para sincronizar la hora del día de todos los mundos con la del
     * servidor.
     */
    private class SincronizarTiempo extends BukkitRunnable {
        /**
         * Sincroniza la hora del día de todos los mundos con la del servidor.
         */
        @Override
        public void run() {
            Calendar horaServidor = Calendar.getInstance();
            List<World> mundos = getWorlds();
            
            if (!mundos.isEmpty()) {
                // Calcular equivalencia hora real-ticks de Minecraft
                byte h_real = (byte) horaServidor.get(Calendar.HOUR_OF_DAY);
                byte h = (byte) ((h_real - 6 + 24) % 24);   // El día de Minecraft empieza a las 6 AM
                byte m = (byte) horaServidor.get(Calendar.MINUTE);
                byte s = (byte) horaServidor.get(Calendar.SECOND);
                short ms = (short) horaServidor.get(Calendar.MILLISECOND);
                long ticks = (h * 1000) + ((m * 50) / 3) + ((s * 5) / 18) + (ms / 3600);    // Se obtiene tras simplificar h * 1000 + (m / 60) * 1000 + (s / 3600) * 1000) + (ms / 1000 / 3600) * 1000

                for (World w : mundos) {
                    w.setTime(ticks);
                }

                horaActual = h_real;
                minutoActual = m;
            }
        }
    }
}