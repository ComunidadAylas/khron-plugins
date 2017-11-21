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
     * Contiene el agente de sincronización de hora usado por el plugin.
     */
    private static AgenteSincHora agenteHora;
    
    /**
     * Crea los objetos y eventos necesarios para sincronizar el tiempo y
     * extender la funcionalidad de relojes.
     */
    @Override
    public void onEnable() {
        PluginTiempoReal.agenteHora = new AgenteSincHora(this);
        new RelojExtendido(this, PluginTiempoReal.agenteHora);
    }

    /**
     * Detiene apropiadamente otras partes del plugin.
     */
    @Override
    public void onDisable() {
        for (World w : getServer().getWorlds()) {
            agenteHora.onWorldUnload(new WorldUnloadEvent(w));
        }
    }
}