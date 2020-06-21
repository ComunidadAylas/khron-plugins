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

import org.bukkit.World;

/**
 * Representa el arco diurno del planeta Tierra, interpretando latitudes y
 * longitudes como puntos en la superficie de la Tierra.
 * <p>
 * Los algoritmos implementados en esta clase están inspirados en <i>The
 * Astronomical Almanac</i> (<i>El Almaneque Astronómico</i>), publicado por el
 * Observatorio Naval de los Estados Unidos, técnicas de análisis matemático,
 * y otra documentación que se indique.
 * </p>
 * <p>
 * Las latitudes se interpretan en radianes. Su valor es 0 en el ecuador, -π/2
 * en el polo sur y π/2 en el polo norte.
 * </p>
 * <p>
 * Las longitudes se interpretan en radianes. Valen 0 en el meridiano de
 * Greenwich, -π en el oeste y π en el este.
 * </p>
 *
 * @author AlexTMjugador
 */
final class ArcoDiurnoSolarTerrestre implements ArcoDiurnoSolar {
    /**
	 * Restringe la instanciación a clases de este paquete.
	 */
    ArcoDiurnoSolarTerrestre() {}

    /**
     * El doble del valor de pi.
     */
    private static final double PI_2 = 2 * Math.PI;

    @Override
    public long getTiempoJugador(Instant instante, World mundo, double latitud, double longitud) {
        if (mundo == null) {
            throw new IllegalArgumentException("No se admite un mundo nulo para esta operación");
        }

        long tiempoJugador = getTiempoMundo(instante, latitud, longitud);

        // El tiempo usado por Bukkit es relativo aunque le digamos que no es relativo
        // porque la documentación relativa a la relatividad del tiempo es relativamente mala.
        // Gracias por hacer que lo relativo no sea realmente relativo y obligarme a hacerlo
        // relativo, Bukkit
        long tiempoMundo = mundo.getFullTime();
        tiempoJugador -= tiempoMundo % 24000;

        long milisegundosUtc;
        try {
            milisegundosUtc = instante.toEpochMilli();
        } catch (ArithmeticException exc) {
            // Fallback a fase lunar constante
            milisegundosUtc = Long.MAX_VALUE;
        }

        // Fórmula adaptada de
        // http://community.facer.io/t/moon-phase-formula-updated/35691/7
        // Afirma tener un error de +- 20 min la mayor parte del tiempo,
        // y siempre menor que 60 min, para las fases lunares más significativas:
        // luna nueva (0 = 8), cuarto creciente (2), luna llena (4) y cuarto
        // menguante (6)
        int faseLunar = (int) ((((milisegundosUtc / 2551442844.0 - 0.228535)
            + 0.00591997 * Math.sin(milisegundosUtc / 5023359217.0 + 3.1705094)
            + 0.017672776 * Math.sin(milisegundosUtc / 378923968.0 - 1.5388144)
            - 0.0038844429 * Math.sin(milisegundosUtc / 437435791.0 + 2.0017235)
            - 0.00041488 * Math.sin(milisegundosUtc / 138539900 - 1.236334)) % 1) / 0.125);

        // Sumarle al tiempo relativo al amanecer actual el desfase apropiado para mostrar
        // la fase lunar que correspondería (para Minecraft, la primera fase lunar es luna llena)
        long faseLunarMundo = tiempoMundo / 24000;
        if (Math.sin(latitud) < 0) {
            // Hemisferio sur (el ángulo ocupa el tercer o cuarto cuadrante en la circunferencia
            // goniométrica). El aspecto de las fases lunares se corresponde con el que
            // dibuja Minecraft
            tiempoJugador += 24000 * (((faseLunar + 4) & 7) - faseLunarMundo); // & 7 = % 8 para enteros positivos
        } else {
            // Hemisferio norte. El aspecto de las fases lunares está invertido respecto
            // al que dibuja Minecraft
            tiempoJugador += 24000 * (((12 - faseLunar) & 7) - faseLunarMundo);
        }

        return tiempoJugador;
    }

