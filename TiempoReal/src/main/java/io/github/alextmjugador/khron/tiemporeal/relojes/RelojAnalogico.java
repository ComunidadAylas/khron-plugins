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
import java.time.temporal.ChronoField;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Representa un reloj analógico, cuyo display muestra la hora a quien lo empuña
 * con precisión de segundos, pero con granuralidad de 2 para los minutos y
 * segundos.
 *
 * @author AlexTMjugador
 */
public final class RelojAnalogico extends Reloj {
    /**
     * Obtiene la única instancia del reloj digital completo en la JVM,
     * creándola si no lo ha sido ya.
     *
     * @return La devandicha instancia.
     */
    public static RelojAnalogico get() {
        return PoseedorInstanciaClase.INSTANCIA;
    }

    @Override
    protected boolean lePermiteStackVerReloj(Player jugador, ItemStack stack) {
        return Material.CLOCK.equals(stack.getType()) &&
            stack.hasItemMeta() &&
            stack.getItemMeta().hasCustomModelData() &&
            stack.getItemMeta().getCustomModelData() == 2;
    }

    @Override
    protected BaseComponent formatearDisplay(ZonedDateTime hora, Player jugador, World mundo, boolean mundoConCicloDiaNoche) {
        TextComponent display = new TextComponent();
        TextComponent separadorDigitos = new TextComponent(":");
        TextComponent componenteHora = new TextComponent(String.format("%02d", hora.get(ChronoField.CLOCK_HOUR_OF_AMPM)));
        TextComponent componenteMinuto = new TextComponent(
            String.format("%02d", Math.round(Math.max(hora.getMinute() / 2.0 - 0.5, 0)) * 2)
        );
        TextComponent componenteSegundo = new TextComponent(
            String.format("%02d", Math.round(Math.max(hora.getSecond() / 2.0 - 0.5, 0)) * 2)
        );

        display.addExtra(componenteHora);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteMinuto);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteSegundo);

        if (!mundoConCicloDiaNoche) {
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
        private static final RelojAnalogico INSTANCIA = new RelojAnalogico();
    }
}