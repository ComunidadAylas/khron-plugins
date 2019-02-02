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

import static org.bukkit.Bukkit.getServer;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.alextmjugador.khron.libconfig.NotificableCambioConfiguracion;

/**
 * Implementa un agente de sincronización de hora real con la del juego.
 *
 * @author AlexTMjugador
 */
final class AgenteSincHora implements Listener, NotificableCambioConfiguracion<Set<World>> {
    /**
     * La hora actual en el mundo.
     */
    private byte horaActual = Byte.MIN_VALUE;

    /**
     * El minuto de la hora actual en el mundo.
     */
    private byte minutoActual = Byte.MIN_VALUE;

    /**
     * La tarea de sincronización de hora usada por este agente para lograr su
     * propósito.
     */
    private BukkitTask tareaSincHora = null;

    /**
     * Guarda una referencia al primer (y único) objeto creado de esta clase.
     */
    private static AgenteSincHora ash = null;
    /**
     * Guarda una referencia al plugin que contiene y provee este agente.
     */
    private static PluginTiempoReal estePlugin;

    /**
     * La gamerule que nos interesa controlar en este plugin: el ciclo día-noche.
     */
    private static final GameRule<Boolean> GAMERULE = GameRule.DO_DAYLIGHT_CYCLE;

    /**
     * Error a mostrar cuando un operador o la consola intenten cambiar el gamerule
     * {@link GAMERULE}.
     */
    private static final String ERROR_GAMERULE = "No se puede cambiar el valor de esta propiedad mientras el plugin TiempoReal controle al ciclo día-noche.";

    /**
     * El comando que se usa para cambiar el valor del gamerule doDaylightCycle.
     */
    private static final String COMANDO_GAMERULE = "/gamerule " + GAMERULE.getName();

    /**
     * Contiene los mundos en los que está actuando este agente, además del estado
     * inicial de la gamerule {@link GAMERULE} para cada mundo, antes de que este
     * plugin lo estableciese.
     */
    private static final Map<World, Boolean> MUNDOS_Y_GAMERULE = new HashMap<>(getServer().getWorlds().size());

    /**
     * Los ticks que transcurrirán entre dos sincronizaciones consecutivas de la
     * hora del servidor con las horas de los mundos.
     */
    private static final int TICKS_SINC_HORA = 20;

    /**
     * Impide que se generen instancias de esta clase por parte de código externo a
     * esta clase. Permite, por tanto, aplicar el patrón singleton.
     */
    private AgenteSincHora() {}

    /**
     * Crea un nuevo agente de sincronización de hora real con la del juego,
     * registrando los eventos que maneja en el proceso, u obtiene el ya creado.
     */
    public static AgenteSincHora get() {
        if (ash == null) {
            AgenteSincHora ash = new AgenteSincHora();

            // Actualizar referencia al agente creado
            AgenteSincHora.ash = ash;

            // Registrar eventos que nos conciernen
            estePlugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);
            getServer().getPluginManager().registerEvents(ash, estePlugin);
        }

