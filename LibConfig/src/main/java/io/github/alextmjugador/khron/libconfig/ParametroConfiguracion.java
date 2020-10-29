/*
 * Plugins de Paper del Proyecto Khron
 * Copyright (C) 2019 Comunidad Aylas
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
package io.github.alextmjugador.khron.libconfig;

import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Representa un parámetro de configuración de un plugin.
 *
 * @param <E> El tipo del valor almacenado en memoria por este parámetro.
 * @param <T> El tipo de valor almacenado en el fichero de configuración YAML
 *            para este parámetro.
 * @author AlexTMjugador
 */
public abstract class ParametroConfiguracion<E, T> {
    /**
     * El plugin al que pertenece este parámetro de configuración.
     */
    private final Plugin plugin;
    /**
     * La ruta de este parámetro en el archivo de configuración del plugin.
     */
    private final String rutaConfiguracion;
    /**
     * La identificación de este parámetro de configuración en el comando para
     * cambiarlo del getPlugin().
     */
    private final String id;
    /**
     * El nombre del permiso que un emisor de comandos necesitará tener,
     * normalmente, para cambiar el parámetro.
     */
    private final String permiso;
    /**
     * El valor que toma el parámetro de configuración.
     */
    private E valor = null;

    /**
     * Crea un nuevo parámetro de configuración con su plugin asociado, la ruta
     * en el fichero de configuración, el nombre del argumento para el comando
     * que permite cambiarlo y el permiso necesario para realizarle
     * modificaciones.
     *
     * @param plugin            El plugin al que pertenece este parámetro de
     *                          configuración.
     * @param rutaConfiguracion La ruta de este parámetro en el archivo de
     *                          configuración del plugin.
     * @param id                La identificación de este parámetro de
     *                          configuración en el comando para cambiarlo del
     *                          plugin.
     * @param permiso           El nombre del permiso que un emisor de comandos
     *                          necesitará tener para cambiar el parámetro.
     * @throws IllegalArgumentException Si alguno de los argumentos
     *                                  {@code rutaConfiguracion},
     *                                  {@code nombreCodigo} y {@code permiso}
     *                                  es nulo.
     */
    public ParametroConfiguracion(Plugin plugin, String rutaConfiguracion, String id, String permiso) {
        if (plugin == null || rutaConfiguracion == null || id == null || permiso == null) {
            throw new IllegalArgumentException("Algún parámetro es nulo, cuando no debería de serlo");
        }
        this.plugin = plugin;
        this.rutaConfiguracion = rutaConfiguracion;
        this.id = id;
        this.permiso = permiso;
    }

    /**
     * Comprueba si un emisor de comandos tiene permisos para modificar este
     * parámetro de configuración.
     *
     * @param snd El emisor de comandos a comprobar.
     * @return Verdadero si el emisor de comandos especificado tiene permisos, falso
     *         en caso contrario.
     */
    public boolean puedeCambiarlo(CommandSender snd) {
        return snd.hasPermission(getPermiso());
    }

    /**
     * Obtiene si el valor almacenado para este parámetro de configuración es
     * válido.
     *
     * @return Verdadero si el susodicho valor es válido, falso en caso contrario.
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
     *         contrario.
     */
    public boolean valorValido(E otroValor) {
        // Si la implementación no decide otra cosa, solo el valor nulo es inválido
        return otroValor != null;
    }

    /**
     * Obtiene el plugin asociado a este parámetro de configuración.
     * 
     * @return El devandicho plugin.
     */
    final public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Devuelve la ruta de este parámetro en el archivo de configuración del plugin.
     *
     * @return La ruta de este parámetro en el archivo de configuración del plugin.
     */
    final public String getRutaConfiguracion() {
        return rutaConfiguracion;
    }

