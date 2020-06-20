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
package io.github.alextmjugador.khron.tiemporeal.astronomia;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import org.bukkit.World;

/**
 * Representa un arco diurno simple, con las mismas características que el ciclo
 * día-noche integrado en Minecraft: duración el día constante para todas las
 * épocas del año, fases lunares cambiantes cada día, etc., pero con una hora
 * actual que se corresponde con aquella en la que el servidor se ejecuta.
 *
 * @author AlexTMjugador
 */
final class ArcoDiurnoSimple implements ArcoDiurnoSolar {
	/**
	 * Restringe la instanciación a clases de este paquete.
	 */
	ArcoDiurnoSimple() {}

    @Override
	public PosicionObjetoAstronomico getPosicionSol(Instant instante, double latitud, double longitud) {
		return null;
	}

	@Override
	public long getTiempoJugador(Instant instante, World mundo, double latitud, double longitud) {
		return mundo.getFullTime();
	}

	@Override
	public long getTiempoMundo(Instant instante, double latitud, double longitud) {
		ZonedDateTime fecha = instante.atZone(ZoneId.systemDefault());

		// El día de Minecraft empieza a las 6 AM
		int h = (fecha.get(ChronoField.HOUR_OF_DAY) + 18) % 24;
		int m = fecha.get(ChronoField.MINUTE_OF_HOUR);
		int s = fecha.get(ChronoField.SECOND_OF_MINUTE);
		int ms = fecha.get(ChronoField.MILLI_OF_SECOND);

		// La siguiente expresión se obtiene tras simplificar
		// h * 1000 + (m / 60) * 1000 + (s / 3600) * 1000 + (ms / 1000 / 3600) * 1000
		return (h * 1000) + ((m * 50) / 3) + ((s * 5) / 18) + (ms / 3600);
	}

	@Override
	public boolean simulaPlaneta() {
		return false;
	}
}