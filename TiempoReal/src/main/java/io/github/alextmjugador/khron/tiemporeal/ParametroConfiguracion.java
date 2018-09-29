/*
 * Plugins de Spigot del Proyecto Khron
 * Copyright (C) 2018 Comunidad Aylas
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
     * El nombre del permiso que un emisor de comandos necesitará tener,
     * normalmente, para cambiar el parámetro.
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
     * configuración del plugin.
     * @param nombreCodigo La identificación este parámetro de configuración en
     * el comando para cambiarlo del plugin y en su código.
     * @param permiso El nombre del permiso que un emisor de comandos necesitará
     * tener para cambiar el parámetro.
     * @param valor El valor que toma inicialmente la configuración. El
     * constructor no verifica su validez. Si es nulo, no se guardará en disco
     * el siguiente valor no nulo que este parámetro de configuración tome.
     * @throws IllegalArgumentException Si alguno de los argumentos
     * {@code rutaConfiguracion}, {@code nombreCodigo} y {@code permiso} es
     * nulo.
     */
    public ParametroConfiguracion(String rutaConfiguracion, String nombreCodigo, String permiso, E valor) throws IllegalArgumentException {
        if (rutaConfiguracion != null && nombreCodigo != null && permiso != null) {
            throw new IllegalArgumentException("Algún parámetro es nulo, cuando no debería de serlo");
        }
        this.rutaConfiguracion = rutaConfiguracion;
        this.nombreCodigo = nombreCodigo;
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
     * utiliza {@link valorValido} para determinar la validez del nuevo valor, y
     * {@link procesarValor} para preprocesar el valor que tomará el atributo de
     * la clase que lo almacena.
     *
     * @param nuevoValor El valor a establecer.
     * @return Verdadero si el nuevo valor se pudo establecer por ser válido,
     * falso en caso contrario.
     */
    final public boolean setValor(E nuevoValor) {
        boolean valorPrevioNulo = getValor() == null;
        boolean toret = valorValido(nuevoValor);
        
        if (toret && (valorPrevioNulo || !getValor().equals(nuevoValor))) {
            this.valor = procesarValor(nuevoValor);
            if (!valorPrevioNulo) {
                // Si el valor previo es nulo, asumir que cargamos configuración desde disco y no guardar
                Configuracion.guardar();
            }
        }
        
        return toret;
    }

    /**
     * Procesa el valor que se le pasa como parámetro, dejándolo listo para ser
     * el valor guardado en las estructuras de datos internas del plugin. Este
     * método solo debe de ser llamado internamente desde su clase, no desde
     * otras (las sobreescrituras de las subclases pueden asumir tal condición).
     * Entonces, cuando es invocado, se ha garantizado que {@code nuevoValor} es
     * válido y diferente al actual, y el valor devuelto será asignado como
     * nuevo valor del parámetro de configuración.
     *
     * @param nuevoValor El valor a procesar.
     * @return El susodicho valor, procesado.
     * @todo. Mover este método a otro paquete para hacer imposible que sea
     * llamado desde otras clases del mismo. Posiblemente esta clase sea buen
     * material para una librería.
     */
    protected E procesarValor(E nuevoValor) {
        return nuevoValor;
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
    public abstract static class ParametroConfiguracionNotificado<E> extends ParametroConfiguracion<E> {
        /**
         * Contiene una referencia al método que se invocará cuando cambie el
         * valor del parámetro de configuración.
         */
        private final Method metodoNotificacion;
        
        /**
         * {@inheritDoc}
         *
         * @param claseNotificacion La clase donde se espera encontrar un método
         * estático llamado "onConfigChange", cuyo único parámetro es el nuevo
         * valor del parámetro de configuración.
         * @throws IllegalArgumentException Si la clase que contiene el método
         * de notificación no contiene tal método estático, o no es accesible.
         */
        public ParametroConfiguracionNotificado(String rutaConfiguracion, String nombreArgumentoComando, String permiso, E valor, Class<?> claseNotificacion) throws IllegalArgumentException {
            super(rutaConfiguracion, nombreArgumentoComando, permiso, valor);
            
            if (claseNotificacion == null) {
                throw new IllegalArgumentException("La clase notificación de este parámetro de configuración es nula");
            }
            
            try {
                this.metodoNotificacion = claseNotificacion.getMethod("onConfigChange", Object.class);
            } catch (NoSuchMethodException | SecurityException exc) {
                throw new IllegalArgumentException(exc.getMessage());
            }
        }

        @Override
        protected E procesarValor(E nuevoValor) {
            try {
                metodoNotificacion.invoke(null, nuevoValor);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exc) {
                getServer().getLogger().log(Level.WARNING, "[TiempoReal] Error interno en el plugin: no se ha podido notificar el cambio de configuración del plugin a otras partes del mismo. Su funcionamiento puede ser inconsistente en consecuencia. Detalles: {0}", exc.getMessage());
            }
            
            return nuevoValor;
        }
    }
    
    /**
     * Modela un parámetro de configuración que contiene el conjunto de mundos
     * en el que este plugin sincronizará la hora.
     */
    public static final class MundosSincronizacion extends ParametroConfiguracionNotificado<Set<World>> {
        public MundosSincronizacion(String rutaConfiguracion, String nombreArgumentoComando, String permiso, Set<World> valor) throws IllegalArgumentException {
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
                    if (esMundoValido(w)) {
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
        /**
         * La cadena de texto, sin los cambios aplicados por {@link procesarValor}.
         */
        private String valorSinProcesar;
        /**
         * El número máximo de caracteres que este valor puede tomar.
         */
        private final int CARACTERES_MAX = 75;
        
        public TextoHora(String rutaConfiguracion, String nombreArgumentoComando, String permiso, String valor) throws IllegalArgumentException {
            super(rutaConfiguracion, nombreArgumentoComando, permiso, valor);
            this.valorSinProcesar = valor;
        }
        
        @Override
        protected String procesarValor(String nuevoValor) {
            String toret = ChatColor.translateAlternateColorCodes('&', nuevoValor);
            
            valorSinProcesar = nuevoValor;
            if (toret.matches(".*" + Configuracion.REGEX_CLAVE_TEXTO_HORA + ".*\\S+")) {
                int i = toret.indexOf(Configuracion.PALABRA_CLAVE_TEXTO_HORA);
                String parteAnt = toret.substring(0, i); // i no va a valer -1, así que por lo menos tenemos la cadena vacía
                toret = toret.replaceFirst(Configuracion.REGEX_CLAVE_TEXTO_HORA, Configuracion.PALABRA_CLAVE_TEXTO_HORA + ChatColor.RESET + ChatColor.getLastColors(parteAnt));
            }
            
            return toret;
        }

        @Override
        public Object getValorYaml() {
            return valorSinProcesar;
        }
        
        /**
         * {@inheritDoc} Debe de contener una sola vez la cadena de texto
         * determinada por el atributo estático {@code PALABRA_CLAVE_TEXTO_HORA}
         * en la clase {@link Configuracion}, que el plugin reemplazará por la
         * hora actual. Tampoco debe de ser mayor de {@link CARACTERES_MAX} caracteres.
         *
         * @return {@inheritDoc}
         */
        @Override
        public boolean valorValido(String otroValor) {
            boolean toret = false;
            
            if (otroValor != null && otroValor.length() <= CARACTERES_MAX) {
                int ultimoIndice = otroValor.lastIndexOf(Configuracion.PALABRA_CLAVE_TEXTO_HORA);
                toret = ultimoIndice != -1 && ultimoIndice == otroValor.indexOf(Configuracion.PALABRA_CLAVE_TEXTO_HORA);
            }
            
            return toret;
        }
    }
}