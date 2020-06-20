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

import static org.bukkit.Bukkit.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * Implementa la lógica de negocio de los comandos de gestión de configuración
 * de un plugin.
 *
 * @author AlexTMjugador
 */
public final class ComandosConfiguracion implements TabExecutor {
    /**
     * El comando para establecer parámetros de configuración. Debe de coincidir con
     * su definición en plugin.yml.
     */
    private final String comandoEstablecer;
    /**
     * El comando para recargar la configuración desde disco. Debe de coincidir con
     * su definición en plugin.yml.
     */
    private final String comandoRecargar;
    /**
     * Los parámetros de configuración que son gestionados por este comando.
     */
    private final ParametroConfiguracion<?, ?>[] params;

    /**
     * Relaciona este ejecutador de comandos con los comandos que maneja y los
     * parámetros de configuración que debe de permitir gestionar.
     * 
     * @param comandoEstablecer El comando que un usuario podrá emplear para
     *                          establecer los parámetros de configuración de un
     *                          plugin.
     * @param comandoRecargar   El comando que un usuario podrá emplear para
     *                          recargar desde disco los parámetros de configuración
     *                          de un plugin.
     * @param params            Los parámetros que se pueden gestionar con los
     *                          comandos especificados anteriormente.
     * @throws IllegalArgumentException Si alguno de los parámetros pasados al
     *                                  constructor es nulo.
     */
    public ComandosConfiguracion(
        String comandoEstablecer, String comandoRecargar, ParametroConfiguracion<?, ?>... params
    ) {
        if (comandoEstablecer == null || comandoRecargar == null || params == null) {
            throw new IllegalArgumentException(
                "No se puede crear un ejecutador de comandos de configuración de un plugin sin información de qué comandos o parámetros maneja"
            );
        }

        this.comandoEstablecer = comandoEstablecer;
        this.comandoRecargar = comandoRecargar;
        this.params = params;
    }

    /**
     * Implementa la lógica de negocio de los comandos del plugin.
     *
     * @param sender  El responsable de enviar el comando.
     * @param command El comando que ha enviado.
     * @param label   La etiqueta que se usó para enviar el comando.
     * @param args    Los argumentos con los que se llamaron al comando.
     * @return Verdadero si el comando se ha ejecutado correctamente, falso si
     *         ocurre algún error con los argumentos o su sintaxis.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean toret = seDebeAtender(command, sender);

        if (toret) {
            String nombreComando = command.getName();

            if (nombreComando.equals(comandoEstablecer)) {
                String idParam = args[0];
                String valor = arrayAString(args, 1);
                ParametroConfiguracion<?, ?> param = null;
                int i = 0;

                toret = false;
                while (i < params.length && !toret) {
                    param = params[i++];
                    toret = param.getId().equalsIgnoreCase(idParam);
                }

                if (toret && param.puedeCambiarlo(sender)) {
                    if (param.parsearValor(valor)) {
                        sender.sendMessage(
                            ChatColor.GREEN + "Has cambiado el valor de la configuración \"" +
                            param.getRutaConfiguracion() + "\" con éxito."
                        );
                    } else {
                        sender.sendMessage(
                            ChatColor.RED + "El valor especificado para esa configuración no es válido. " +
                            "Consulta la documentación del plugin para más información."
                        );
                    }
                }
            } else if (nombreComando.equals(comandoRecargar)) {
                try {
                    boolean configuracionRecargada = false;
                    for (ParametroConfiguracion<?, ?> p : params) {
                        // Solo recargar la configuración una vez, pues todos los parámetros son del mismo plugin
                        if (!configuracionRecargada) {
                            p.recargarConfiguracion();
                            configuracionRecargada = true;
                        }
                        p.leer();
                    }

                    sender.sendMessage(ChatColor.GREEN + "Se ha recargado la configuración del plugin con éxito.");
                } catch (IllegalArgumentException | ClassCastException exc) {
                    sender.sendMessage(
                        ChatColor.RED + "Ha ocurrido un error recargando la configuración del plugin. " +
                        "Esto puede ser debido a valores inválidos. Se muestra más información sobre el error por consola."
                    );

                    getLogger().warning(exc.getMessage());
                }
            }
        }

        return toret;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> toret = new ArrayList<>(params.length);

        // args.length == 1 porque solo nos interesa completar el parámetro de
        // configuración
        if (seDebeAtender(command, sender) && command.getName().equals(comandoEstablecer) && args.length == 1) {
            for (ParametroConfiguracion<?, ?> param : params) {
                if (param.puedeCambiarlo(sender)) {
                    toret.add(param.getId());
                }
            }
        }

        return toret;
    }

    /**
     * Comprueba si un determinado comando, enviado por el responsable especificado,
     * debe de ser tenido en cuenta para esta lógica de negocio, ignorando el número
     * de argumentos especificados.
     *
     * @param cmd El comando a comprobar.
     * @param snd El responsable de enviar el comando a comprobar.
     * @return Verdadero si debe de ser tenido en cuenta, falso en caso contrario.
     */
    private boolean seDebeAtender(Command cmd, CommandSender snd) {
        return
            (snd instanceof Player || snd instanceof ConsoleCommandSender || snd instanceof RemoteConsoleCommandSender) &&
            (cmd.getName().equals(comandoEstablecer) || cmd.getName().equals(comandoRecargar));
    }

    /**
     * Concatena todas las cadenas de texto contenidas en un array, separándolas por
     * un espacio en blanco.
     *
     * @param arr El array a concatenar.
     * @param i   El índice del array a partir del cual concatenar sus siguientes,
     *            inclusive.
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