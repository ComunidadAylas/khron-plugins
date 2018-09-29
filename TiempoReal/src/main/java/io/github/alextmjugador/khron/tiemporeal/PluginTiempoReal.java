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

import java.util.Set;
import static org.bukkit.Bukkit.getPluginManager;
import org.bukkit.World;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Implementa un plugin que sincroniza el tiempo real del servidor con el tiempo
 * de todos los mundos del juego.
 *
 * @author AlexTMjugador
 */
public final class PluginTiempoReal extends JavaPlugin {
    /**
     * Almacena si el plugin ha sido inicializado con éxito o no.
     */
    private static boolean inicializado = false;
    
    /**
     * Crea los objetos y eventos necesarios para sincronizar el tiempo y
     * extender la funcionalidad de relojes.
     */
    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void onEnable() {
        try {
            // Cargar configuraciones
            this.saveDefaultConfig();   // Necesario para que se lea correctamente la primera vez que se ejecuta el plugin
            Configuracion.inicializar(this);
            
            // Añadir comandos del plugin
            LogicaComandos lc = new LogicaComandos();
            for (String comando : this.getDescription().getCommands().keySet()) {
                getCommand(comando).setExecutor(lc);
                getCommand(comando).setTabCompleter(lc);
            }
            
            // Comenzar acciones del plugin
            new AgenteSincHora();
            new RelojExtendido();
            
            inicializado = true;
        } catch (IllegalArgumentException exc) {
            getServer().getLogger().severe("[TiempoReal] La configuración del plugin es inválida. Se detiene su ejecución. Detalles:");
            getServer().getLogger().severe(exc.getMessage());
            getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Detiene apropiadamente otras partes del plugin.
     */
    @Override
    public void onDisable() {
        if (inicializado) {
            AgenteSincHora ash = AgenteSincHora.getInstancia();

            @SuppressWarnings("unchecked")
            Set<World> mundosSinc = (Set<World>) Configuracion.get(ParametroConfiguracion.MundosSincronizacion.class).getValor();

            for (World w : mundosSinc) {
                ash.onWorldUnload(new WorldUnloadEvent(w));
            }
        }
    }
}