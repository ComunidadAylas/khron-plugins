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

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;
import io.github.alextmjugador.khron.tiemporeal.SimuladorTiempo;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.TextoReloj;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Modela un reloj, que muestra información de tiempo a quienes lo observan.
 *
 * @param <T> El tipo de dato del estado del reloj que se puede asociar a un
 *            jugador. Puede ser {@link Void} si no se desea asociar estado.
 * @author AlexTMjugador
 */
public abstract class Reloj<T> implements Listener {
    /**
     * El número de ticks que han de pasar entre ejecuciones consecutivas de la
     * tarea que se encarga de mostrar el display de tiempo a los jugadores
     * interesados y ejecutar el método de actualización periódica de los relojes.
     * Este valor es el mayor que permite mostrar un display de hora actualizado,
     * aunque no se sincronice la hora en el mundo en el que está el jugador,
     * teniendo en cuenta que 1 minuto en Minecraft = 16,6 ticks = 0,83 s, y es lo
     * suficientemente frecuente como para que el método de actualización periódica
     * pueda reaccionar relativamente rápido a lo que desee.
     */
    private static final short TICKS_TAREA_RELOJ = 8;

    /**
     * Alberga las diferentes instancias concretas de relojes creadas.
     */
    private static final Set<Reloj<?>> RELOJES_CREADOS = new LinkedHashSet<>((int) (2 / 0.75));

    /**
     * Tarea que se encarga de mostrar el display a los jugadores interesados, y de
     * ejecutar las actualizaciones periódicas de los relojes.
     */
    private static BukkitTask tareaTickRelojes = null;