        return ash;
    }

    /**
     * Detiene el ciclo natural día-noche de Minecraft de un mundo que se carga, si
     * está en el conjunto de mundos en los que este plugin sincronizará la hora.
     *
     * @param event El evento de carga del mundo.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        World w = event.getWorld();

        if (estePlugin.getCfgMundosSincronizacion().contains(w)) {
            sincronizarHoraMundo(w);
        }
    }

    /**
     * Restablece el ciclo natural día-noche de Minecraft de un mundo que se
     * descarga, si es necesario.
     *
     * @param event El evento de descarga del mundo.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        World w = event.getWorld();

        if (MUNDOS_Y_GAMERULE.containsKey(w)) {
            w.setGameRule(GAMERULE, MUNDOS_Y_GAMERULE.get(w));
            MUNDOS_Y_GAMERULE.remove(w);

            if (MUNDOS_Y_GAMERULE.isEmpty() && tareaSincHora != null) {
                tareaSincHora.cancel();
                tareaSincHora = null;
            }
        }
    }

    /**
     * Se asegura de que la gamerule de ciclo día-noche se restablece
     * consistentemente en mundos que ya no se controlen, y coloca nuevos mundos en
     * el mapa {@link MUNDOS_Y_GAMERULE}.
     *
     * @param antiguoValor El antiguo valor que tomaba la configuración.
     * @param nuevoValor   El nuevo valor que va a tomar la configuración, cuyo tipo
     *                     es determinado por la clase {@link MundosSincronizacion}.
     */
    @Override
    public void onNewConfig(Set<World> antiguoValor, Set<World> nuevoValor) {
        // Restaurar propiedades simulando descarga de los mundos
        for (World w : MUNDOS_Y_GAMERULE.keySet()) {
            ash.onWorldUnload(new WorldUnloadEvent(w));
        }

        // Recargar nuevos mundos a manejar
        for (World w : nuevoValor) {
            ash.sincronizarHoraMundo(w); // No se usa el evento porque el nuevo valor aún no ha sido establecido en
                                         // la configuración
        }
    }

    /**
     * Cancela el comando de cambiar el ciclo día-noche puesto por un jugador en un
     * mundo controlado por este agente.
     *
     * @param event El evento de comando puesto por un jugador.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();

        if (event.getMessage().startsWith(COMANDO_GAMERULE)
                && estePlugin.getCfgMundosSincronizacion().contains(p.getWorld())) {
            p.sendRawMessage(ChatColor.RED + ERROR_GAMERULE);
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
            estePlugin.getLogger().info(ERROR_GAMERULE);
            event.setCancelled(true);
        }
    }

    /**
     * Obtiene la hora actual de un mundo, que coincide, como máximo con un pequeño
     * retraso, con la real, si este agente sincroniza la hora en él.
     *
     * @param w El mundo del que obtener la hora.
     * @return La hora actual del mundo. Puede ser negativa si la hora aún no fue
     *         sincronizada cuando debería.
     */
    public byte getHora(World w) {
        byte toret;

        if (MUNDOS_Y_GAMERULE.containsKey(w)) {
            toret = horaActual;
        } else {
            // Convierte ticks del día actual del mundo de Minecraft a hora
            toret = (byte) ((w.getTime() / 1000 + 6) % 24);
        }

        return toret;
    }

    /**
     * Obtiene el minuto de la hora actual del juego, que coincide, como máximo con
     * un pequeño retraso, con el de la real, si este agente sincroniza la hora en
     * él.
     *
     * @param w El mundo del que obtener el minuto.
     * @return El minuto actual del juego. Puede ser negativo si la hora aún no fue
     *         sincronizada cuando debería.
     */
    public byte getMinuto(World w) {
        byte toret;

        if (MUNDOS_Y_GAMERULE.containsKey(w)) {
            toret = minutoActual;
        } else {
            // Convierte ticks del día actual del mundo de Minecraft a minuto de la hora
            // actual
            toret = (byte) ((60 * (w.getTime() % 1000)) / 1000);
        }

        return toret;
    }

    /**
     * Añade el mundo especificado al mapa de mundos a sincronizar, sin importar si
     * está en la configuración o no.
     *
     * @param w El mundo a añadir.
     */
    private void sincronizarHoraMundo(World w) {
        MUNDOS_Y_GAMERULE.putIfAbsent(w, w.getGameRuleValue(GAMERULE));
        w.setGameRule(GAMERULE, false);

        if (MUNDOS_Y_GAMERULE.size() == 1 && tareaSincHora == null) {
            tareaSincHora = new SincronizarTiempo().runTaskTimer(estePlugin, 0, TICKS_SINC_HORA);
        }
    }

    /**
     * Tarea para sincronizar la hora del día de todos los mundos configurados con
     * la del servidor.
     */
    private class SincronizarTiempo extends BukkitRunnable {
        /**
         * Sincroniza la hora del día de todos los mundos configurados con la del
         * servidor.
         */
        @Override
        public void run() {
            Calendar horaServidor = Calendar.getInstance();
            Set<World> mundosHora = MUNDOS_Y_GAMERULE.keySet();

            if (!mundosHora.isEmpty()) {
                // Calcular equivalencia hora real-ticks de Minecraft
                byte h_real = (byte) horaServidor.get(Calendar.HOUR_OF_DAY);
                byte h = (byte) ((h_real - 6 + 24) % 24); // El día de Minecraft empieza a las 6 AM
                byte m = (byte) horaServidor.get(Calendar.MINUTE);
                byte s = (byte) horaServidor.get(Calendar.SECOND);
                short ms = (short) horaServidor.get(Calendar.MILLISECOND);
                long ticks = (h * 1000) + ((m * 50) / 3) + ((s * 5) / 18) + (ms / 3600); // Se obtiene tras simplificar
                                                                                         // h * 1000 + (m / 60) * 1000 +
                                                                                         // (s / 3600) * 1000 + (ms /
                                                                                         // 1000 / 3600) * 1000

                for (World w : mundosHora) {
                    w.setTime(ticks);
                }

                horaActual = h_real;
                minutoActual = m;
            }
        }
    }
}