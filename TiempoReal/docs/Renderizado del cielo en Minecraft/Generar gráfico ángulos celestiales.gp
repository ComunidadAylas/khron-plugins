#!/usr/bin/gnuplot
set xtics 1000
set xlabel 'Ticks desde amanecer actual'
set ylabel 'Ángulo celestial (coeficiente de ángulo)'
set term svg size 1024, 768
set output 'Gráfico ángulos celestiales.svg'
plot [0:24000] 'Ángulos celestiales.txt' notitle with lines linecolor 'black'
