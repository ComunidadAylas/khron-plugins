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
package io.github.alextmjugador.khron.tiemporeal.meteorologia;

import java.lang.reflect.Modifier;
import java.util.NoSuchElementException;

/**
 * Ofrece servicios de creación de climas a otras partes del plugin.
 *
 * @author AlexTMjugador
 */
public final class FactoriaClima {
    /**
     * Restringe la instanciación accidental de esta clase.
     */
    private FactoriaClima() {}

    /**
     * Crea un clima a partir de su nombre identificativo, que actualmente se
     * corresponde con el nombre simple de su clase.
     *
     * @param nombreClima El nombre del clima a crear.
     * @return El clima creado.
     * @throws NoSuchElementException Si no se ha podido instanciar un clima con
     *                                el nombre especificado, posiblemente
     *                                porque el nombre es incorrecto.
     */
    public static Clima crearPorNombre(String nombreClima) {
        try {
            Class<?> claseClima = Class.forName(FactoriaClima.class.getPackage().getName() + "." + nombreClima);
            int modificadoresClaseClima = claseClima.getModifiers();

            if (
                Clima.class.isAssignableFrom(claseClima) &&
                !Modifier.isAbstract(modificadoresClaseClima) &&
                !Modifier.isInterface(modificadoresClaseClima)
            ) {
                return (Clima) claseClima.getDeclaredConstructor().newInstance();
            } else {
                throw new IllegalAccessException("La clase especificada no implementa un clima");
            }
        } catch (ReflectiveOperationException | LinkageError exc) {
            throw new NoSuchElementException(exc.getMessage());
        }
    }
}