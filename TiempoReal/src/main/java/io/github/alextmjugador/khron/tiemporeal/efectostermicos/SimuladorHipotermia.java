/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2022 Comunidad Aylas
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
package io.github.alextmjugador.khron.tiemporeal.efectostermicos;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.alextmjugador.khron.tiemporeal.PluginTiempoReal;
import io.github.alextmjugador.khron.tiemporeal.SimuladorTiempo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static org.bukkit.Bukkit.getServer;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Simula la ocurrencia de hipotermia para jugadores que estén en un entorno muy
 * frío, teniendo en cuenta la ropa que lleven y la temperatura ambiente.
 *
 * @author AlexTMjugador
 */
public final class SimuladorHipotermia extends BukkitRunnable implements Listener {
	/**
	 * La frecuencia con la que se actualizará el estado de hipotermia de los
	 * jugadores, en ticks.
	 */
	private static final int FRECUENCIA_SIMULACION = 20;
	/**
	 * La temperatura máxima base que soporta un jugador sin armadura. En entornos
	 * más fríos que la temperatura soportada por el jugador, éste padecerá
	 * hipotermia. En entornos más cálidos que tal temperatura, éste no padecerá
	 * hipotermia, y se cancelarán los efectos de la hipotermia.
	 */
	private static final float TEMPERATURA_MAXIMA_BASE = 10;

	/**
	 * Restringe la instanciación de esta clase a otras clases.
	 */
	private SimuladorHipotermia() {}

	/**
	 * Inicializa el simulador de hipotermia, aplicando efectos de congelación a
	 * jugadores.
	 *
	 * @return El simulador inicializado.
	 */
	public static SimuladorHipotermia inicializar() {
		SimuladorHipotermia instancia = PoseedorInstanciaClase.INSTANCIA;

		instancia.runTaskTimer(PluginTiempoReal.getPlugin(PluginTiempoReal.class), 0, FRECUENCIA_SIMULACION);

		return instancia;
	}

	/**
	 * Detiene el simulador de hipotermia, dejando de aplicar efectos de
	 * congelación a jugadores.
	 */
	public static void detener() {
		SimuladorHipotermia instancia = PoseedorInstanciaClase.INSTANCIA;
		instancia.cancel();

		for (Player p : getServer().getOnlinePlayers()) {
			p.lockFreezeTicks(false);
		}
	}

