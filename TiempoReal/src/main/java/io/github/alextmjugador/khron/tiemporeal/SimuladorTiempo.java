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

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Bukkit.getWorlds;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.github.alextmjugador.khron.libconfig.NotificableCambioConfiguracion;
import io.github.alextmjugador.khron.tiemporeal.astronomia.ArcoDiurnoSolar;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.ParametrosSimulacionMundo;
import io.github.alextmjugador.khron.tiemporeal.meteorologia.Clima;
import io.github.alextmjugador.khron.tiemporeal.meteorologia.MeteorologiaDesconocidaException;
import io.github.alextmjugador.khron.tiemporeal.meteorologia.TiempoAtmosferico;

/**
 * Simula características de tiempo atmosférico y de reloj de un mundo,
 * siguiendo estrategias configurables para tal propósito.
 *
 * @author AlexTMjugador
 */
public final class SimuladorTiempo
    implements Listener, NotificableCambioConfiguracion<Map<String, ParametrosSimulacionMundo>>
{
    /**
     * Los ticks que transcurrirán entre dos actualizaciones consecutivas de la
     * simulación del tiempo de los mundos.
     */
    private static final int TICKS_ACTUALIZACION_SIMULACION = 10;

    /**
     * Error a mostrar cuando un operador o la consola intenten cambiar una
     * propiedad de un mundo simulada por esta clase.
     */
    private static final String PROPIEDAD_RESTRINGIDA =
        "No se puede cambiar el valor de esta propiedad mientras el plugin TiempoReal simule " +
        "propiedades de tiempo de este mundo.";

    /**
     * El comando que se usa para cambiar el valor del gamerule doDaylightCycle.
     */
    private static final Pattern COMANDO_GAMERULE = Pattern.compile(
        "^/?gamerule " + GameRule.DO_DAYLIGHT_CYCLE.getName() + " "
    );

    /**
     * El patrón que representa el comando para cambiar la hora de un mundo.
     */
    private static final Pattern COMANDO_CAMBIO_HORA = Pattern.compile("^/?time (add|set)");

    /**
     * Los mundos que se están simulando actualmente.
     */
    private final Map<World, Boolean> mundosSimulados = new HashMap<>(
        (int) (getServer().getWorlds().size() / 0.75)
    );

    /**
     * Alberga cuándo se ha solicitado por última vez un cálculo a un
     * determinado clima.
     */
    private final Map<Clima, Long> ultimoCalculoClima = new HashMap<>(3);

    /**
     * La caché de tiempos de reloj simulados calculados para cada mundo, usada
     * para evitar repetir cálculos en cada tick de la simulación.
     */
    private final Cache<Vector, Long> cacheTiemposCalculados = CacheBuilder.newBuilder()
        .concurrencyLevel(1)
        .softValues() // Es deseable permitir que Java quite entradas si cree que tiene poca memoria
        .initialCapacity(Math.max(getServer().getMaxPlayers() / 2, 8))
        .build();

    /**
     * La tarea usada para actualizar periódicamente la simulación del tiempo de
     * los mundos.
     */
    private BukkitTask tareaActualizacionSimulacion = null;

    /**
     * El último momento simulado en los ciclos diurnos de todos los mundos.
     */
    private Instant ultimoMomentoSimulado = null;

    /**
     * Restringe la instanciación de este objeto.
     */
    private SimuladorTiempo() {}

    /**
     * Obtiene la única instancia del simulador de ciclo diurno en la JVM,
     * creándola si no lo ha sido ya.
     *
     * @return La devandicha instancia.
     */
    public static SimuladorTiempo get() {
        return PoseedorInstanciaClase.INSTANCIA;
    }

    /**
     * Comienza la simulación de un mundo que se carga, si está en el conjunto
     * de mundos en los que este plugin actúa.
     *
     * @param event El evento de carga del mundo.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        comenzarSimulacionSiCorresponde(event.getWorld());
    }

    /**
     * Detiene ordenadamente la simulación de un mundo que se descarga, si es
     * necesario.
     *
     * @param event El evento de descarga del mundo.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        detenerSimulacion(event.getWorld());
    }

    /**
     * Se asegura de que diversas propiedades se restablecen correctamente en mundos
     * que ya no se simulen, y gestiona el comienzo de la simulación de nuevos
     * mundos.
     *
     * @param antiguoValor El antiguo valor que tomaba la configuración.
     * @param nuevoValor   El nuevo valor que va a tomar la configuración.
     */
    @Override
    public void onNewConfig(
        Map<String, ParametrosSimulacionMundo> antiguoValor,
        Map<String, ParametrosSimulacionMundo> nuevoValor
    ) {
        // Ignorar la primera vez que se llama a este evento
        if (antiguoValor != null) {
            // Detener simulación de todos los mundos anteriores
            detenerSimulacion();

            // Iniciar simulación de los nuevos mundos
            for (String w : nuevoValor.keySet()) {
                comenzarSimulacion(getServer().getWorld(w));
            }
        }
    }

    /**
     * Cancela la ejecución de comandos que interfieren con propiedades del mundo simuladas por el plugin.
     *
     * @param event El evento de comando recibido.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String mensaje = event.getMessage();

        if (
            mundosSimulados.containsKey(p.getWorld()) &&
            (COMANDO_GAMERULE.matcher(mensaje).find() || COMANDO_CAMBIO_HORA.matcher(mensaje).find())
        ) {
            p.sendRawMessage(ChatColor.RED + PROPIEDAD_RESTRINGIDA);
            event.setCancelled(true);
        }
    }

    /**
     * Cancela la ejecución de comandos que interfieren con propiedades del mundo
     * simuladas por el plugin.
     *
     * @param event El evento de comando recibido.
     */
    @EventHandler(ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String mensaje = event.getCommand();
        CommandSender responsable;
        World w;

        if (COMANDO_GAMERULE.matcher(mensaje).find() || COMANDO_CAMBIO_HORA.matcher(mensaje).find()) {
            responsable = event.getSender();
            w = null;

            if (responsable instanceof BlockCommandSender) {
                w = ((BlockCommandSender) responsable).getBlock().getWorld();
            } else if (responsable instanceof Entity) {
                w = ((Entity) responsable).getWorld();
            } else if (responsable instanceof ConsoleCommandSender || responsable instanceof RemoteConsoleCommandSender) {
                w = getWorlds().get(0);
            }

            if (mundosSimulados.containsKey(w)) {
                getLogger().info(PROPIEDAD_RESTRINGIDA);
                event.setCancelled(true);
            }
        }
    }

    /**
     * Restaura la hora del día vista por el cliente para mundos que no estén
     * siendo simulados por este objeto.
     *
     * @param event El evento de cambio de mundo recibido.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        if (mundosSimulados.containsKey(event.getFrom())) {
            // Restaurar sincronización predeterminada de la hora del cliente
            // con la del servidor. Esto corrige una desincronización en caso
            // de que el jugador vaya a un mundo cuyo ciclo no está siendo
            // simulado, y también hace que se note menos el retraso hasta la
            // próxima simulación en el caso de que vaya a otro mundo sí
            // simulado (solamente no se vería bien la fase de la luna por 0,5 s)
            event.getPlayer().resetPlayerTime();
        }
    }

    /**
     * Obtiene la última hora simulada en un mundo o, de no haberse simulado aún
     * un ciclo diurno en el mundo, la hora del día de Minecraft.
     *
     * @param w El mundo del que obtener la hora.
     * @return La hora actual del mundo, no nula.
     */
    public ZonedDateTime getHoraMundo(World w) {
        ZonedDateTime hora;

        ParametrosSimulacionMundo parametrosSimulacion = PluginTiempoReal
            .getPlugin(PluginTiempoReal.class).getParametrosSimulacionMundo().get(w.getName());

        if (mundosSimulados.containsKey(w) && ultimoMomentoSimulado != null && parametrosSimulacion != null) {
            hora = ZonedDateTime.ofInstant(
                ultimoMomentoSimulado,
                parametrosSimulacion.getFranjaHoraria()
            );
        } else {
            // Si el mundo no está siendo simulado, o no tenemos datos de su simulación,
            // deducir su hora a partir de las mecánicas usuales de Minecraft
            hora = ZonedDateTime.ofInstant(
                // + 21600000 porque los ticks empiezan a contar a las 6 AM
                Instant.ofEpochMilli(w.getFullTime() * 3600 + 21600000),
                ZoneId.systemDefault()
            );
        }

        return hora;
    }

    /**
     * Comienza la simulación del tiempo de todos los mundos que se especifiquen
     * en la configuración, ya cargada.
     */
    public void comenzarSimulacion() {
        for (World w : getServer().getWorlds()) {
            comenzarSimulacionSiCorresponde(w);
        }
    }

    /**
     * Detiene la simulación del tiempo de todos los mundos.
     */
    public void detenerSimulacion() {
        // El cuerpo de este método debe de ser similar al de #detenerSimulacion(World)
        // (No es igual para que sea algo más eficiente)

        if (tareaActualizacionSimulacion != null) {
            tareaActualizacionSimulacion.cancel();
            tareaActualizacionSimulacion = null;
            ultimoMomentoSimulado = null;
        }

        Iterator<Entry<World, Boolean>> iter = mundosSimulados.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<World, Boolean> entrada = iter.next();
            World w = entrada.getKey();

            TiempoAtmosferico.restaurarMundo(w);

            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, entrada.getValue());

            for (Player p : w.getPlayers()) {
                TiempoAtmosferico.restaurarJugador(p);
                p.resetPlayerTime();
            }

            iter.remove();
        }
    }

    /**
     * Comienza la simulación del tiempo de un mundo si ello está recogido en la
     * configuración del plugin.
     *
     * @param w El mundo del que comenzar a simular su tiempo.
     */
    private void comenzarSimulacionSiCorresponde(World w) {
        Map<String, ParametrosSimulacionMundo> parametros = PluginTiempoReal.getPlugin(PluginTiempoReal.class)
            .getParametrosSimulacionMundo();

        if (parametros != null && parametros.get(w.getName()) != null) {
            comenzarSimulacion(w);
        }
    }

    /**
     * Comienza la simulación del tiempo de un mundo, independientemente de que
     * ello esté recogido en la configuración del plugin. Se asume que, de no
     * estarlo, lo estará enseguida.
     *
     * @param w El mundo del que comenzar su tiempo.
     */
    private void comenzarSimulacion(World w) {
        if (w != null && mundosSimulados.putIfAbsent(w, w.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)) == null) {
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);

            if (tareaActualizacionSimulacion == null) {
                tareaActualizacionSimulacion = new ActualizarSimulacion().runTaskTimer(
                    PluginTiempoReal.getPlugin(PluginTiempoReal.class), 0, TICKS_ACTUALIZACION_SIMULACION
                );
            }
        }
    }

    /**
     * Detiene la simulación del tiempo de un mundo que está siendo simulado.
     *
     * @param w El mundo del que detener la simulación del tiempo.
     */
    private void detenerSimulacion(World w) {
        Boolean estadoPrevioGamerule = mundosSimulados.remove(w);

        if (estadoPrevioGamerule != null) {
            TiempoAtmosferico.restaurarMundo(w);

            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, estadoPrevioGamerule);

            // Restaurar sincronización predeterminada de la hora del cliente
            // con la del servidor
            for (Player p : w.getPlayers()) {
                TiempoAtmosferico.restaurarJugador(p);
                p.resetPlayerTime();
            }

            if (tareaActualizacionSimulacion != null && mundosSimulados.isEmpty()) {
                tareaActualizacionSimulacion.cancel();
                tareaActualizacionSimulacion = null;
                ultimoMomentoSimulado = null;
            }
        }
    }

    /**
     * Tarea para actualizar la simulación del tiempo de los mundos a intervalos
     * de tiempo discretos.
     *
     * @author AlexTMjugador
     */
    private class ActualizarSimulacion extends BukkitRunnable {
        @Override
        public void run() {
            Instant ahora = Instant.now();

            Map<String, ParametrosSimulacionMundo> parametrosSimulacionMundos = PluginTiempoReal
                .getPlugin(PluginTiempoReal.class).getParametrosSimulacionMundo();

            for (World w : mundosSimulados.keySet()) {
                ParametrosSimulacionMundo parametrosSimulacionMundo = parametrosSimulacionMundos.get(w.getName());

                // Si no tenemos los parámetros de simulación del mundo para este mundo es porque acabamos de cambiar
                // la configuración o algo parecido, así que simplemente ignorar el mundo
                if (parametrosSimulacionMundo != null) {
                    // Obtener los parámetros de simulación del mundo
                    double latitudSpawn = parametrosSimulacionMundo.getLatitudSpawn();
                    double longitudSpawn = parametrosSimulacionMundo.getLongitudSpawn();
                    double radio = parametrosSimulacionMundo.getRadio() * 1000.0; // Pasar a metros
                    ArcoDiurnoSolar arcoDiurnoSolar = parametrosSimulacionMundo.getArcoDiurnoSolar();
                    Clima clima = parametrosSimulacionMundo.getClima();
                    float maximosCalculosClimaDia = clima.maximasInvocacionesPorDiaPermitidas();
                    boolean climaSimulado = clima.simulaMeteorologia();
                    boolean tiempoSimulado = arcoDiurnoSolar.simulaPlaneta();

                    // La distancia mínima a recorrer en un eje de latitud o longitud por un jugador
                    // para incrementarla 0,5 grados = 0,00872665 radianes
                    double umbralAgrupamiento = radio * 0.00872665;

                    Location puntoAparicionMundo = w.getSpawnLocation();

                    // Establecer el tiempo del mundo en el servidor, usado para las mecánicas del juego,
                    // según lo calculado por el arco diurno configurado, si corresponde
                    if (tiempoSimulado) {
                        w.setTime(arcoDiurnoSolar.getTiempoMundo(ahora, latitudSpawn, longitudSpawn));

                        // Discretizar la posición del punto de aparición y guardarla en la caché,
                        // para que jugadores cerca del punto de aparición puedan obtener el tiempo
                        // calculado de ella más rápidamente
                        Vector puntoAparicionMundoDiscretizado = new Vector(
                            Math.floor(puntoAparicionMundo.getX() / umbralAgrupamiento),
                            0,
                            Math.floor(puntoAparicionMundo.getZ() / umbralAgrupamiento)
                        );

                        // Calcular el tiempo de los jugadores cerca del spawn, y guardarlo si se va a usar
                        if (w.getPlayerCount() > 0) {
                            long tiempoJugador = arcoDiurnoSolar.getTiempoJugador(ahora, w, latitudSpawn, longitudSpawn);
                            cacheTiemposCalculados.put(puntoAparicionMundoDiscretizado, tiempoJugador);
                        }
                    }

                    // Guardar el tiempo del mundo desde el comienzo del primer día, que usaremos luego
                    long tiempoMundo = w.getFullTime();

                    // Calcular el clima del mundo si corresponde
                    if (climaSimulado) {
                        actualizarTiempoAtmosferico(
                            clima, latitudSpawn, longitudSpawn, maximosCalculosClimaDia,
                            (TiempoAtmosferico t) -> {
                                t.aplicarAMundo(w);
                            }
                        );
                    }

                    // Establecer un umbral en 1 cálculo por jugador + 1 cálculo por mundo en cada tick de simulación
                    // para la simulación de tiempo atmosférico por jugador
                    float umbralCalculos = (1728000f / TICKS_ACTUALIZACION_SIMULACION) *
                        (getServer().getMaxPlayers() + mundosSimulados.size());

                    // Ahora simular el tiempo de reloj y atmosférico particular para cada jugador
                    for (Player p : w.getPlayers()) {
                        Location posicionOjos = p.getEyeLocation();

                        // Obtener el ángulo de desplazamiento respecto al punto de aparición, a partir
                        // de la distancia en latitud y longitud
                        double deltax = (puntoAparicionMundo.getX() - posicionOjos.getX()) / radio;
                        double deltaz = (puntoAparicionMundo.getZ() - posicionOjos.getZ()) / radio;

                        // Sumar el desplazamiento a la latitud y longitud del punto de aparición
                        double latitudJugador = latitudSpawn + deltaz;
                        double longitudJugador = longitudSpawn + deltax;

                        // Obtener el tiempo a mostrarle al jugador de la caché, si es posible,
                        // o calcularlo si no está
                        if (tiempoSimulado) {
                            try {
                                long tiempoJugador = cacheTiemposCalculados.get(
                                    new Vector(
                                        Math.floor(posicionOjos.getX() / umbralAgrupamiento),
                                        0,
                                        Math.floor(posicionOjos.getZ() / umbralAgrupamiento)
                                    ), () -> {
                                        return arcoDiurnoSolar.getTiempoJugador(
                                            ahora, w, latitudJugador, longitudJugador
                                        );
                                    }
                                );

                                // El tiempo visible para un cliente es siempre relativo a otro tiempo,
                                // porque el API de Bukkit está algo mal documentada en este aspecto. Véase:
                                // https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/EntityPlayer.java#L1077
                                // https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/org/bukkit/craftbukkit/entity/CraftPlayer.java#L680
                                // https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/PacketPlayOutUpdateTime.java#L5
                                // https://github.com/Attano/Spigot-1.8/blob/9db48bc15e203179554b8d992ca6b0a528c8d300/net/minecraft/server/v1_8_R3/MinecraftServer.java#L745
                                // Por tanto, lo más sencillo es enviarle a cada cliente la desviación del tiempo que deberían de ver
                                // respecto al tiempo del servidor
                                p.setPlayerTime(tiempoJugador - tiempoMundo, true);
                            } catch (ExecutionException exc) {
                                getLogger().log(
                                    Level.WARNING,
                                    "Ha ocurrido una excepción no controlada durante la simulación del ciclo diurno para un jugador",
                                    exc
                                );
                            }
                        }

                        // Aplicar el tiempo atmosférico particular si es necesario, y si
                        // el proveedor de tiempo atmosférico usado va sobrado de cálculos disponibles
                        if (climaSimulado && maximosCalculosClimaDia >= umbralCalculos) {
                            actualizarTiempoAtmosferico(
                                clima, latitudJugador, longitudJugador, maximosCalculosClimaDia,
                                (TiempoAtmosferico t) -> {
                                    t.aplicarAJugador(p);
                                }
                            );
                        }
                    }

                    // No reutilizar los resultados de la caché para otros mundos
                    // (los datos solo son relevantes para este mundo)
                    cacheTiemposCalculados.invalidateAll();
                }
            }

            ultimoMomentoSimulado = ahora;
        }

        /**
         * Actualiza el tiempo atmosférico visible para un objeto, ejecutando la
         * acción especificada con él como parámetro.
         *
         * @param clima                   El clima del mundo relacionado, que se
         *                                asume no nulo.
         * @param latitud                 La latitud del lugar del que obtener
         *                                el tiempo atmosférico.
         * @param longitud                La longitud del lugar del que obtener
         *                                el tiempo atmosférico.
         * @param maximosCalculosClimaDia El número máximo de cálculos de tiempo
         *                                atmosférico permitidos para el clima
         *                                especificado.
         * @param accion                  La acción a ejecutar para aplicar el
         *                                clima especificado al objeto que se
         *                                desee.
         */
        private void actualizarTiempoAtmosferico(
            Clima clima, double latitud, double longitud, float maximosCalculosClimaDia, Consumer<TiempoAtmosferico> accion
        ) {
            long msDesdeUltimoCalculoClima = ultimoCalculoClima.containsKey(clima) ?
                System.currentTimeMillis() - ultimoCalculoClima.get(clima) :
                Long.MAX_VALUE;

            long msIntervaloCalculoClima = (long) Math.ceil(86400000 / maximosCalculosClimaDia);

            if (msDesdeUltimoCalculoClima >= msIntervaloCalculoClima) {
                try {
                    if (clima.esBloqueante()) {
                        clima.calcularTiempoAtmosfericoActual(latitud, longitud, (TiempoAtmosferico t) -> {
                            accion.accept(t);
                        });
                    } else {
                        accion.accept(clima.calcularTiempoAtmosfericoActual(latitud, longitud));
                    }
                } catch (MeteorologiaDesconocidaException exc) {
                    getLogger().log(
                        Level.WARNING,
                        "Ha ocurrido un error al calcular el tiempo atmosférico de un mundo",
                        exc
                    );
                } finally {
                    ultimoCalculoClima.put(clima, System.currentTimeMillis());
                }
            }
        }
    }

    /**
     * Ayuda a implementar el patrón singleton de inicialización retardada al uso de
     * la instancia, de forma segura entre hilos y eficiente.
     *
     * @author AlexTMjugador
     */
    private static final class PoseedorInstanciaClase {
        private static final SimuladorTiempo INSTANCIA = new SimuladorTiempo();
    }
}