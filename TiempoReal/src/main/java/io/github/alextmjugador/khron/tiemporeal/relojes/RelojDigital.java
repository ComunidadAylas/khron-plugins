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

import static org.bukkit.Bukkit.getServer;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.alextmjugador.khron.tiemporeal.SimuladorTiempo;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Representa un reloj digital, cuyo display muestra la hora a quien lo empuña
 * con precisión de segundos, el tiempo atmosférico y la temperatura. También
 * reproduce un aviso sonoro cada vez que pasa una hora.
 *
 * @author AlexTMjugador
 */
public final class RelojDigital extends RelojItem<Void> {
    /**
     * La fuente proporcionada por un paquete de recursos que contiene los iconos
     * usados por este reloj.
     */
    private static final String FUENTE_ICONOS = "khron:iconos_plugins";

    /**
     * El sonido que se reproducirá cuando el reloj avance una hora mientras está
     * siendo empuñado por un jugador.
     */
    private static final String SONIDO_HORA = "custom.reloj_digital_pitidos";

    /**
     * El identificador del modelo personalizado del reloj.
     */
    private static final int ID_MODELO = 1;

    /**
     * La hora del día en la que está cada mundo.
     */
    private final Map<World, Byte> ultimaHoraMundo = new HashMap<>(
        (int) (getServer().getWorlds().size() / 0.75)
    );

    /**
     * Restringe la instanciación de esta clase a otras clases.
     */
    private RelojDigital() {}

    /**
     * Obtiene la única instancia del reloj digital completo en la JVM, creándola si
     * no lo ha sido ya.
     *
     * @return La devandicha instancia.
     */
    public static RelojDigital get() {
        RelojDigital instancia = PoseedorInstanciaClase.INSTANCIA;
        instancia.inicializar();
        return instancia;
    }

