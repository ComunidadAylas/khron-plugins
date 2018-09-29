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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import static org.bukkit.Bukkit.getLogger;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * Implementa la lógica de negocio de los comandos del plugin.
 *
 * @author AlexTMjugador
 */
final class LogicaComandos implements TabExecutor {
    /**
     * El comando para establecer parámetros de configuración. Debe de coincidir
     * con su definición en plugin.yml.
     */
    private static final String COMANDO_SET_CONFIG = "trconfig";
    /**
     * El comando para recargar la configuración desde disco. Debe de coincidir
     * con su definición en plugin.yml.
     */
    private static final String COMANDO_RECARGAR_CONFIG = "trrecargarconfig";

    /**
     * Implementa la lógica de negocio de los comandos del plugin.
     *
     * @param sender El responsable de enviar el comando.
     * @param command El comando que ha enviado.
     * @param label La etiqueta que se usó para enviar el comando.
     * @param args Los argumentos con los que se llamaron al comando.
     * @return Verdadero si el comando se ha ejecutado correctamente, falso si
     * ocurre algún error con los argumentos o su sintaxis.
     */
    @Override
    @SuppressWarnings("null")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean toret = seDebeAtender(command, sender, args.length);

        if (toret) {
            switch (command.getName().toLowerCase()) {
                case COMANDO_SET_CONFIG:
                    String codigoParam = args[0];
                    String valor = arrayAString(args, 1);
                    ParametroConfiguracion<?> param = null;
                    Collection<ParametroConfiguracion<?>> params = Configuracion.get();
                    Iterator<ParametroConfiguracion<?>> iter = params.iterator();

                    toret = false;
                    while (iter.hasNext() && !toret) {
                        param = iter.next();
                        toret = param.getNombreCodigo().equalsIgnoreCase(codigoParam);
                    }

                    if (toret && param.puedeCambiarlo(sender)) {
                        if (param.setValor(valor)) {
                            sender.sendMessage(ChatColor.GREEN + "Has cambiado el valor de la configuración \"" + param.getRutaConfiguracion() + "\" con éxito.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "El valor especificado para esa configuración no es válido. Consulta la documentación del plugin para más información.");
                        }
                    }
                    
                    break;
                case COMANDO_RECARGAR_CONFIG:
                    try {
                        Configuracion.recargar();
                        sender.sendMessage(ChatColor.GREEN + "Se ha recargado la configuración del plugin desde disco con éxito.");
                    } catch (IllegalArgumentException exc) {
                        sender.sendMessage(ChatColor.RED + "Ha ocurrido un error recargando la configuración del plugin. Esto puede ser debido a valores inválidos. Se muestra más información sobre el error por consola.");
                        getLogger().warning(exc.getMessage());
                    }
                    
                    break;
                default:
                    // Este caso no debería de ocurrir
                    assert(false);
                    break;
            }
        }

        return toret;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> toret = new LinkedList<>();

        // args.length == 1 porque solo nos interesa completar el parámetro de configuración
        if (seDebeAtender(command, sender) && command.getName().equalsIgnoreCase(COMANDO_SET_CONFIG) && args.length == 1) {
            Collection<ParametroConfiguracion<?>> params = Configuracion.get();
            
            for (ParametroConfiguracion param : params) {
                if (param.puedeCambiarlo(sender)) {
                    toret.add(param.getNombreCodigo());
                }
            }
        }

        return toret;
    }

    /**
     * Comprueba si un determinado comando, enviado por el responsable
     * especificado, debe de ser tenido en cuenta para esta lógica de negocio.
     *
     * @param cmd El comando a comprobar.
     * @param snd El responsable de enviar el comando a comprobar.
     * @param nargs El número de argumentos que el responsable de enviar el
     * comando especificó.
     * @return Verdadero si debe de ser tenido en cuenta, falso en caso
     * contrario.
     */
    private static boolean seDebeAtender(Command cmd, CommandSender snd, int nargs) {
        boolean formaCorrecta = seDebeAtender(cmd, snd);
        boolean argsCorrectos;

        switch (cmd.getName().toLowerCase()) {
            case COMANDO_SET_CONFIG:
                argsCorrectos = nargs >= 2;
                break;
            case COMANDO_RECARGAR_CONFIG:
            default:
                // Se ignoran, cualquier número sirve
                argsCorrectos = true;
                break;
        }

        return formaCorrecta && argsCorrectos;
    }
    
    /**
     * Comprueba si un determinado comando, enviado por el responsable
     * especificado, debe de ser tenido en cuenta para esta lógica de negocio,
     * ignorando el número de argumentos especificados.
     *
     * @param cmd El comando a comprobar.
     * @param snd El responsable de enviar el comando a comprobar.
     * @return Verdadero si debe de ser tenido en cuenta, falso en caso
     * contrario.
     */
    private static boolean seDebeAtender(Command cmd, CommandSender snd) {
        boolean emisorCorrecto = snd instanceof Player || snd instanceof ConsoleCommandSender;
        boolean nombreCorrecto = cmd.getName().equalsIgnoreCase(COMANDO_SET_CONFIG) || cmd.getName().equalsIgnoreCase(COMANDO_RECARGAR_CONFIG);

        return emisorCorrecto && nombreCorrecto;
    }
    
    /**
     * Concatena todas las cadenas de texto contenidas en un array, separándolas
     * por un espacio en blanco.
     *
     * @param arr El array a concatenar.
     * @param i El índice del array a partir del cual concatenar sus siguientes,
     * inclusive.
     * @return La susodicha cadena concatenada.
     */
    private static String arrayAString(String[] arr, int i) {
        StringBuilder toret = new StringBuilder();
        
        for (int j = i; j < arr.length; ++j) {
            if (j != i) {
                toret.append(" ");
            }
            toret.append(arr[j]);
        }
        
        return toret.toString();
    }
}
