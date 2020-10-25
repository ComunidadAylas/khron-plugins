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

import java.time.ZonedDateTime;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.alextmjugador.khron.tiemporeal.SimuladorTiempo;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Representa un reloj digital completo, cuyo display muestra la hora a quien lo
 * empuña con precisión de segundos, el tiempo atmosférico y la temperatura.
 *
 * @author AlexTMjugador
 */
public final class RelojDigital extends Reloj<Byte> {
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
     * Obtiene la única instancia del reloj digital completo en la JVM, creándola si
     * no lo ha sido ya.
     *
     * @return La devandicha instancia.
     */
    public static RelojDigital get() {
        return PoseedorInstanciaClase.INSTANCIA;
    }

    @Override
    protected boolean lePermiteStackVerReloj(Player jugador, ItemStack stack) {
        return Material.CLOCK.equals(stack.getType()) &&
            stack.hasItemMeta() &&
            stack.getItemMeta().hasCustomModelData() &&
            stack.getItemMeta().getCustomModelData() == 1;
    }

    @Override
	protected BaseComponent formatearDisplay(ZonedDateTime hora, Player jugador, World mundo, boolean mundoConCicloDiaNoche) {
        TextComponent display = new TextComponent();
        TextComponent separadorDigitos = new TextComponent(":");
        int numeroHora = hora.getHour();
        TextComponent componenteHora = new TextComponent(String.format("%02d", numeroHora));
        TextComponent componenteMinuto = new TextComponent(String.format("%02d", hora.getMinute()));
        int segundo = hora.getSecond();
        TextComponent componenteSegundo = new TextComponent(String.format("%02d", segundo));
        TextComponent espacio = new TextComponent(" ");

        if (mundoConCicloDiaNoche && segundo % 2 != 0) {
            separadorDigitos.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
        }

        display.addExtra(componenteHora);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteMinuto);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteSegundo);

        if (mundoConCicloDiaNoche) {
            Location posicionJugador = jugador.getLocation();

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

            Byte ultimaHoraVista = getEstadoReloj(jugador);
            if (ultimaHoraVista != null && ultimaHoraVista != numeroHora) {
                jugador.playSound(
                    posicionJugador, SONIDO_HORA, SoundCategory.MASTER, 1, 1
                );

                // Queremos que otros jugadores escuchen el reloj, pero en una
                // categoría de sonido diferente
                for (Player jugadorCercano : mundo.getNearbyPlayers(posicionJugador, 16)) {
                    if (!jugadorCercano.equals(jugador)) {
                        jugadorCercano.playSound(
                            posicionJugador, SONIDO_HORA, SoundCategory.PLAYERS, 1, 1
                        );
                    }
                }
            }

            if (ultimaHoraVista == null || ultimaHoraVista != numeroHora) {
                setEstadoReloj(jugador, (byte) numeroHora);
            }
        } else {
            componenteHora.setObfuscated(true);
            componenteMinuto.setObfuscated(true);
            componenteSegundo.setObfuscated(true);
        }

        return display;
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