	@Override
	public void run() {
		Location pos = new Location(null, 0, 0, 0);

		for (Player p : getServer().getOnlinePlayers()) {
			p.getLocation(pos);

			Block bloque = pos.getBlock();

			// No intervenir con la mecánica de congelación vanilla de bloques de nieve en
			// polvo
			if (bloque.getType().equals(Material.POWDER_SNOW)) {
				continue;
			}

			p.lockFreezeTicks(true);

			// Los jugadores en modo creativo o espectador no tienen frío
			if (p.getGameMode().equals(GameMode.CREATIVE) || p.getGameMode().equals(GameMode.SPECTATOR)) {
				p.setFreezeTicks(0);
				continue;
			}

			boolean estaCercaDeFuenteDeCalor = bloque.getLightFromBlocks() > 11;
			float deltaCongelacion;
			if (estaCercaDeFuenteDeCalor) {
				// Las fuentes de calor descongelan de forma constante
				deltaCongelacion = -4;
			} else {
				// Congelar o descongelar al jugador dependiendo de la diferencia de temperaturas
				float temperatura = SimuladorTiempo.get().getTemperatura(p);
				float temperaturaMaxima = ajustarTemperaturaMaximaEnBaseAEquipamiento(p);
				deltaCongelacion = temperaturaMaxima - temperatura;
			}

			// Avisar al jugador si se está empezando a congelar
			int ticksCongelacion = p.getFreezeTicks();
			if (ticksCongelacion <= 0) {
				if (deltaCongelacion > 0 && deltaCongelacion < 4) {
					p.sendMessage(
						ChatColor.AQUA + "❄ Hace frío por aquí. Abrígate o busca una fuente de calor."
					);
				} else if (deltaCongelacion >= 4) {
					p.sendMessage(Component
						.text("❄ ¡Hace mucho frío por aquí! ¡Abrígate o busca una fuente de calor inmediatamente!")
						.color(NamedTextColor.AQUA)
						.decoration(TextDecoration.BOLD, true)
					);
				}
			}

			p.setFreezeTicks((int) (Math.min(Math.max(ticksCongelacion + deltaCongelacion, 0), 140)));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public final void onPlayerEvent(PlayerMoveEvent event) {
		// Asegurarse de que el bloque de nieve en polvo conserva mecánicas vanilla
		if (event.hasChangedBlock() && event.getTo().getBlock().getType().equals(Material.POWDER_SNOW)) {
			event.getPlayer().lockFreezeTicks(false);
		}
	}

	/**
	 * Calcula la temperatura máxima que un jugador puede soportar en base al
	 * equipamiento que lleva.
	 *
	 * @param p El jugador del que obtener la temperatura máxima que soporta.
	 * @return La temperatura máxima soportada por el jugador.
	 */
	private static float ajustarTemperaturaMaximaEnBaseAEquipamiento(Player p) {
		float temperaturaMaximaBase = TEMPERATURA_MAXIMA_BASE;
		PlayerInventory inventario = p.getInventory();
		ItemStack item;

		if ((item = inventario.getHelmet()) != null && item.getAmount() > 0) {
			switch (item.getType()) {
			case LEATHER_HELMET:
				temperaturaMaximaBase -= 2;
				break;
			case TURTLE_HELMET:
			case NETHERITE_HELMET:
				temperaturaMaximaBase -= 1.5;
				break;
			case IRON_HELMET:
			case GOLDEN_HELMET:
			case DIAMOND_HELMET:
				temperaturaMaximaBase -= 1;
				break;
			case CHAINMAIL_HELMET:
				temperaturaMaximaBase -= 0.5;
				break;
			default:
				break;
			}
		}

		if ((item = inventario.getChestplate()) != null && item.getAmount() > 0) {
			switch (item.getType()) {
			case LEATHER_CHESTPLATE:
				temperaturaMaximaBase -= 8;
				break;
			case NETHERITE_CHESTPLATE:
				temperaturaMaximaBase -= 6;
				break;
			case IRON_CHESTPLATE:
			case GOLDEN_CHESTPLATE:
			case DIAMOND_CHESTPLATE:
				temperaturaMaximaBase -= 4;
				break;
			case ELYTRA:
			case CHAINMAIL_CHESTPLATE:
				temperaturaMaximaBase -= 2;
				break;
			default:
				break;
			}
		}

		if ((item = inventario.getLeggings()) != null && item.getAmount() > 0) {
			switch (item.getType()) {
			case LEATHER_LEGGINGS:
				temperaturaMaximaBase -= 6;
				break;
			case NETHERITE_LEGGINGS:
				temperaturaMaximaBase -= 4.5;
				break;
			case IRON_LEGGINGS:
			case GOLDEN_LEGGINGS:
			case DIAMOND_LEGGINGS:
				temperaturaMaximaBase -= 3;
				break;
			case CHAINMAIL_LEGGINGS:
				temperaturaMaximaBase -= 1.5;
				break;
			default:
				break;
			}
		}

		if ((item = inventario.getBoots()) != null && item.getAmount() > 0) {
			switch (item.getType()) {
			case LEATHER_BOOTS:
				temperaturaMaximaBase -= 4;
				break;
			case NETHERITE_BOOTS:
				temperaturaMaximaBase -= 3;
				break;
			case IRON_BOOTS:
			case GOLDEN_BOOTS:
			case DIAMOND_BOOTS:
				temperaturaMaximaBase -= 2;
				break;
			case CHAINMAIL_BOOTS:
				temperaturaMaximaBase -= 1;
				break;
			default:
				break;
			}
		}

		return temperaturaMaximaBase;
	}

	/**
	 * Ayuda a implementar el patrón singleton de inicialización retardada al uso de
	 * la instancia, de forma segura entre hilos y eficiente.
	 *
	 * @author AlexTMjugador
	 */
	private static final class PoseedorInstanciaClase {
		private static final SimuladorHipotermia INSTANCIA = new SimuladorHipotermia();
	}
}