    @Override
    public long getTiempoMundo(Instant instante, double latitud, double longitud) {
        // El algoritmo que viene a continuación es una adaptación de
        // https://en.wikipedia.org/wiki/Sunrise_equation#Complete_calculation_on_Earth
        // Los ángulos se han convertido a radianes para un mejor aprovechamiento de la
        // precisión de los números de coma flotante y un uso directo de los métodos de trigonometría de Java

        // El día juliano (JD, Julian day) astronómico se puede obtener a partir del
        // tiempo Unix proporcionado por Java, mediante la expresión JD = UT / 86400 + 2440587.5.
        // En realidad, las fórmulas a continuación podrían usar UT1, que no es exactamente igual a UTC,
        // pero como máximo tiene un desfase de 0,9 s, suficientemente cercano para nuestros propósitos
        long segundosUTC = instante.getEpochSecond();
        double JDahora = segundosUTC / 86400.0 + 2440587.5;
        double[] datosAhora = calcularMediodiaYAnguloHora(JDahora, latitud, longitud);

        // Calculamos un coeficiente para usar al calcular el ángulo celestial de Minecraft
        // (ver a continuación). Este coeficiente asume que la función que va desde un punto
        // horario interesante a otro es lineal, cuando en realidad no lo es, por lo que solo es precisa
        // en el amanecer, mediodía y atardecer. Sin embargo, ello es mucho más simple que obtener
        // el punto de inflexión de la función de altura del Sol respecto al tiempo, lo que requiere
        // expresar esa función en términos de los días julianos (queda monstruosa), calcular sus dos
        // primeras derivadas (que existen), aplicar Newton-Raphson para obtener la mediodía, tomar
        // ese valor como el más alto posible, y hallar el coeficiente dividiendo la altura actual
        // entre la más alta para hoy (función del valor de mediodía hallado por Newton-Raphson).
        // Habría que ajustar el coeficiente dependiendo de la fase del día: no es lo mismo una altura
        // de 30º durante un amanecer que durante un atardecer. Ello quizás se pueda conseguir
        // analizando la pendiente (valor de la primera derivada) de la función en el instante
        // actual.
        // TODO: tener el tiempo libre suficiente como para hacer lo anterior. Al menos la parte de
        // calcular la altura del Sol para una determinada fecha está en una revisión anterior
        // de esta clase...
        double beta;
        if (JDahora >= datosAhora[0] - datosAhora[1] && JDahora <= datosAhora[0] + datosAhora[1]) {
            // Si estamos entre el amanecer y el atardecer, nuestro coeficiente β va a tomar valores
            // en [-1, 1], donde -1 representa el amanecer, 0 el mediodía, y 1 el atardecer,
            // calculados linealmente a partir del momento de amanecer y atardecer del día de hoy.
            // Sea x = JDahora - datosAhora[0] (tiempo transcurrido desde mediodía):
            // β = -1 <-> x = -datosAhora[1]
            // β = 0 <-> x = 0
            // β = 1 <-> x = datosAhora[1]
            beta = (JDahora - datosAhora[0]) / datosAhora[1];
        } else {
            // Para el intervalo de tiempo comprendido entre el último atardecer y el siguiente
            // amanecer, β pertenece a [1, 3), donde 1 representa el atardecer, 2 un cénit ficticio
            // de la luna, y 3 el amanecer, que se interpreta como el -1 de la fórmula anterior.
            // Sea d = amanecer hoy - atardecer ayer = (datosAhora[0] - datosAhora[1]) - (datosAyer[0] + datosAyer[1]).
            // Sea x = JDahora - (datosAyer[0] + datosAyer[1]) (tiempo transcurrido desde el atardecer de ayer):
            // β = 1 <-> x = 0
            // β = 2 <-> x = d / 2
            // β = 3 (= -1) <-> x = d

            double[] datosAyer;
            if (JDahora < datosAhora[0] - datosAhora[1]) {
                // Hemos pasado medianoche. El ayer solar es también ayer en tiempo civil
                datosAyer = calcularMediodiaYAnguloHora((segundosUTC - 86400) / 86400.0 + 2440587.5, latitud, longitud);
            } else {
                // No hemos pasado medianoche. El ayer solar todavía sigue siendo hoy en tiempo civil,
                // pero la mañana solar es la mañana civil
                datosAyer = datosAhora;
                datosAhora = calcularMediodiaYAnguloHora((segundosUTC + 86400) / 86400.0 + 2440587.5, latitud, longitud);
            }

            beta = 1 +
                ((JDahora - (datosAyer[0] + datosAyer[1])) * 2) /
                (datosAhora[0] - datosAhora[1] - (datosAyer[0] + datosAyer[1]));
        }

        // Para renderizar el Sol, Minecraft dibuja un plano paralelo al suelo, que va rotando
        // en el eje X según el valor del coeficiente celestial α, y tiene la textura del Sol.
        // En cualquier momento, la rotación de ese plano en X es α · 2π rad. Una rotación de
        // 0 = 2π se corresponde con el cénit del Sol, π/2 con el atardecer, π con el cénit de
        // la luna, y 3π/2 con el amanecer (sentido horario de rotación).
        // La rotación en X que buscamos es r = β · π/2 rad. Tomando α = β/4, la rotación aplicada
        // por Minecraft sería entonces β/4 · 2π rad, equivalente a r.
        // Le sumamos 1 y tomamos el módulo 1 para que α esté en [0, 1] y siga representando la
        // misma rotación (la función rotación sobre α tiene periodo 1)
        double alfa = (beta / 4 + 1) % 1;

        // Minecraft calcula α a partir de los ticks transcurridos desde el último amanecer (t), según
        // la ecuación α = x + ((1 - (cos(x * π) + 1) / 2) - x) / 3, donde x = (t / 24000) - 0.25,
        // sumándole 1 a x si x < 0. Una expresión equivalente para calcular x es x =
        // = (t / 24000 - 0.25 + 1) mod 1 = (t / 24000 + 0.75) mod 1. Teniendo en cuenta la igualdad
        // de la división, D = d * c + r, r = x = t / 24000 + 0.75 - floor(t / 24000 + 0.75).
        // Sustituyendo x = r en la expresión de α y operando, nos queda
        // α = ((4*t)/24000+4*(1-floor(t/24000+0.75))-cos(π*(t/24000+0.75-floor(t/24000+0.75))))/6

        // El valor de α lo conocemos, luego es una constante más, y solo tenemos que despejar t.
        // Sin embargo, se trata de una ecuación presuntamente trascendente que no sé cómo resolver
        // exactamente. Por suerte, puede redefinirse como la función
        // f(t) = (t/6000+4*(1-floor(t/24000+0.75))-cos(π*(t/24000+0.75-floor(t/24000+0.75))))/6 - α,
        // que es doblemente derivable, excepto para t = 6000 (punto de discontinuidad causado por
        // el cálculo de x). Su primera derivada es siempre positiva, rozando el 0 en el punto de
        // discontinuidad, y se expresa como
        // f'(t) = (π*sin(π*(-floor(t/24000+0.75)+t/24000+0.75))+4)/144000.
        // Teniendo todo esto en cuenta, podemos aplicar varias iteraciones del método de Newton-Raphson
        // para obtener una solución numérica aproximada, más que suficiente para nuestros propósitos,
        // pues solo requerimos una precisión de +- 0.5 ticks (es necesario redondear a un entero el
        // resultado). Escogemos como valor inicial t0 = 16000, que experimentalmente ha mostrado brindar
        // convergencias rápidas, y a partir de él calculamos iteraciones hasta t4

        double tn;
        if (alfa <= 0.00001 || alfa >= 0.99999) {
            // Si alfa está muy cerca de la discontinuidad, devolver la solución aproximada que buscamos
            // directamente, pues Newton-Raphson podría fallar por varias razones
            tn = 6000;
        } else {
            // t1
            tn = 16000 - (0.4013079369273576507196279715071030563862662942244560 - alfa)
                / 0.0000488510102762665812852085054654283544573232390534;

            // t2
            double sueloTemp = Math.floor(tn / 24000 + 0.75);
            tn = tn -
                ((tn / 6000 + 4 * (1 - sueloTemp) - Math.cos(Math.PI * (tn / 24000 + 0.75 - sueloTemp))) / 6 - alfa) / // f
                ((Math.PI * Math.sin(Math.PI * (-sueloTemp + tn / 24000 + 0.75)) + 4) / 144000); // f'

            // t3
            sueloTemp = Math.floor(tn / 24000 + 0.75);
            tn = tn -
                ((tn / 6000 + 4 * (1 - sueloTemp) - Math.cos(Math.PI * (tn / 24000 + 0.75 - sueloTemp))) / 6 - alfa) / // f
                ((Math.PI * Math.sin(Math.PI * (-sueloTemp + tn / 24000 + 0.75)) + 4) / 144000); // f'

            // t4
            sueloTemp = Math.floor(tn / 24000 + 0.75);
            tn = tn -
                ((tn / 6000 + 4 * (1 - sueloTemp) - Math.cos(Math.PI * (tn / 24000 + 0.75 - sueloTemp))) / 6 - alfa) / // f
                ((Math.PI * Math.sin(Math.PI * (-sueloTemp + tn / 24000 + 0.75)) + 4) / 144000); // f'

            // En caso de que Newton-Raphson haya convergido a una solución fuera del rango [0, 24000),
            // pasarla al rango deseado, teniendo en cuenta que es una función periódica de periodo 24000.
            // Luego la redondeamos al entero más cercano por defecto
            tn = (int) (Math.abs(tn % 24000) + 0.5);
        }

        return (int) tn;
    }

