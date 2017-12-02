/*
 * Copyright (C) 2017 Proyecto Khron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.alextmjugador.khron.tiemporeal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import static org.bukkit.Bukkit.getServer;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;

/**
 * Representa un parámetro de configuración del plugin.
 *
 * @author AlexTMjugador
 * @param <E> El tipo del valor almacenado por este parámetro.
 */
abstract class ParametroConfiguracion<E> {
    /**
     * La ruta de este parámetro en el archivo de configuración del plugin.
     */
    private final String rutaConfiguracion;
    /**
     * La identificación este parámetro de configuración en el comando para
     * cambiarlo del plugin.
     */
    private final String nombreCodigo;
    /**
     * El nombre del permiso que un emisor de comandos necesitará tener para
     * cambiar el parámetro.
     */
    private final String permiso;
    /**
     * El valor que toma el parámetro de configuración.
     */
    private E valor;

    /**
     * Crea un nuevo parámetro de configuración con la ruta, el nombre del
     * argumento para el comando que permite cambiarlo y el permiso necesario
     * para realizar modificaciones a él.
     *
     * @param rutaConfiguracion La ruta de este parámetro en el archivo de
     * configuración del plugin. No debe de ser nulo.
     * @param nombreArgumentoComando La identificación este parámetro de
     * configuración en el comando para cambiarlo del plugin. No debe de ser
     * nulo.
     * @param permiso El nombre del permiso que un emisor de comandos necesitará
     * tener para cambiar el parámetro. No debe de ser nulo.
     * @param valor El valor que toma inicialmente la configuración. El
     * constructor no verifica su validez. Si es nulo, no se guardará en disco
     * el siguiente valor no nulo que este parámetro de configuración tome.
     */
    public ParametroConfiguracion(String rutaConfiguracion, String nombreArgumentoComando, String permiso, E valor) {
        this.rutaConfiguracion = rutaConfiguracion;
        this.nombreCodigo = nombreArgumentoComando;
        this.permiso = permiso;
        this.valor = valor;
    }

    /**
     * Comprueba si un emisor de comandos tiene permisos para modificar este
     * parámetro de configuración.
     *
     * @param snd El emisor de comandos a comprobar.
     * @return Verdadero si el emisor de comandos especificado tiene permisos,
     * falso en caso contrario.
     */
    public boolean puedeCambiarlo(CommandSender snd) {
        return snd.hasPermission(getPermiso());
    }
    
    /**
     * Obtiene si el valor almacenado para este parámetro de configuración es
     * válido.
     *
     * @return Verdadero si el susodicho valor es válido, falso en caso
     * contrario.
     */
    final public boolean valorValido() {
        return valorValido(valor);
    }
    
    /**
     * Obtiene si el valor especificado de este parámetro de configuración sería
     * válido.
     *
     * @param otroValor El valor a comprobar.
     * @return Verdadero si el susodicho parámetro sería válido, falso en caso
     * contrario.
     */
    public boolean valorValido(E otroValor) {
        // Si la implementación no decide otra cosa, solo el valor nulo es inválido
        return otroValor != null;
    }

    /**
     * Devuelve la ruta de este parámetro en el archivo de configuración del
     * plugin.
     *
     * @return La ruta de este parámetro en el archivo de configuración del
     * plugin.
     */
    final public String getRutaConfiguracion() {
        return rutaConfiguracion;
    }
    
    /**
     * Obtiene la identificación este parámetro de configuración en el comando
     * para cambiarlo del plugin.
     *
     * @return La devandicha identificación.
     */
    final public String getNombreCodigo() {
        return nombreCodigo;
    }

    /**
     * Obtiene el permiso que un emisor de comandos necesitará para cambiar el
     * parámetro.
     *
     * @return El susodicho permiso.
     */
    final public String getPermiso() {
        return permiso;
    }

    /**
     * Obtiene el valor al que está establecido este parámetro de configuración.
     *
     * @return El susodicho valor.
     */
    final public E getValor() {
        return valor;
    }
    
