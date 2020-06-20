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

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;
import io.github.alextmjugador.khron.tiemporeal.SimuladorTiempo;
import io.github.alextmjugador.khron.tiemporeal.configuraciones.TextoRelojDigital;

/**
 * Representa un reloj digital básico, cuyo display muestra la hora a quien lo
 * empuña con precisión de segundos.
 *
 * @author AlexTMjugador
 */
public class RelojDigitalBasico extends Reloj {
    private static final ChatColor[] COLORES_FORMATO = {
        ChatColor.BOLD, ChatColor.ITALIC, ChatColor.UNDERLINE,
        ChatColor.STRIKETHROUGH, ChatColor.MAGIC
    };

    /**
     * Obtiene la única instancia del reloj digital básico en la JVM,
     * creándola si no lo ha sido ya.
     *
     * @return La devandicha instancia.
     */
    public static RelojDigitalBasico get() {
        return PoseedorInstanciaClase.INSTANCIA;
    }

    @Override
    protected boolean lePermiteStackVerReloj(Player jugador, ItemStack stack) {
        return Material.CLOCK.equals(stack.getType()) /*&&
            stack.hasItemMeta() &&
            stack.getItemMeta().hasCustomModelData() &&
            stack.getItemMeta().getCustomModelData() == Integer.MIN_VALUE*/;
    }

    @Override
    protected final String obtenerTextoDisplay(Player jugador, World mundo) {
        PluginTiempoReal plugin = PluginTiempoReal.getPlugin(PluginTiempoReal.class);
        ZonedDateTime hora = SimuladorTiempo.get().getHoraMundo(mundo);

        boolean mundoConCicloDiaNoche = mundo.getEnvironment().equals(Environment.NORMAL);

        String textoReloj = mundoConCicloDiaNoche ?
            plugin.getTextoRelojDigital() :
            plugin.getTextoRelojDigitalDimensionSinCiclo();

        int i = textoReloj.indexOf(TextoRelojDigital.DISPLAY);
        String formatoTexto = ChatColor.getLastColors(textoReloj.substring(0, i));

        return textoReloj.replace(
            TextoRelojDigital.DISPLAY,
            formatearDisplay(hora, formatoTexto, mundo, mundoConCicloDiaNoche)
        );
    }

    /**
     * Formatea el display de un reloj a mostrar al usuario.
     *
     * @param hora                  La hora a usar para formatear. No es nula.
     * @param formatoTexto          Una cadena conteniendo los últimos
     *                              caracteres de formato aplicados al texto
     *                              previo al display. No es nula.
     * @param mundo                 El mundo del que se ha obtenido la hora. No
     *                              es nulo.
     * @param mundoConCicloDiaNoche Verdadero si el mundo tiene una hora válida,
     *                              debido a que tiene un ciclo de día-noche.
     * @return El display formateado, no nulo.
     */
    protected String formatearDisplay(ZonedDateTime hora, String formatoTexto, World mundo, boolean mundoConCicloDiaNoche) {
        int segundo = hora.getSecond();

        String separador;
        if (mundoConCicloDiaNoche && segundo % 2 != 0) {
            String caracteresFormato = "";

            // Después de aplicar un color, el cliente puede dejar de renderizar el texto en negrita
            // o con el otro formato que se use. Para evitar eso, añadimos de nuevo los colores
            // de formato empleados previamente
            for (ChatColor colorFormato : COLORES_FORMATO) {
                if (formatoTexto.contains(colorFormato.toString())) {
                    caracteresFormato += colorFormato.toString();
                }
            }

            separador = ChatColor.DARK_GRAY + caracteresFormato + ":" + ChatColor.RESET + formatoTexto;
        } else {
            separador = ":";
        }

        return String.format(
            "%s%02d%s%02d%s%02d", mundoConCicloDiaNoche ? "" : ChatColor.MAGIC,
            hora.getHour(), separador, hora.getMinute(), separador, segundo
        );
    }

    /**
     * Ayuda a implementar el patrón singleton de inicialización retardada al uso de
     * la instancia, de forma segura entre hilos y eficiente.
     *
     * @author AlexTMjugador
     */
    private static final class PoseedorInstanciaClase {
        private static final RelojDigitalBasico INSTANCIA = new RelojDigitalBasico();
    }
}