    /**
     * Calcula el día juliano decimal de mediodía para un día juliano que se
     * pasa como parámetro, teniendo en cuenta la latitud y longitud del
     * observador en la Tierra. También devuelve el número de días julianos a
     * restar a mediodía para obtener el amanecer, o a sumar para obtener el
     * atardecer.
     *
     * @param JD       El día juliano del que calcular su mediodía y longitud
     *                 del día.
     * @param latitud  La latitud del observador en el globo terráqueo.
     * @param longitud La longitud del observador en el globo terráqueo.
     * @return Un array de dos posiciones no nulo, donde la primera posición es
     *         el día juliano del mediodía, y la segunda posición el número de
     *         días julianos a restar o sumar para calcular el amanecer o
     *         atardecer, respectivamente.
     */
    private static double[] calcularMediodiaYAnguloHora(double JD, double latitud, double longitud) {
        // El algoritmo que viene a continuación es una transcripción de
        // https://en.wikipedia.org/wiki/Sunrise_equation#Complete_calculation_on_Earth
        // Los ángulos se han convertido a radianes para un mejor aprovechamiento de la
        // precisión de los números de coma flotante y un uso directo de los métodos de trigonometría de Java

        // Del JD nos interesa su conversión a número de días desde mediodía en Greenwich, tiempo terrestre,
        // 1 de enero del 2000, n. Luego n = JD - 2451545. Le sumamos 0.5 para empezar los días en la
        // medianoche actual, y calculamos su función suelo para descartar el progreso del día actual, que
        // interfiere con las fórmulas que se usan.
        // A partir de n calculamos el tiempo de mediodía solar medio como J* = n - (longitud / 2 * pi)
        double jAsterisco = Math.floor(JD - 2451544.5) - (longitud / PI_2);

        // Anomalía media del Sol
        double M = (6.2400599667 + 0.01720196999454 * jAsterisco) % PI_2;

        // Longitud eclíptica del Sol
        double lambda =
            (M + 0.0334195645 * Math.sin(M) + 0.0003490659 * Math.sin(2 * M) + 0.000005236 * Math.sin(3 * M) + 4.9381857164) % PI_2;

        // Día juliano para el tiempo de mediodía local
        double JT = 2451545 + jAsterisco + 0.0053 * Math.sin(M) - 0.0069 * Math.sin(2 * lambda);

        // El seno del ángulo de declinación del Sol
        double senoDelta = Math.sin(lambda) * 0.39778850739;

        // Ángulo horario del Sol en un sistema de coordenadas ecuatorial
        double omega =
            Math.acos((-0.01448572613 - Math.sin(latitud) * senoDelta) / (Math.cos(latitud) * Math.cos(Math.asin(senoDelta))));

        return new double[] { JT, omega / PI_2 };
    }
}