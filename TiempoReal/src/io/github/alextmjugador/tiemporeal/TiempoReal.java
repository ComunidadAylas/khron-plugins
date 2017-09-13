/*
 * Copyright (C) 2017 AlexTMjugador
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
package io.github.alextmjugador.tiemporeal;

import java.util.Calendar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Modela un plugin que sincroniza el tiempo real del servidor con el tiempo de todos los mundos del juego.
 * @author AlexTMjugador
 */
public class TiempoReal extends JavaPlugin implements Listener {
    private static final String TEXTO_PREDET_BARRA_HORA = "§lHora en Khron: §n§7--:--";
    private static final BossBar BARRA_HORA = Bukkit.createBossBar(TEXTO_PREDET_BARRA_HORA, BarColor.WHITE, BarStyle.SOLID);
    
    /**
     * Inicializa y planea los objetos necesarios para sincronizar el tiempo.
     */
    @Override
    public void onEnable() {
        // Visualiza la hora para todos los jugadores que pudiera haber
        for (Player p : Bukkit.getOnlinePlayers()) {
            mostrarHora(p);
        }
        
        // Eventos para visualizar la hora
        getServer().getPluginManager().registerEvents(this, this);
        
        // Tarea para sincronizar el tiempo
        new SincronizarTiempo().runTaskTimer(this, 0, 1);
    }

    /**
     * Detiene las tareas planeadas por este plugin.
     */
    @Override
    public void onDisable() {
        ocultarHora();
        Bukkit.getScheduler().cancelTasks(this);
    }
    
    /**
     * Muestra la hora a jugadores que se unen.
     * @param event El evento de unión del jugador.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        mostrarHora(event.getPlayer());
    }
    
    /**
     * Tarea para sincronizar la hora del día de todos los mundos con la del servidor.
     */
    private class SincronizarTiempo extends BukkitRunnable {
        /**
         * Sincroniza la hora del día de todos los mundos con la del servidor.
         */
        @Override
        public void run() {
            // Calcular equivalencia hora real-ticks de Minecraft
            Calendar c = Calendar.getInstance();
            byte h_real = (byte) c.get(Calendar.HOUR_OF_DAY);
            byte h = (byte) ((h_real - 6 + 24) % 24);
            byte m = (byte) c.get(Calendar.MINUTE);
            byte s = (byte) c.get(Calendar.SECOND);
            int ms = c.get(Calendar.MILLISECOND);
            long ticks = (h * 1000) + ((m * 50) / 3) + ((s * 5) / 18) + (ms / 3600);    // Se obtiene tras simplificar h * 1000 + (m / 60) * 1000 + (s / 3600) * 1000) + (ms / 1000 / 3600) * 1000
            
            // Actualizar display de la hora
            BARRA_HORA.setTitle(TEXTO_PREDET_BARRA_HORA.replaceFirst("--:--",
                    new StringBuilder().append(String.format("%02d", h_real))
                            .append(":")
                            .append(String.format("%02d", m))
                            .toString()
            ));
            
            // Para todos los mundos, asegurarse de que Minecraft no afecta al ciclo día-noche y establecer la hora apropiada
            for (World w : Bukkit.getServer().getWorlds()) {
                w.setGameRuleValue("doDaylightCycle", "false");
                w.setTime(ticks);
            }
        }
    }
    
    /**
     * Muestra la hora en pantalla a un jugador determinado.
     * @param p El jugador al que mostrar la hora en pantalla.
     */
    private static void mostrarHora(Player p) {
        BARRA_HORA.addPlayer(p);
    }
    
    /**
     * Oculta la hora en pantalla a todos los jugadores.
     */
    private static void ocultarHora() {
        BARRA_HORA.removeAll();
    }
}