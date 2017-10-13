/*
 * Copyright (C) 2017 Khron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.alextmjugador.khron.tiemporeal;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Modela un plugin que sincroniza el tiempo real del servidor con el tiempo de todos los mundos del juego. También muestra la hora real al empuñar un reloj.
 * @author AlexTMjugador
 */
public class TiempoReal extends JavaPlugin implements Listener {
    /**
     * Contiene los jugadores que están visualizando un reloj.
     */
    private final List<Player> JUGADORES_RELOJ = new ArrayList<>(Math.max(getServer().getMaxPlayers() / 5, 1));
    
    /**
     * Inicializa y planea los objetos necesarios para sincronizar el tiempo y relojes.
     */
    @Override
    public void onEnable() {
        // Eventos para gestión de los relojes
        getServer().getPluginManager().registerEvents(this, this);
        
        // Tarea para sincronizar el tiempo y relojes cada tick
        new SincronizarTiempo().runTaskTimer(this, 0, 1);
    }

    /**
     * Detiene las tareas planeadas por este plugin.
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
    }
    
    /**
     * Muestra u oculta la hora a jugadores que empuñan un reloj o lo guardan.
     * @param event El evento de cambio de ítem seleccionado.
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        ItemStack stack = p.getInventory().getItem(event.getNewSlot());
        
        if (stack != null && stack.getType().equals(Material.WATCH)) {
            mostrarHora(p);
        } else {
            ocultarHora(p);
        }
    }
    
    /**
     * Oculta la hora a jugadores que se desconecten, si la estaban mirando.
     * @param event El evento de desconexión del jugador.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ocultarHora(event.getPlayer());
    }
    
    /**
     * Oculta la hora a jugadores que sean echados del juego, si la estaban mirando.
     * @param event El evento de expulsión del jugador.
     */
    @EventHandler
    public void onKick(PlayerKickEvent event) {
        ocultarHora(event.getPlayer());
    }
    
    /**
     * Tarea para sincronizar la hora del día de todos los mundos con la del servidor. También muestra una representación
     */
    private class SincronizarTiempo extends BukkitRunnable {
        /**
         * Sincroniza la hora del día de todos los mundos con la del servidor.
         */
        @Override
        public void run() {
            Calendar horaServidor = Calendar.getInstance();
            
            // Calcular equivalencia hora real-ticks de Minecraft
            byte h_real = (byte) horaServidor.get(Calendar.HOUR_OF_DAY);
            byte h = (byte) ((h_real - 6 + 24) % 24);
            byte m = (byte) horaServidor.get(Calendar.MINUTE);
            byte s = (byte) horaServidor.get(Calendar.SECOND);
            int ms = horaServidor.get(Calendar.MILLISECOND);
            long ticks = (h * 1000) + ((m * 50) / 3) + ((s * 5) / 18) + (ms / 3600);    // Se obtiene tras simplificar h * 1000 + (m / 60) * 1000 + (s / 3600) * 1000) + (ms / 1000 / 3600) * 1000
            
            // Actualizar display de la hora a jugadores interesados
            if (JUGADORES_RELOJ.size() > 0) {
                String hora = new StringBuilder().append(String.format("%02d", h_real))
                        .append(":")
                        .append(String.format("%02d", m))
                        .toString();

                StringBuilder texto = new StringBuilder();
                texto.append("§bEl reloj marca las §l");
                texto.append(hora);
                
                for (Player p : JUGADORES_RELOJ) {
                    ActionBarAPI.sendActionBar(p, texto.toString());
                }
            }
            
            // Para todos los mundos, asegurarse de que Minecraft no afecta al ciclo día-noche y establecer la misma hora que en el servidor
            for (World w : getServer().getWorlds()) {
                w.setGameRuleValue("doDaylightCycle", "false");
                w.setTime(ticks);
            }
        }
    }
    
    /**
     * Añade un jugador a la lista de jugadores a los que mostrar la hora en pantalla.
     * @param p El jugador a añadir.
     */
    private void mostrarHora(Player p) {
        if (!JUGADORES_RELOJ.contains(p)) {
            JUGADORES_RELOJ.add(p);
        }
    }
    
    /**
     * Elimina un jugador de la lista de jugadores a los que mostrar la hora en pantalla.
     * @param p El jugador a eliminar de la lista.
     */
    private void ocultarHora(Player p) {
        JUGADORES_RELOJ.remove(p);
    }
}