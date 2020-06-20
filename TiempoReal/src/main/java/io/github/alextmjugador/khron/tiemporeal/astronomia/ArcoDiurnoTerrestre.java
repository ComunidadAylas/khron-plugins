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
 * Los algoritmos implementados en esta clase están basados en <i>The
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
final class ArcoDiurnoTerrestre implements ArcoDiurnoSolar {
    /**
	 * Restringe la instanciación a clases de este paquete.
	 */
    ArcoDiurnoTerrestre() {}

    /**
     * El doble del valor de pi.
     */
    private static final double PI_2 = 2 * Math.PI;

    @Override
    public PosicionObjetoAstronomico getPosicionSol(Instant instante, double latitud, double longitud) {
        if (instante == null) {
            throw new IllegalArgumentException("Un arco diurno no puede usar un instante nulo");
        }

        // El algoritmo que viene a continuación es una transcripción de
        // https://en.wikipedia.org/w/index.php?title=Position_of_the_Sun&oldid=955802092#Approximate_position
        // Los ángulos se han convertido a radianes para un mejor aprovechamiento de la
        // precisión de los números de coma flotante y un uso directo de los métodos de trigonometría de Java

        // El día juliano (JD, Julian day) astronómico se puede obtener a partir del
        // tiempo Unix proporcionado por Java, mediante la expresión JD = UT / 86400 + 2440587.5.
        // En realidad, las fórmulas a continuación podrían usar UT1, que no es exactamente igual a UTC,
        // pero como máximo tiene un desfase de 0,9 s, suficientemente cercano para nuestros propósitos.
        // Del JD nos interesa su conversión a número de días desde mediodía en Greenwich, tiempo terrestre,
        // 1 de enero del 2000, n. Luego n = JD - 2451545 = (UT / 86400 + 2440587.5) - 2451545 =
        // = UT / 86400 - 10957.5
        double n = instante.getEpochSecond() / 86400.0 - 10957.5;

        // Oblicuidad de la eclíptica del Sol
        double epsilon = 0.409087723 + 0.000000006981317008 * n;

        // Calcular coordenadas eclípticas

        // Longitud media, módulo 2 * pi para mayor precisión
        double L = (4.89495042 + 0.0172027923937 * n) % PI_2;
        // Anomalía media, módulo 2 * pi para mayor precisión
        double g = (6.240040768 + 0.0172019703436 * n) % PI_2;
        // Longitud eclíptica
        double lambda = L + 0.033423055 * Math.sin(g) + 0.0003490659 * Math.sin(2 * g);
        // La latitud eclíptica del Sol aproximada es 0
        // Distancia de la Tierra al Sol, en unidades astronómicas.
        // Su valor no se usa luego, pero define un punto en el sistema de coordenadas eclíptico
        //double R = 1.00014 - 0.01671 * Math.cos(g) - 0.00014 * Math.cos(2 * g);

        // Conversión a coordenadas ecuatoriales

        double senoLambda = Math.sin(lambda);

        // La ascensión recta es alfa = Math.atan2(Math.cos(epsilon) * senoLambda, Math.cos(lambda)).
        // A partir de ella podemos hallar el ángulo horario, más útil para calcular coordenadas
        // horizontales, como h = tiempo sidéreo de Greenwich + longitud - alfa.
        // Una aproximación al tiempo sidéreo de Greenwich es el tiempo sidéreo de Greenwich medio
        // (GMST), que se puede aproximar en horas por GMST = 18.697374558 + 24.06570982441908 * n:
        // https://astronomy.stackexchange.com/questions/12026/low-precision-gmst-formula-clarification
        // La diferencia entre el tiempo sidéreo medio y el aparente es que el primero no tiene en cuenta
        // la nutación de la Tierra.
        // Un tiempo sidéreo en radianes se obtiene como TSrad = (2 * pi * TSh) / 24 = (pi * TSh) / 12,
        // lo que permite expresar la fórmula anterior como GMST = 4.894961213 + 6.300388099 * n
        double h = ((4.894961213 + 6.300388099 * n) % PI_2) +
            longitud -
            Math.atan2(Math.cos(epsilon) * senoLambda, Math.cos(lambda));

        // Declinación
        double delta = Math.asin(Math.sin(epsilon) * senoLambda);

        // Conversión a coordenadas horizontales
        // https://en.wikipedia.org/w/index.php?title=Celestial_coordinate_system&oldid=958842725#Equatorial_%E2%86%94_horizontal

        double senDelta = Math.sin(delta);
        double cosDelta = Math.cos(delta);
        double senLatitud = Math.sin(latitud);
        double cosLatitud = Math.cos(latitud);
        double cosH = Math.cos(h);

        // Acimut
        double A = -Math.atan2(
            cosDelta * Math.sin(h),
            -senLatitud * cosDelta * cosH + cosLatitud * senDelta
        );

        // Altura
        double a = Math.asin(
            senLatitud * senDelta + cosLatitud * cosDelta * cosH
        );

        return new PosicionObjetoAstronomico(a, A);
    }

