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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.bukkit.Bukkit.getServer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Provee una interfaz a otras partes del plugin para acceder y modificar
 * configuraciones.
 *
 * @author AlexTMjugador
 */
final class Configuracion {
    /**
     * La palabra clave que debe de contener el texto de la hora en el reloj
     * para ser válido. Se sustituirá por la hora del mundo en el que el jugador
     * esté.
     */
    public static final String PALABRA_CLAVE_TEXTO_HORA = "{HORA}";
    /**
     * La cadena de texto {@link PALABRA_CLAVE_TEXTO_HORA}, pero construida de
     * manera que puede ser interpretada con seguridad como una expresión
     * regular.
     */
    public static final String REGEX_CLAVE_TEXTO_HORA = "\\{HORA\\}";
    
    /**
     * Contiene los parámetros de configuración de este plugin.
     */
    private static final ParametroConfiguracion<?>[] PARAMETROS = new ParametroConfiguracion<?>[2];
    
    /**
     * Contiene los datos de identificación de los parámetros de configuración.
     * Deben de ser coherentes con los contenidos de config.yml y plugin.yml.
     */
    private static final String[][] DATOS_PARAMETROS = new String[][]
    {
        { "Mundos en los que sincronizar la hora", "mundosSinc", "tiemporeal.trconfig.mundosSinc" },
        { "Texto de hora en reloj", "textoHora", "tiemporeal.trconfig.textoHora" }
    };
    
    /**
     * El índice del dato de la ruta del parámetro de configuración, en una fila
     * (o columna) del array {@link DATOS_PARAMETROS}.
     */
    private static final int DATO_RUTA = 0;
    /**
     * El índice del dato del nombre en el código del parámetro de
     * configuración, en una fila (o columna) del array
     * {@link DATOS_PARAMETROS}.
     */
    private static final int DATO_NOMBRE_COD = 1;
    /**
     * El índice del dato del permiso necesario para cambiar el parámetro de
     * configuración, en una fila (o columna) del array
     * {@link DATOS_PARAMETROS}.
     */
    private static final int DATO_PERMISO = 2;
    
    /**
     * El plugin del que obtener y al que guardar los parámetros de
     * configuración.
     */
    private static Plugin plugin;
    
    /**
     * Prepara la configuración del plugin para ser leída o modificada.
     *
     * @param plugin El plugin asociado a la configuración.
     * @throws IllegalArgumentException Si el plugin es nulo, está detenido o
     * algún parámetro de configuración no es válido.
     */
    public static void inicializar(Plugin plugin) throws IllegalArgumentException {
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalArgumentException("Se ha intentado inicializar la configuración con un plugin nulo o detenido");
        }
        
        try {
            for (int i = 0; i < PARAMETROS.length; ++i) {
                inicializarParametro(i, plugin);
            }
        } catch (IllegalArgumentException exc) {
            throw exc;
        }
        
