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
import java.time.format.TextStyle;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
public final class RelojDigital extends RelojItem<Byte> {
    /**
     * La fuente proporcionada por un paquete de recursos que contiene los iconos
     * usados por este reloj.
     */
    private static final String FUENTE_ICONOS = "khron:iconos_plugins";

    /**
     * El sonido que se reproducirá cuando el reloj avance una hora mientras está
     * siendo empuñado por un jugador.
     */
    private static final String SONIDO_HORA = "khron.misc.reloj_digital_pitidos";

    /**
     * El identificador del modelo personalizado del reloj.
     */
    private static final int ID_MODELO = 1;

    /**
     * Localización de Java que representa el idioma español.
     */
    private static final Locale LOCALIZACION_ESP = new Locale("es");

    /**
     * Espacio que se puede mostrar en el display.
     */
    private static final TextComponent ESPACIO;
    /**
     * Icono de fecha que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_FECHA;
    /**
     * Icono de temperatura que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_TEMPERATURA;
    /**
     * Icono de tormenta que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_TORMENTA;
    /**
     * Icono de lluvia que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_LLUVIA;
    /**
     * Icono de nieve que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_NIEVE;
    /**
     * Icono de nublado que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_NUBLADO;
    /**
     * Icono de soleado que se puede mostrar en el display.
     */
    private static final TextComponent ICONO_SOLEADO;

    /**
     * Atributo que almacena temporalmente la última posición obtenida, con la
     * finalidad de reducir el número de objetos creados por segundo y ejercer menos
     * presión sobre el colector de basura. Se asume que no se ejecutan de manera
     * concurrente los métodos que usan este atributo.
     */
    private final Location ultimaPosicionTemp = new Location(null, 0, 0, 0);

    static {
        // Crear un componente de texto con un espacio
        ESPACIO = new TextComponent(" ");

        // Crear los iconos que vamos a usar
        ICONO_FECHA = new TextComponent("\u2004");
        ICONO_TEMPERATURA = new TextComponent("\u2063");
        ICONO_TORMENTA = new TextComponent("\u2002");
        ICONO_LLUVIA = new TextComponent("\u2000");
        ICONO_NIEVE = new TextComponent("\u00A0");
        ICONO_NUBLADO = new TextComponent("\u2003");
        ICONO_SOLEADO = new TextComponent("\u2001");

        for (TextComponent icono : new TextComponent[] {
            ICONO_FECHA, ICONO_TEMPERATURA, ICONO_TORMENTA, ICONO_LLUVIA,
            ICONO_NIEVE, ICONO_NUBLADO, ICONO_SOLEADO
        }) {
            icono.setFont(FUENTE_ICONOS);
            icono.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            icono.setBold(false);
            icono.setItalic(false);
            icono.setStrikethrough(false);
            icono.setObfuscated(false);
        }
    }

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

    @Override
    protected boolean lePermiteStackVerReloj(Player jugador, ItemStack stack) {
        ItemMeta metaStack;

        return stack.getType() == Material.CLOCK &&
            stack.hasItemMeta() &&
            (metaStack = stack.getItemMeta()).hasCustomModelData() &&
            metaStack.getCustomModelData() == ID_MODELO;
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

        if (mundoConCicloDiaNoche && segundo % 2 != 0) {
            separadorDigitos.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
        }

        display.addExtra(componenteHora);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteMinuto);
        display.addExtra(separadorDigitos);
        display.addExtra(componenteSegundo);

        jugador.getLocation(ultimaPosicionTemp);

