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
package org.aylas.khron.tiemporeal.relojes;

import static org.bukkit.Bukkit.getServer;

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.aylas.khron.tiemporeal.PluginTiempoReal;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import org.aylas.khron.tiemporeal.SimuladorTiempo;
import org.aylas.khron.tiemporeal.configuraciones.TextoReloj;
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
     * Los componentes de texto del texto de reloj.
     */
    private static BaseComponent[] componentesTextoReloj = null;
    /**
     * Los componentes de texto del texto de reloj para dimensiones sin ciclo
     * día-noche.
     */
    private static BaseComponent[] componentesTextoRelojDimensionSinCiclo = null;
    /**
     * El índice del componente de texto donde colocar el display del reloj.
     */
    private static int indiceComponenteTextoDisplay = Integer.MIN_VALUE;
    /**
     * El índice del componente de texto donde colocar el display del reloj para
     * dimensiones sin ciclo día-noche.
     */
    private static int indiceComponenteTextoDisplayDimensionSinCiclo = Integer.MIN_VALUE;

    /**
     * Relaciona cada jugador con su estado del display de este reloj, y si se
     * encuentran viendo el display o no.
     */
    private final Map<Player, InformacionDisplay<T>> jugadoresEstadoDisplay = new LinkedHashMap<>(
        (int) (Math.max(getServer().getMaxPlayers() / 4, 8) / 0.75)
    );

    /**
     * Tarea que se encarga de mostrar el display a los jugadores interesados, y de
     * ejecutar las actualizaciones periódicas de los relojes.
     */
    private BukkitTask tareaTickRelojes = null;

    /**
     * Crea un nuevo reloj observable por un jugador.
     */
    protected Reloj() {
        if (componentesTextoReloj == null) {
            PluginTiempoReal plugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);

            componentesTextoReloj = TextComponent.fromLegacyText(plugin.getTextoReloj());
            componentesTextoRelojDimensionSinCiclo = TextComponent.fromLegacyText(
                plugin.getTextoRelojDimensionSinCiclo()
            );

            indiceComponenteTextoDisplay = indiceComponenteTextoDisplay(componentesTextoReloj);
            indiceComponenteTextoDisplayDimensionSinCiclo = indiceComponenteTextoDisplay(
                componentesTextoRelojDimensionSinCiclo
            );
        }
    }

    /**
     * Actualiza los componentes de texto del reloj con el nuevo valor del parámetro
     * de configuración correspondiente.
     *
     * @param nuevoTextoReloj El nuevo valor del parámetro de configuración. Se
     *                        asume que es válido.
     */
    public static void actualizarComponentesTextoReloj(String nuevoTextoReloj) {
        componentesTextoReloj = TextComponent.fromLegacyText(nuevoTextoReloj);
        indiceComponenteTextoDisplay = indiceComponenteTextoDisplay(componentesTextoReloj);
    }

    /**
     * Actualiza los componentes de texto del reloj en dimensiones sin ciclo
     * día-noche con el nuevo valor del parámetro de configuración correspondiente.
     *
     * @param nuevoTextoReloj El nuevo valor del parámetro de configuración. Se
     *                        asume que es válido.
     */
    public static void actualizarComponentesTextoRelojDimensionSinCiclo(String nuevoTextoReloj) {
        componentesTextoRelojDimensionSinCiclo = TextComponent.fromLegacyText(nuevoTextoReloj);
        indiceComponenteTextoDisplayDimensionSinCiclo = indiceComponenteTextoDisplay(
            componentesTextoRelojDimensionSinCiclo
        );
    }

    /**
     * Obtiene el índice del componente de texto con la palabra clave a sustituir
     * por el display de entre los componentes de texto de reloj pasados como
     * parámetro.
     *
     * @param componentesTextoReloj Los componentes de texto del texto de reloj de
     *                              los que buscar el componente de texto descrito.
     *                              Se asume que son válidos.
     * @return El índice del componente de texto con la palabra clave a sustituir
     *         por el display eliminada.
     */
    private static int indiceComponenteTextoDisplay(BaseComponent[] componentesTextoReloj) {
        int i = 0;
        boolean componenteNoEncontrado = true;

        // Buscar el componente de texto que contiene la palabra clave a sustituir por
        // el display. Siempre existe uno y solo uno
        while (i < componentesTextoReloj.length && componenteNoEncontrado) {
            BaseComponent componente = componentesTextoReloj[i++];

            if (componente instanceof TextComponent) {
                TextComponent componenteTexto = (TextComponent) componente;

                if (componenteTexto.getText().contains(TextoReloj.DISPLAY)) {
                    componenteTexto.setText(componenteTexto.getText().replace(TextoReloj.DISPLAY, ""));
                    componenteNoEncontrado = false;
                    --i;
                }
            }
        }

        return i;
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
    public final void onPlayerEvent(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        ocultarDisplay(p);
        jugadoresEstadoDisplay.remove(p);
    }

    /**
     * Decide si mostrar u ocultar el display a jugadores que realicen algún evento
     * relacionado con ver o dejar de ver un reloj.
     *
     * @param event El evento realizado por el jugador.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public final void onPlayerEvent(PlayerKickEvent event) {
        Player p = event.getPlayer();
        ocultarDisplay(p);
        jugadoresEstadoDisplay.remove(p);
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
     * Por defecto, este método no hace nada. Este método se llama tras comprobar
     * que un jugador debe de recibir actualizaciones de reloj mediante
     * {@link #debeJugadorRecibirActualizaciones(Player, boolean)}, solo si éste
     * devuelve verdadero.
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
     * display. Por defecto, este método no hace nada. Este método se invoca tras
     * registrar el jugador como no viendo el display.
     *
     * @param p El jugador al que se le va a ocultar el display. No es nulo.
     */
    protected void onOcultarDisplay(Player p) {}

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
     * Obtiene el estado del display de reloj asociado al jugador especificado,. Las
     * subclases pueden usar este estado como crean conveniente para mejorar el
     * display u otros propósitos.
     *
     * @param p El jugador del que obtener el estado de reloj asociado.
     * @return Un valor nulo si el estado ha sido recién inicializado o el jugador
     *         no tiene un estado de reloj asociado, o el valor que una subclase
     *         haya decidido almacenar.
     */
    protected final T getEstadoDisplay(Player p) {
        InformacionDisplay<T> infoDisplay = jugadoresEstadoDisplay.get(p);
        return infoDisplay != null ? infoDisplay.getEstado() : null;
    }

    /**
     * Establece el estado del display de reloj asociado al jugador especificado.
     * Las subclases pueden usar este estado como crean conveniente para mejorar el
     * display u otros propósitos.
     *
     * @param p      El jugador del que establecer el estado de reloj asociado.
     * @param estado El estado de reloj a asociar con el jugador.
     */
    protected final void setEstadoDisplay(Player p, T estado) {
        jugadoresEstadoDisplay.compute(p, (clave, infoDisplay) -> {
            InformacionDisplay<T> toret;

            if (infoDisplay == null && estado != null) {
                toret = new InformacionDisplay<>(false, estado);
            } else if ((infoDisplay == null || !infoDisplay.mostrandoDisplay()) && estado == null) {
                // Eliminar la entrada del mapa si se va a guardar un estado nulo
                // y no se está mostrando el display
                toret = null;
            } else {
                // infoDisplay no es nulo, y estamos mostrando el display.
                // Cambiar el estado en la entrada existente
                infoDisplay.setEstado(estado);
                toret = infoDisplay;
            }

            return toret;
        });
    }

    /**
     * Le oculta el display a un jugador si ya no debería de verlo.
     *
     * @param p El jugador cuyo estado de muestra de display actualizar.
     */
    protected final void actualizarDisplay(Player p) {
        if (!leCorrespondeVerDisplay(p)) {
            ocultarDisplay(p);
        }
    }

    /**
     * Realiza las operaciones necesarias para ocultarle el display del reloj a un
     * jugador.
     *
     * @param p El jugador al que ocultarle el display.
     */
    private void ocultarDisplay(Player p) {
        InformacionDisplay<T> infoDisplay = jugadoresEstadoDisplay.get(p);

        if (infoDisplay != null && infoDisplay.mostrandoDisplay()) {
            infoDisplay.setMostrandoDisplay(false);
            onOcultarDisplay(p);
        }
    }

    /**
     * Tarea periódica que se encarga de mostrar y mantener actualizado el display
     * de la hora en las pantallas de los jugadores, además de ejecutar
     * periódicamente el método de actualización de los relojes.
     *
     * @author AlexTMjugador
     */
    private final class TickRelojes extends BukkitRunnable {
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
                    if (r.leCorrespondeVerDisplay(p)) {
                        // Cambiar la información del display para indicar que lo
                        // estamos mostrando
                        jugadoresEstadoDisplay.compute(p, (clave, infoDisplay) -> {
                            InformacionDisplay<T> toret;

                            if (infoDisplay == null) {
                                toret = new InformacionDisplay<>(true, null);
                            } else {
                                infoDisplay.setMostrandoDisplay(true);
                                toret = infoDisplay;
                            }

                            return toret;
                        });

                        // Generar el texto a mostrar
                        BaseComponent[] componentesTextoRelojPlantilla = mundoConCicloDiaNoche ?
                            Reloj.componentesTextoReloj :
                            Reloj.componentesTextoRelojDimensionSinCiclo;
                        int indiceComponenteTextoDisplay = mundoConCicloDiaNoche ?
                            Reloj.indiceComponenteTextoDisplay :
                            Reloj.indiceComponenteTextoDisplayDimensionSinCiclo;

                        // Duplicamos los componentes de la plantilla a otro array debido
                        // a necesidades de la implementación de sendActionBar, para que cada
                        // invocación se haga con objetos específicos y diferentes
                        BaseComponent[] componentesTextoReloj = new BaseComponent[componentesTextoRelojPlantilla.length];
                        for (int i = 0; i < componentesTextoReloj.length; ++i) {
                            componentesTextoReloj[i] = componentesTextoRelojPlantilla[i].duplicate();
                        }

                        // Añadir el componente generado al componente de display
                        // (como hemos borrado la palabra clave, y la palabra clave está al final,
                        // la reemplazará)
                        componentesTextoReloj[indiceComponenteTextoDisplay].addExtra(
                            r.formatearDisplay(hora, p, mundo, mundoConCicloDiaNoche)
                        );

                        // Mostrar los componentes de texto finales, con el componente correspondiente
                        // al display ya modificado
                        p.sendActionBar(componentesTextoReloj);
                    } else {
                        // No le corresponde ver un display de hora (esto puede ocurrir tras un /clear
                        // u otros eventos que no podemos o no es factible manejar)
                        r.ocultarDisplay(p);
                    }
                }
            }
        }
    }

    /**
     * Contiene la información del display de reloj para un jugador, incluyendo su
     * estado y si se está mostrando o no.
     *
     * @author AlexTMjugador
     *
     * @param <T> El tipo de dato del estado del reloj.
     */
    private static final class InformacionDisplay<T> {
        private boolean mostrandoDisplay;
        private T estado;

        /**
         * Crea una nueva información de display.
         *
         * @param mostrandoDisplay Verdadero si se está mostrando el display, falso en
         *                         caso contrario.
         * @param estado           El estado del display, gestionado por las subclases y
         *                         específico a cada jugador.
         */
        public InformacionDisplay(boolean mostrandoDisplay, T estado) {
            this.mostrandoDisplay = mostrandoDisplay;
            this.estado = estado;
        }

        /**
         * Comprueba si el display se está mostrando.
         *
         * @return Verdadero si se está mostrando, falso en caso contrario.
         */
        public boolean mostrandoDisplay() {
            return mostrandoDisplay;
        }

        /**
         * Establece si el display se está mostrando.
         *
         * @param mostrandoDisplay Verdadero si se está mostrando, falso en caso
         *                         contrario.
         */
        public void setMostrandoDisplay(boolean mostrandoDisplay) {
            this.mostrandoDisplay = mostrandoDisplay;
        }

        /**
         * Obtiene el estado del display, gestionado por las subclases y específico a
         * cada jugador.
         *
         * @return El estado del display.
         */
        public T getEstado() {
            return estado;
        }

        /**
         * Establece el estado del display, gestionado por las subclases y específico a
         * cada jugador.
         *
         * @param estado El estado del display
         */
        public void setEstado(T estado) {
            this.estado = estado;
        }
    }
}