    /**
     * Los jugadores que están viendo este reloj en el instante de tiempo presente.
     */
    private final Map<Player, T> jugadoresEstadoDisplay = new LinkedHashMap<>(
        (int) (Math.max(getServer().getMaxPlayers() / 4, 8) / 0.75)
    );

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
     * Detiene la tarea de actualización periódica de todos los relojes. Esto
     * también implica detener la muestra de display de relojes para todos los
     * jugadores.
     */
    public final void detener() {
        for (Reloj<?> reloj : RELOJES_CREADOS) {
            reloj.jugadoresEstadoDisplay.clear();
        }

        RELOJES_CREADOS.clear();

        if (tareaTickRelojes != null) {
            tareaTickRelojes.cancel();
            tareaTickRelojes = null;
        }
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con ver o dejar de ver un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerJoinEvent event) {
        actualizarDisplay(event.getPlayer());
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con ver o dejar de ver un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerQuitEvent event) {
        ocultarDisplay(event.getPlayer());
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con ver o dejar de ver un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerKickEvent event) {
        ocultarDisplay(event.getPlayer());
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
     * Formatea el display de un reloj a mostrar al usuario. Este método se ejecuta
     * justo antes de enviarle al jugador el display actualizado de la hora.
     *
     * @param fechaHora             La hora a usar para formatear. No es nula.
     * @param jugador               El jugador que verá el display. No es nulo.
     * @param mundo                 El mundo del que se ha obtenido la hora. No es
     *                              nulo.
     * @param mundoConCicloDiaNoche Verdadero si el mundo tiene una hora válida,
     *                              debido a que tiene un ciclo de día-noche.
     * @return Un subcomponente de display, que se incorporará en el texto final que
     *         se mostrará.
     */
    protected abstract BaseComponent formatearDisplay(
        ZonedDateTime fechaHora, Player jugador, World mundo, boolean mundoConCicloDiaNoche
    );

    /**
     * Comprueba si un determinado jugador debe de recibir actualizaciones
     * periódicas del reloj, mediante invocaciones del método
     * {@link #onActualizacionReloj(ZonedDateTime, Player, World, boolean)}. La
     * implementación predeterminada de este método siempre devuelve falso.
     *
     * @param jugador               El jugador del que se quiere saber si debe de
     *                              recibir actualizaciones periódicas del reloj.
     * @param mundoConCicloDiaNoche Verdadero si el mundo tiene una hora válida,
     *                              debido a que tiene un ciclo de día-noche.
     * @return Verdadero si el jugador debe de recibir las devandichas
     *         actualizaciones, falso en otro caso.
     */
    protected boolean debeJugadorRecibirActualizaciones(Player jugador, boolean mundoConCicloDiaNoche) {
        return false;
    }

    /**
     * Método ejecutado periódicamente para cada jugador del servidor que reciba
     * actualizaciones del reloj. Las subclases pueden implementarlo de manera
     * diferente cuando deseen ejecutar código en respuesta a esta actualización.
     * Por defecto, ningún jugador recibe actualizaciones periódicas del reloj, y
     * este método no hace nada.
     *
     * @param fechaHora             La hora actual en el mundo en el que está el
     *                              jugador. No es nula.
     * @param jugador               El jugador al que va destinada la actualización
     *                              de hora. NO es nulo.
     * @param mundo                 El mundo en el que está el jugador. No es nulo.
     * @param mundoConCicloDiaNoche Verdadero si el mundo tiene una hora válida,
     *                              debido a que tiene un ciclo de día-noche.
     */
    protected void onActualizacionReloj(
        ZonedDateTime fechaHora, Player jugador, World mundo, boolean mundoConCicloDiaNoche
    ) {}

    /**
     * Método ejecutado cuando se va a ocultar el display. Las subclases pueden
     * implementarlo de manera diferente para realizar acciones cuando se oculta el
     * display. Por defecto, este método no hace nada.
     *
     * @param jugador El jugador al que se le va a ocultar el display. No es nulo.
     */
    protected void onOcultarDisplay(Player jugador) {}

    /**
     * Comprueba si a un jugador le corresponde ver el display en pantalla. Por
     * defecto, la implementación de este método devuelve verdadero si y solo si el
     * jugador está vivo y conectado.
     *
     * @param p El jugador a comprobar.
     * @return Verdadero si le corresponde, falso en otro caso.
     */
    protected boolean leCorrespondeVerDisplay(Player p) {
        return p.isOnline() && !p.isDead();
    }

    /**
     * Prepara el reloj para ejecutar su cometido, lo que incluye poner en marcha la
     * tarea que se encarga de la actualización periódica del reloj y de la muestra
     * del display de hora a los jugadores y registrar la instancia en algunas
     * estructuras de datos.
     *
     * <p>
     * <b>Las subclases deben de llamar a este método antes de devolver una
     * instancia de ellas</b>, para garantizar que aquellos que tengan una
     * referencia a la instancia posean un reloj plenamente funcional en todo
     * momento.
     * </p>
     */
    protected final void inicializar() {
        RELOJES_CREADOS.add(this);

        if (tareaTickRelojes == null) {
            tareaTickRelojes = new TickRelojes().runTaskTimer(
                PluginTiempoReal.getPlugin(PluginTiempoReal.class), 0, TICKS_TAREA_RELOJ
            );
        }
    }

    /**
     * Obtiene el estado del display de reloj asociado al jugador especificado, que
     * se borra automáticamente cuando el reloj deja de ser empuñado, y se
     * inicializa a un valor nulo cuando se vuelve a empuñar. Las subclases pueden
     * usar este estado como crean conveniente para mejorar el display u otros
     * propósitos.
     *
     * @param p El jugador del que obtener el estado de reloj asociado.
     * @return Un valor nulo si el estado ha sido recién inicializado o el jugador
     *         no tiene un estado de reloj asociado, o el valor que una subclase
     *         haya decidido almacenar mientras el reloj está empuñado.
     */
    protected final T getEstadoDisplay(Player p) {
        return jugadoresEstadoDisplay.get(p);
    }

    /**
     * Establece el estado del display de reloj asociado al jugador especificado,
     * que se borra automáticamente cuando el reloj deja de ser empuñado, y se
     * inicializa a un valor nulo cuando se vuelve a empuñar. Las subclases pueden
     * usar este estado como crean conveniente para mejorar el display u otros
     * propósitos.
     *
     * @param p      El jugador del que establecer el estado de reloj asociado.
     * @param estado El estado de reloj a asociar con el jugador.
     */
    protected final void setEstadoDisplay(Player p, T estado) {
        jugadoresEstadoDisplay.put(p, estado);
    }

    /**
     * Le muestra u oculta el display de un reloj a un jugador, dependiendo de si le
     * corresponde verlo o no.
     *
     * @param p El jugador cuyo estado de muestra de display actualizar.
     */
    protected final void actualizarDisplay(Player p) {
        if (leCorrespondeVerDisplay(p)) {
            mostrarDisplay(p);
        } else {
            ocultarDisplay(p);
        }
    }

    /**
     * Añade un jugador a la estructura de datos de jugadores con aquellos a los que
     * mostrar el display en pantalla.
     *
     * @param p El jugador a añadir.
     */
    private void mostrarDisplay(Player p) {
        if (p != null && !jugadoresEstadoDisplay.containsKey(p)) {
            jugadoresEstadoDisplay.put(p, null);
        }
    }

    /**
     * Elimina un jugador de la estructura de datos con jugadores a los que mostrar
     * el display del reloj en pantalla.
     *
     * @param p El jugador a eliminar de la lista.
     */
    private void ocultarDisplay(Player p) {
        if (jugadoresEstadoDisplay.containsKey(p)) {
            onOcultarDisplay(p);
            jugadoresEstadoDisplay.remove(p);
        }
    }

    /**
     * Tarea periódica que se encarga de mostrar y mantener actualizado el display
     * de la hora en las pantallas de los jugadores, además de ejecutar
     * periódicamente el método de actualización de los relojes.
     *
     * @author AlexTMjugador
     */
    private static final class TickRelojes extends BukkitRunnable {
        /**
         * Ejecuta el mostrado y actualización del display de la hora en las pantallas
         * de los jugadores, además del método de actualización de los relojes.
         */
        @Override
        public void run() {
            for (Player p : getServer().getOnlinePlayers()) {
                World mundo = p.getWorld();
                ZonedDateTime hora = SimuladorTiempo.get().getHoraMundo(mundo);
                boolean mundoConCicloDiaNoche = mundo.getEnvironment() == Environment.NORMAL;

                for (Reloj<?> r : RELOJES_CREADOS) {
                    // Primero ejecutar la actualización del reloj si es necesario
                    if (r.debeJugadorRecibirActualizaciones(p, mundoConCicloDiaNoche)) {
                        r.onActualizacionReloj(hora, p, mundo, mundoConCicloDiaNoche);
                    }

                    // Ver si el jugador es candidato a que se le muestre el display
                    if (r.jugadoresEstadoDisplay.containsKey(p)) {
                        if (r.leCorrespondeVerDisplay(p)) {
                            // Le corresponde ver un display de hora al jugador; mostrárselo
                            PluginTiempoReal plugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);
                            String textoReloj = mundoConCicloDiaNoche ? plugin.getTextoReloj()
                                : plugin.getTextoRelojDimensionSinCiclo();

                            // Buscar el componente de texto que contiene la palabra clave a sustituir por
                            // el display. Siempre existe uno y solo uno
                            BaseComponent[] componentesTextoReloj = TextComponent.fromLegacyText(textoReloj);
                            TextComponent componenteTextoDisplay = null;
                            for (int i = 0; i < componentesTextoReloj.length && componenteTextoDisplay == null; ++i) {
                                BaseComponent componente = componentesTextoReloj[i];
                                if (componente instanceof TextComponent) {
                                    TextComponent componenteTexto = (TextComponent) componente;

                                    if (componenteTexto.getText().contains(TextoReloj.DISPLAY)) {
                                        componenteTextoDisplay = componenteTexto;
                                    }
                                }
                            }

                            BaseComponent display = r.formatearDisplay(hora, p, mundo, mundoConCicloDiaNoche);

                            // Reemplazar la palabra clave por el componente generado
                            componenteTextoDisplay.setText(componenteTextoDisplay.getText().replace(TextoReloj.DISPLAY, ""));
                            componenteTextoDisplay.addExtra(display);

                            // Mostrar los componentes de texto finales, con el componente correspondiente
                            // al display ya modificado
                            p.sendActionBar(componentesTextoReloj);
                        } else {
                            // No le corresponde ver un display de hora (esto puede ocurrir tras un /clear
                            // u otros eventos que no podemos o no es factible manejar). No debería de estar
                            // en este mapa
                            r.jugadoresEstadoDisplay.remove(p);
                        }
                    }
                }
            }
        }
    }
}