    /**
     * Obtiene el valor tal y como se debe de guardar en un fichero de
     * configuración YAML.
     *
     * @return El susodicho valor.
     */
    public Object getValorYaml() {
        return valor;
    }
    
    /**
     * Establece el valor de este parámetro de configuración, si es válido. Se
     * utiliza {@link valorValido} para determinar la validez del nuevo valor.
     * Es altamente recomendable que las sobreescrituras de este método utilicen
     * éste.
     *
     * @param nuevoValor El valor a establecer.
     * @return Verdadero si el nuevo valor se pudo establecer por ser válido,
     * falso en caso contrario.
     */
    public boolean setValor(E nuevoValor) {
        boolean valorPrevioNulo = getValor() == null;
        boolean toret = valorValido(nuevoValor);
        
        if (toret && (valorPrevioNulo || !getValor().equals(nuevoValor))) {
            this.valor = nuevoValor;
            if (!valorPrevioNulo) {
                // Si el valor previo es nulo, asumir que cargamos configuración desde disco y no guardar
                Configuracion.guardar();
            }
        }
        
        return toret;
    }
    
    /**
     * Funcionalidad de conveniencia para establece el valor de este parámetro
     * de configuración, si es válido, convirtiéndolo antes de {@link String} al
     * tipo de dato que se use para almacenar el valor. Se recomienda que las
     * implementaciones utilicen setValor para establecer el valor tras la
     * conversión que sea necesaria.
     *
     * @param nuevoValor El valor a establecer, como una cadena de texto que será
     * convertida al tipo de dato usado.
     * @return Verdadero si el nuevo valor se pudo establecer por ser válido,
     * falso en caso contrario.
     */
    public abstract boolean setValor(String nuevoValor);
    
    /**
     * Modela un parámetro de configuración que notifica a un método estático de
     * una determinada clase cuando se produce un cambio en él.
     *
     * @param <E> El tipo de dato del parámetro de configuración.
     */
    abstract static class ParametroConfiguracionNotificado<E> extends ParametroConfiguracion<E> {
        /**
         * Contiene una referencia al método que se invocará cuando cambie el
         * valor del parámetro de configuración.
         */
        private final Method metodoNotificacion;
        
        /**
         * {@inheritDoc}
         *
         * @param claseNotificacion La clase donde se espera encontrar un método
         * estático llamado "onConfigChange", que tome un único parámetro de
         * tipo {@link ParametroConfiguracionNotificado}.
         * @throws IllegalArgumentException Si la clase que contiene el método
         * de notificación no contiene tal método estático que cumpla las
         * condiciones esperadas.
         */
        public ParametroConfiguracionNotificado(String rutaConfiguracion, String nombreArgumentoComando, String permiso, E valor, Class<?> claseNotificacion) throws IllegalArgumentException {
            super(rutaConfiguracion, nombreArgumentoComando, permiso, valor);
            
            if (claseNotificacion == null) {
                throw new IllegalArgumentException("La clase notificación de este parámetro de configuración es nula");
            }
            
            try {
                this.metodoNotificacion = claseNotificacion.getMethod("onConfigChange");
            } catch (NoSuchMethodException | SecurityException exc) {
                throw new IllegalArgumentException(exc.getMessage());
            }
        }

        @Override
        public boolean setValor(E nuevoValor) {
            boolean valoresDiferentes = !getValor().equals(nuevoValor);
            boolean toret = super.setValor(nuevoValor);
            
            if (toret && valoresDiferentes) {
                try {
                    metodoNotificacion.invoke(null);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exc) {
                    getServer().getLogger().log(Level.WARNING, "[TiempoReal] Error interno en el plugin: no se ha podido notificar el cambio de configuración del plugin a otras partes del mismo. Su funcionamiento puede ser inconsistente en consecuencia. Detalles: {0}", exc.getMessage());
                }
            }
            
            return toret;
        }
    }
    