        Configuracion.plugin = plugin;
    }
    
    /**
     * Obtiene el parámetro de configuración del tipo especificado.
     *
     * @param tipo El tipo del parámetro de configuración a obtener.
     * @return El parámetro de configuración deseado, o null si no se pudo
     * encontrar.
     * @throws IllegalStateException Si se llama antes de que la configuración
     * del plugin sea inicializada correctamente.
     * @todo. Estudiar alguna manera de evitar el cast sin comprobar que las
     * clases que utilizan este método tienen que hacer.<br>Idea AlexTMjugador
     * 02/12/2017: si no se guardan los parámetros en esta clase como tan solo
     * un array, quizá se pueda resolver eso.
     */
    public static ParametroConfiguracion<?> get(Class<? extends ParametroConfiguracion<?>> tipo) throws IllegalStateException {
        ParametroConfiguracion<?> toret = null;
        int i = 0;
        
        if (plugin == null) {
            throw new IllegalStateException("La configuración no ha sido inicializada");
        }
        
        while (i < PARAMETROS.length && toret == null) {
            ParametroConfiguracion<?> actual = PARAMETROS[i++];
            if (actual.getClass().equals(tipo)) {
                toret = actual;
            }
        }
        
        return toret;
    }
    
    /**
     * Obtiene todos los parámetros de configuración del plugin.
     *
     * @return Los parámetros de configuración del plugin. Si no hay, devuelve
     * una colección vacía.
     * @throws IllegalStateException Si se llama antes de que la configuración
     * del plugin sea inicializada correctamente.
     */
    public static Collection<ParametroConfiguracion<?>> get() throws IllegalStateException {
        if (plugin == null) {
            throw new IllegalStateException("La configuración no ha sido inicializada");
        }
        
        return Arrays.asList(PARAMETROS);
    }
    
    /**
     * Recarga la configuración del plugin desde disco.
     *
     * @throws IllegalArgumentException Si se carece de una configuración de
     * plugin válida que recargar.
     */
    public static void recargar() throws IllegalArgumentException {
        ParametroConfiguracion<?>[] antiguosParametros;
        if (plugin != null) {
            antiguosParametros = Arrays.copyOf(PARAMETROS, PARAMETROS.length);
            
            try {
                plugin.reloadConfig();
                inicializar(plugin);
            } catch (IllegalArgumentException exc) {
                // Restaurar parámetros guardados previamente para asegurarse de que el plugin queda en un estado consistente
                for (int i = 0; i < PARAMETROS.length; ++i) {
                    PARAMETROS[i] = antiguosParametros[i];
                }
                throw exc;
            }
        }
    }
    
    /**
     * Guarda los cambios de configuración del plugin a memoria secundaria.
     */
    public static void guardar() {
        if (plugin != null) {
            getServer().getLogger().info("[TiempoReal] Se guarda configuración del plugin al disco");
            
            // Reflejar cambios en archivo
            for (int i = 0; i < PARAMETROS.length; ++i) {
                ParametroConfiguracion<?> actual = PARAMETROS[i];
                plugin.getConfig().set(actual.getRutaConfiguracion(), actual.getValorYaml());
            }
            
            plugin.saveConfig();
        }
    }
    
    /**
     * Inicializa el parámetro de índice especificado al valor que tenga
     * guardado en la configuración del plugin.
     *
     * @param i El índice del parámetro.
     * @param plugin El plugin del que leer la configuración.
     * @throws IllegalArgumentException Si algún parámetro de configuración
     * leído es inválido.
     */
    private static void inicializarParametro(int i, Plugin plugin) throws IllegalArgumentException {
        ParametroConfiguracion<?> param;
        Object valor = null;
        
        try {
            switch (i) {
                case 0: // "Mundos en los que sincronizar la hora"
                    List<String> valor0 = plugin.getConfig().getStringList(DATOS_PARAMETROS[i][DATO_RUTA]);

                    // Convertir lista de nombres obtenida desde la configuración a conjunto de mundos
                    Set<World> toset = new HashSet<>();
                    for (String nombreMundo : valor0) {
                        toset.add(getServer().getWorld(nombreMundo));
                    }

                    param = new ParametroConfiguracion.MundosSincronizacion(DATOS_PARAMETROS[i][DATO_RUTA], DATOS_PARAMETROS[i][DATO_NOMBRE_COD], DATOS_PARAMETROS[i][DATO_PERMISO], toset);
                    valor = valor0;
                    break;
                case 1: // "Texto de hora en reloj"
                    String valor1 = plugin.getConfig().getString(DATOS_PARAMETROS[i][DATO_RUTA]);
                    param = new ParametroConfiguracion.TextoHora(DATOS_PARAMETROS[i][DATO_RUTA], DATOS_PARAMETROS[i][DATO_NOMBRE_COD], DATOS_PARAMETROS[i][DATO_PERMISO], null);
                    param.setValor(valor1); // Para asegurarse de que se interpretan correctamente códigos de formato. El constructor no realiza las operaciones necesarias
                    valor = valor1;
                    break;
                default:
                    param = null;
                    break;
            }
        } catch (IllegalArgumentException exc) {
            throw exc;
        }
        
        if (param != null) {
            if (!param.valorValido()) {
                throw new IllegalArgumentException("El valor del parámetro de configuración " + i + " no es válido (" + valor + ")");
            }
            PARAMETROS[i] = param;
        }
    }
}