    /**
     * Elimina un mundo que se va a descargar de las estructuras de datos de esta
     * clase.
     *
     * @param event El evento con la información del mundo que se va a descargar.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        ultimaHoraMundo.remove(event.getWorld());
    }

    @Override
    protected boolean lePermiteStackVerReloj(Player jugador, ItemStack stack) {
        return Material.CLOCK.equals(stack.getType()) &&
            stack.hasItemMeta() &&
            stack.getItemMeta().hasCustomModelData() &&
            stack.getItemMeta().getCustomModelData() == ID_MODELO;
    }

    @Override
	protected BaseComponent formatearDisplay(ZonedDateTime fechaHora, Player jugador, World mundo, boolean mundoConCicloDiaNoche) {
        TextComponent display = new TextComponent();
        TextComponent separadorDigitos = new TextComponent(":");
        int hora = fechaHora.getHour();
        TextComponent componenteHora = new TextComponent(String.format("%02d", hora));
        TextComponent componenteMinuto = new TextComponent(String.format("%02d", fechaHora.getMinute()));
        int segundo = fechaHora.getSecond();
        TextComponent componenteSegundo = new TextComponent(String.format("%02d", segundo));
        TextComponent espacio = new TextComponent(" ");
        Location posicionJugador = jugador.getLocation();

        if (mundoConCicloDiaNoche && segundo % 2 != 0) {
            separadorDigitos.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
        }

        display.addExtra(componenteHora);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteMinuto);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteSegundo);

        if (mundoConCicloDiaNoche) {
            double temperaturaBioma = mundo.getTemperature(
                posicionJugador.getBlockX(), posicionJugador.getBlockY(), posicionJugador.getBlockZ()
            );

            TextComponent iconoTemperatura = new TextComponent("\u2063");
            iconoTemperatura.setFont(FUENTE_ICONOS);
            iconoTemperatura.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            iconoTemperatura.setBold(false);
            iconoTemperatura.setItalic(false);
            iconoTemperatura.setStrikethrough(false);
            iconoTemperatura.setObfuscated(false);

            // Mostrar la temperatura
            display.addExtra(espacio);
            display.addExtra(iconoTemperatura);
            display.addExtra(espacio);
            display.addExtra(new TextComponent(String.format(
                "%.1fºC", SimuladorTiempo.get().getTemperatura(jugador))
            ));

            // Mostrar tiempo atmosférico
            display.addExtra(espacio);

            String caracterIcono;
            if (mundo.isThundering() && temperaturaBioma < 0.95) {
                // Tormenta
                caracterIcono = "\u2002";
            } else if (mundo.hasStorm() && temperaturaBioma >= 0.15 && temperaturaBioma < 0.95) {
                // Lluvia
                caracterIcono = "\u2000";
            } else if (mundo.hasStorm() && temperaturaBioma < 0.15) {
                // Nieve
                caracterIcono = "\u00A0";
            } else if ((mundo.hasStorm() || mundo.isThundering()) && temperaturaBioma >= 0.95) {
                // Nublado
                caracterIcono = "\u2003";
            } else {
                // Soleado
                caracterIcono = "\u2001";
            }

            TextComponent iconoTiempo = new TextComponent(caracterIcono);
            iconoTiempo.setFont(FUENTE_ICONOS);
            iconoTiempo.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            iconoTiempo.setBold(false);
            iconoTiempo.setItalic(false);
            iconoTiempo.setStrikethrough(false);
            iconoTiempo.setObfuscated(false);

            display.addExtra(iconoTiempo);
        } else {
            // Reproducir los pitidos del reloj mucho más rápidamente, para
            // dar la impresión de que algo está roto
            jugador.playSound(
                posicionJugador, SONIDO_HORA, SoundCategory.MASTER, 0.5f, 1
            );

            componenteHora.setObfuscated(true);
            componenteMinuto.setObfuscated(true);
            componenteSegundo.setObfuscated(true);
        }

        return display;
    }

    @Override
    protected boolean debeJugadorRecibirActualizaciones(Player jugador, boolean mundoConCicloDiaNoche) {
        boolean toret;

        if (mundoConCicloDiaNoche) {
            ItemStack stackRelojDigital = new ItemStack(Material.CLOCK);
            ItemMeta meta = getServer().getItemFactory().getItemMeta(Material.CLOCK);

            meta.setCustomModelData(ID_MODELO);
            stackRelojDigital.setItemMeta(meta);

            // Enviar actualizaciones a jugadores que tengan al menos un reloj digital en su inventario
            // y estén en un mundo con ciclo día-noche
            toret = jugador.getInventory().containsAtLeast(stackRelojDigital, 1);
        } else {
            toret = false;
        }

        return toret;
    }

    @Override
    protected void onActualizacionReloj(ZonedDateTime fechaHora, Player jugador, World mundo, boolean mundoConCicloDiaNoche) {
        byte hora = (byte) fechaHora.getHour();
        Byte ultimaHora = ultimaHoraMundo.get(mundo);

        if (ultimaHora != null && hora != ultimaHora) {
            Location posicionJugador = jugador.getLocation();

            jugador.playSound(
                posicionJugador, SONIDO_HORA, SoundCategory.MASTER, 1, 1
            );

            // Queremos que otros jugadores escuchen el reloj, pero en una
            // categoría de sonido diferente
            for (Player jugadorCercano : mundo.getNearbyPlayers(posicionJugador, 16)) {
                if (!jugador.equals(jugadorCercano)) {
                    jugadorCercano.playSound(
                        posicionJugador, SONIDO_HORA, SoundCategory.PLAYERS, 1, 1
                    );
                }
            }
        }

        if (ultimaHora == null || hora != ultimaHora) {
            ultimaHoraMundo.put(mundo, hora);
        }
    }

    /**
     * Ayuda a implementar el patrón singleton de inicialización retardada al uso de
     * la instancia, de forma segura entre hilos y eficiente.
     *
     * @author AlexTMjugador
     */
    private static final class PoseedorInstanciaClase {
        private static final RelojDigital INSTANCIA = new RelojDigital();
    }
}