    @Override
    public long getTiempoJugador(Instant instante, World mundo, double latitud, double longitud) {
        if (mundo == null) {
            throw new IllegalArgumentException("No se admite un mundo nulo para esta operación");
        }

        long tiempoJugador = getTiempoMundo(instante, latitud, longitud);

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
        if (Math.sin(latitud) < 0) {
            // Hemisferio sur (el ángulo ocupa el tercer o cuarto cuadrante en la circunferencia
            // goniométrica). El aspecto de las fases lunares se corresponde con el que
            // dibuja Minecraft
            tiempoJugador += 24000 * ((faseLunar + 4) & 7); // & 7 = % 8 para enteros positivos
        } else {
            // Hemisferio norte. El aspecto de las fases lunares está invertido respecto
            // al que dibuja Minecraft
            tiempoJugador += 24000 * ((12 - faseLunar) & 7);
        }

        // Sumar días transcurridos en el mundo del servidor con cuidado de no alterar la fase lunar,
        // siempre que ello no genere un desbordamiento
        long ticksDiasTranscurridos = 192200 * (mundo.getFullTime() / 24000);
        if (tiempoJugador + ticksDiasTranscurridos >= tiempoJugador) {
            tiempoJugador += 192200 * (mundo.getFullTime() / 24000);
        }

        return tiempoJugador;
    }

    @Override
    public long getTiempoMundo(Instant instante, double latitud, double longitud) {
        PosicionObjetoAstronomico posicionSol = getPosicionSol(instante, latitud, longitud);

        // Para renderizar el Sol, Minecraft dibuja un plano paralelo al suelo, que va rotando
        // en el eje X según el valor del coeficiente celestial α, y tiene la textura del Sol.
        // En cualquier momento, la rotación de ese plano en X es α · 2π rad. Una rotación de
        // 0 = 2π se corresponde con el cénit del Sol, π/2 con el atardecer, π con el cénit de
        // la luna, y 3π/2 con el amanecer (sentido horario de rotación).

        // Como Minecraft renderiza el Sol con acimut constante, admitiendo solamente una altura
        // variable cuyo punto máximo es π/2 para todos los días, usamos el acimut como altura,
        // lo que equivale a rotar el plano descrito por el movimiento del Sol de forma que quede
        // perpendicular al suelo. Ello debería de verse bien, ya que independientemente de la
        // estación y coordenadas geográficas representa con precisión el grado de progreso del día.

        // Primero pasamos el acimut a un intervalo [0, 2π), donde el punto de referencia del ángulo
        // coincide con el usado por el plano del Sol en el cielo de Minecraft, obteniendo A'
        double acimutPrima = (((posicionSol.getAcimut() + PI_2) % PI_2) + Math.PI) % PI_2;

        // Por construcción, A' = α · 2π. Luego α = A' / 2π
        double alfa = acimutPrima / PI_2;

        // Minecraft calcula α a partir de los ticks transcurridos desde el último amanecer (t), según
        // la ecuación α = x + ((1 - (cos(x * π) + 1) / 2) - x) / 3, donde x = (t / 24000) - 0.25,
        // sumándole 1 a x si x < 0. Una expresión equivalente para calcular x es x =
        // = (t / 24000 - 0.25 + 1) mod 1 = (t / 24000 + 0.75) mod 1. Teniendo en cuenta la igualdad
        // de la división, D = d * c + r, r = x = t / 24000 + 0.75 - floor(t / 24000 + 0.75).
        // Sustituyendo x = r en la expresión de α y operando, nos queda
        // α = ((4*t)/24000+4*(1-floor(t/24000+0.75))-cos(π*(t/24000+0.75-floor(t/24000+0.75))))/6.

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
        // convergencias rápidas, y a partir de él calculamos iteraciones hasta t4.

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
     * {@inheritDoc} Un arco diurno terrestre simula el planeta Tierra, por lo que
     * este método devuelve verdadero.
     */
    @Override
    public boolean simulaPlaneta() {
        return true;
    }
}