        if (mundoConCicloDiaNoche) {
            TextComponent componenteFecha = new TextComponent(
                String.format(
                    "%s, %02d/%02d/%02d",
                    fechaHora.getDayOfWeek().getDisplayName(TextStyle.NARROW, LOCALIZACION_ESP),
                    fechaHora.getDayOfMonth(),
                    fechaHora.getMonthValue(),
                    fechaHora.getYear() % 100
                )
            );
            double temperaturaBioma = mundo.getTemperature(
                ultimaPosicionTemp.getBlockX(), ultimaPosicionTemp.getBlockY(), ultimaPosicionTemp.getBlockZ()
            );

            // Mostrar la fecha
            display.addExtra(ESPACIO);
            display.addExtra(ICONO_FECHA);
            display.addExtra(ESPACIO);
            display.addExtra(componenteFecha);

            // Mostrar la temperatura
            display.addExtra(ESPACIO);
            display.addExtra(ICONO_TEMPERATURA);
            display.addExtra(ESPACIO);
            display.addExtra(new TextComponent(String.format(
                "%.1fºC", SimuladorTiempo.get().getTemperatura(jugador))
            ));

            // Mostrar tiempo atmosférico
            display.addExtra(ESPACIO);

            TextComponent iconoTiempo;
            if (mundo.isThundering() && temperaturaBioma < 0.95) {
                iconoTiempo = ICONO_TORMENTA;
            } else if (mundo.hasStorm() && temperaturaBioma >= 0.15 && temperaturaBioma < 0.95) {
                iconoTiempo = ICONO_LLUVIA;
            } else if (mundo.hasStorm() && temperaturaBioma < 0.15) {
                iconoTiempo = ICONO_NIEVE;
            } else if ((mundo.hasStorm() || mundo.isThundering()) && temperaturaBioma >= 0.95) {
                iconoTiempo = ICONO_NUBLADO;
            } else {
                iconoTiempo = ICONO_SOLEADO;
            }

            display.addExtra(iconoTiempo);
        } else {
            // Reproducir los pitidos del reloj mucho más rápidamente, para
            // dar la impresión de que algo está roto
            jugador.playSound(
                ultimaPosicionTemp, SONIDO_HORA, SoundCategory.MASTER, 0.5f, 1
            );

            componenteHora.setObfuscated(true);
            componenteMinuto.setObfuscated(true);
            componenteSegundo.setObfuscated(true);
        }

        return display;
    }

    @Override
    protected boolean debeJugadorRecibirActualizaciones(Player jugador, boolean mundoConCicloDiaNoche) {
        boolean toret = false;

        // Enviar actualizaciones a jugadores que tengan al menos un reloj digital en su inventario
        // y estén en un mundo con ciclo día-noche.
        // getStorageContents delega en el atributo items de la clase net.minecraft.world.entity.player.Inventory.
        // Actualmente (1.16.3), el stack de la mano secundaria va aparte, y no se incluye en ese atributo
        if (mundoConCicloDiaNoche) {
            PlayerInventory pinv = jugador.getInventory();
            ItemStack[] itemsInventario = pinv.getStorageContents();

            for (int i = 0; i < itemsInventario.length && !toret; ++i) {
                // En ocasiones las posiciones del array pueden ser nulas debido a cómo funciona
                // Minecraft. Ignorarlas a efectos de comparación
                toret = itemsInventario[i] != null && lePermiteStackVerReloj(jugador, itemsInventario[i]);
            }

            toret = toret || lePermiteStackVerReloj(jugador, pinv.getItemInOffHand());
        }

        // En caso de que dejemos de recibir actualizaciones (es decir, no tengamos el reloj en el inventario),
        // eliminar el estado del display. De esta forma, si pasa una hora y volvemos a coger el reloj, no
        // sonará inmediatamente la alarma, y se esperará a la hora siguiente
        if (!toret) {
            setEstadoDisplay(jugador, null);
        }

        return toret;
    }

    @Override
    protected void onActualizacionReloj(ZonedDateTime fechaHora, Player jugador, World mundo, boolean mundoConCicloDiaNoche) {
        byte hora = (byte) fechaHora.getHour();
        Byte ultimaHora = getEstadoDisplay(jugador);

        if (ultimaHora != null && hora != ultimaHora) {
            jugador.getLocation(ultimaPosicionTemp);

            jugador.playSound(
                ultimaPosicionTemp, SONIDO_HORA, SoundCategory.MASTER, 1, 1
            );

            // Queremos que otros jugadores escuchen el reloj, pero en una
            // categoría de sonido diferente
            for (Player jugadorCercano : mundo.getNearbyPlayers(ultimaPosicionTemp, 16)) {
                if (!jugador.equals(jugadorCercano)) {
                    jugadorCercano.playSound(
                        ultimaPosicionTemp, SONIDO_HORA, SoundCategory.PLAYERS, 1, 1
                    );
                }
            }
        }

        setEstadoDisplay(jugador, hora);
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