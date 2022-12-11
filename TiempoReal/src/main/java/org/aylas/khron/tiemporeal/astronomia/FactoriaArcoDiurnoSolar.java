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
package org.aylas.khron.tiemporeal.astronomia;

import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;

/**
 * Ofrece servicios de creación de arcos diurnos solares a otras partes del plugin.
 *
 * @author AlexTMjugador
 */
public final class FactoriaArcoDiurnoSolar {
    /**
     * Restringe la instanciación accidental de esta clase.
     */
    private FactoriaArcoDiurnoSolar() {}

    /**
     * Crea un arco diurno solar a partir de su nombre identificativo, que
     * actualmente se corresponde con el nombre simple de su clase.
     *
     * @param nombreArco El nombre del arco diurno solar a crear.
     * @return El arco diurno solar creado.
     * @throws NoSuchElementException Si no se ha podido instanciar un arco diurno
     *                                solar con el nombre especificado, posiblemente
     *                                porque el nombre es incorrecto.
     */
    public static ArcoDiurnoSolar crearPorNombre(String nombreArco) {
        try {
            Class<?> claseArco = Class.forName(FactoriaArcoDiurnoSolar.class.getPackage().getName() + "." + nombreArco);
            int modificadoresClaseArco = claseArco.getModifiers();

            if (
                ArcoDiurnoSolar.class.isAssignableFrom(claseArco) &&
                !Modifier.isAbstract(modificadoresClaseArco) &&
                !Modifier.isInterface(modificadoresClaseArco)
            ) {
                return (ArcoDiurnoSolar) claseArco.getDeclaredConstructor().newInstance();
            } else {
                throw new IllegalAccessException("La clase especificada no implementa un arco diurno solar");
            }
        } catch (ReflectiveOperationException | LinkageError exc) {
            throw new NoSuchElementException(exc.getMessage());
        }
    }
}