    /**
     * Obtiene la identificación de este parámetro de configuración en el comando
     * para cambiarlo del plugin.
     *
     * @return La devandicha identificación.
     */
    final public String getId() {
        return id;
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
     * Obtiene el valor tal y como se debe de guardar en un fichero de configuración
     * YAML.
     *
     * @return El susodicho valor. La implementación predeterminada de este método
     *         asume que la clase del objeto a guardar en el fichero de
     *         configuración YAML es la misma o una superclase de la clase del valor
     *         en memoria.
     */
    protected T getValorYaml() {
        @SuppressWarnings("unchecked")
        T toret = (T) valor;
        return toret;
    }

    /**
     * Recarga los contenidos del fichero de configuración asociado a este parámetro
     * de configuración desde memoria secundaria. Es útil llamar a este método antes
     * de {@link leer} lo que se espera que sean nuevos valores para los parámetros
     * de configuración.
     */
    final public void recargarConfiguracion() {
        getPlugin().reloadConfig();
    }

    /**
     * Lee el valor guardado en la configuración del plugin para este parámetro,
     * e inicializa el valor asociado a este parámetro de configuración a partir
     * de lo leído desde el fichero de configuración. Cabe destacar que este
     * método por si solo no recarga la configuración asociada al plugin en
     * memoria por Paper.
     * <p>
     * La implementación predeterminada de este método asume que el tipo de dato
     * cargado en memoria es el mismo que el que se guarda en el fichero YAML.
     * </p>
     * 
     * @throws IllegalArgumentException Si el valor guardado en la configuración
     *                                  del plugin para este parámetro no es
     *                                  válido.
     */
    public void leer() {
        @SuppressWarnings("unchecked")
        E toset = (E) getPlugin().getConfig().get(rutaConfiguracion, null);

        if (!setValor(toset, false)) {
            throw new IllegalArgumentException(
                "El valor de configuración para la clave \"" + getRutaConfiguracion() +
                "\" no es válido (valor leído: " + Objects.toString(toset) + ")"
            );
        }
    }

    /**
     * Establece el valor de este parámetro de configuración, si es válido. Se
     * utiliza {@link valorValido} para determinar la validez del nuevo valor, y
     * {@link procesarValor} para preprocesar el valor que tomará el atributo de
     * la clase que lo almacena.
     *
     * @param nuevoValor    El valor a establecer.
     * @param guardarADisco Verdadero si se debe de considerar la posibilidad de
     *                      guardar el nuevo valor a memoria secundaria si ha
     *                      cambiado, falso en caso contrario.
     * @return Verdadero si el nuevo valor se pudo establecer por ser válido,
     *         falso en caso contrario.
     */
    final protected boolean setValor(E nuevoValor, boolean guardarADisco) {
        E valorAnterior = getValor();
        boolean toret;

        if (valorAnterior == null || !valorAnterior.equals(nuevoValor)) {
            toret = valorValido(nuevoValor);

            if (toret) {
                this.valor = procesarValor(nuevoValor);

                if (guardarADisco) {
                    getPlugin().getSLF4JLogger().info("Se guarda configuración del plugin a memoria secundaria");
                    getPlugin().getConfig().set(rutaConfiguracion, getValorYaml());
                    getPlugin().saveConfig();
                }
            }
        } else {
            toret = true;
        }

        return toret;
    }

    /**
     * Establece el valor de este parámetro de configuración, si es válido. Se
     * utiliza {@link valorValido} para determinar la validez del nuevo valor, y
     * {@link procesarValor} para preprocesar el valor que tomará el atributo de la
     * clase que lo almacena. Si se trata de un nuevo valor diferente al anterior,
     * se guardará el nuevo valor en el fichero de configuración del plugin.
     *
     * @param nuevoValor El valor a establecer.
     * @return Verdadero si el nuevo valor se pudo establecer por ser válido, falso
     *         en caso contrario.
     */
    final public boolean setValor(E nuevoValor) {
        return setValor(nuevoValor, true);
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
     * @return El susodicho valor, procesado. La implementación predeterminada
     *         de este método simplemente devuelve el valor que recibe.
     */
    protected E procesarValor(E nuevoValor) {
        return nuevoValor;
    }

    /**
     * Establece el valor de este parámetro de configuración, si es válido,
     * convirtiéndolo antes de {@link String} al tipo de dato que se use para
     * almacenar el valor. Se recomienda que las implementaciones utilicen
     * setValor para establecer el valor tras la conversión que sea necesaria.
     * Este método es usado para modificar el valor del parámetro de
     * configuración a partir de lo especificado por un usuario en un comando.
     *
     * @param nuevoValor El valor a establecer, como una cadena de texto que
     *                   será convertida al tipo de dato usado.
     * @return Verdadero si el nuevo valor se pudo establecer por ser válido,
     *         falso en caso contrario. La implementación predeterminada de este
     *         método establece directamente el valor que recibe, para lo que el
     *         parámetro de configuración debe de tener una cadena de texto en
     *         sus parámetros de tipos.
     */
    @SuppressWarnings("unchecked")
    public boolean parsearValor(String nuevoValor) {
        return setValor((E) nuevoValor, false);
    }
}