    /**
     * Modela un parámetro de configuración que contiene el conjunto de mundos
     * en el que este plugin sincronizará la hora.
     */
    public static final class MundosSincronizacion extends ParametroConfiguracionNotificado<Set<World>> {
        public MundosSincronizacion(String rutaConfiguracion, String nombreArgumentoComando, String permiso, Set<World> valor) {
            super(rutaConfiguracion, nombreArgumentoComando, permiso, valor, AgenteSincHora.class);
        }

        @Override
        public Object getValorYaml() {
            List<String> toret = new LinkedList<>();
            Set<World> valor = getValor();
            
            if (valor != null) {
                for (World w : valor) {
                    toret.add(w.getName());
                }
            }
            
            return toret;
        }

        @Override
        public boolean setValor(String nuevoValor) {
            boolean toret = true;
            Set<World> nuevoConjunto;
            
            if (nuevoValor != null) {
                nuevoConjunto = new HashSet<>();

                for (String nombreMundo : nuevoValor.split(",")) {
                    World w = getServer().getWorld(nombreMundo);
                    if (w != null) {
                        nuevoConjunto.add(w);
                    }
                }
                
                toret = setValor(nuevoConjunto);
            }
            
            return toret;
        }

        /**
         * {@inheritDoc} Los mundos del conjunto deben de ser no nulos y de entorno normal.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean valorValido(Set<World> otroValor) {
            boolean toret = otroValor != null;
            
            if (toret) {
                Iterator<World> iter = getValor().iterator();
                while (iter.hasNext() && toret) {
                    toret = esMundoValido(iter.next());
                }
            }
            
            return toret;
        }
        
        /**
         * Comprueba si un determinado mundo es válido (no es nulo y es de
         * entorno normal).
         *
         * @param w El mundo a comprobar.
         * @return Verdadero si es válido, falso en caso contrario.
         */
        private static boolean esMundoValido(World w) {
            return w != null && w.getEnvironment().equals(Environment.NORMAL);
        }
    }
    
    /**
     * Modela un parámetro de configuración que contiene el texto a mostrar
     * cuando un jugador empuñe un reloj, para que vea la hora.
     */
    public static final class TextoHora extends ParametroConfiguracion<String> {
        public TextoHora(String rutaConfiguracion, String nombreArgumentoComando, String permiso, String valor) {
            super(rutaConfiguracion, nombreArgumentoComando, permiso, valor);
        }

        @Override
        public boolean setValor(String nuevoValor) {
            String[] partes = ChatColor.translateAlternateColorCodes('&', nuevoValor).split(Configuracion.REGEX_CLAVE_TEXTO_HORA);
            StringBuilder toset = new StringBuilder(partes[0]);
            
            // Solo contiene un {HORA}. No entraría en el for, así que añadirlo
            if (nuevoValor.contains(Configuracion.PALABRA_CLAVE_TEXTO_HORA) && partes.length == 1) {
                toset.append(Configuracion.PALABRA_CLAVE_TEXTO_HORA);
            }
            
            // Empezando en la segunda parte, tras el primer {HORA}, restaurar y reaplicar formatos para evitar que las sustituciones de {HORA} tengan efectos adversos
            for (int i = 1; i < partes.length; ++i) {
                String formatosAplicables = ChatColor.getLastColors(partes[i - 1]);
                toset.append(Configuracion.PALABRA_CLAVE_TEXTO_HORA);
                toset.append(ChatColor.COLOR_CHAR).append(ChatColor.RESET);
                toset.append(formatosAplicables).append(partes[i]);
            }
            
            return super.setValor(toset.toString());
        }
        
        /**
         * {@inheritDoc} Debe de contener la cadena de texto determinada por el
         * atributo estático {@code PALABRA_CLAVE_TEXTO_HORA} en la clase
         * {@link Configuracion}, que el plugin reemplazará por la hora actual.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean valorValido(String otroValor) {
            return otroValor != null && otroValor.contains(Configuracion.PALABRA_CLAVE_TEXTO_HORA);
        }
    }
}