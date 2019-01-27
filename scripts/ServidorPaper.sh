#!/bin/bash
# Plugins de Spigot del Proyecto Khron
# Copyright (C) 2019 Comunidad Aylas
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# El URL desde el que se descargará el JAR principal de Paper
readonly URL_DESCARGA_PAPER=https://papermc.io/ci/job/Paper-1.13/lastSuccessfulBuild/artifact/paperclip.jar

function instalarPaper {
	# Directorio donde residirá el servidor de Paper
	mkdir -p tasks/servidor-paper
	cd tasks/servidor-paper

	# Propagar error que pudiese haber ocurrido con el sistema de ficheros
	if [ $? -ne 0 ]; then
		return $?
	fi

	# Si paperclip.jar existe y es un fichero de tamaño menor de 40 MiB, asumir que es una descarga parcial
	# incorrecta y descartarla
	if [[ -f paperclip.jar && `wc -c paperclip.jar | cut -d" " -f1` -lt 41943040 ]]; then
		rm paperclip.jar
	fi

	# Si tenemos un paperclip.jar, solo lo descargaremos si el ofrecido por el servidor es más reciente que el que tenemos
	echo "> Descargando Paper..."
	curl -L -z paperclip.jar -o paperclip.jar -w "%{http_code}" $URL_DESCARGA_PAPER > codigo_http

	# Propagar error que pudiese haber ocurrido durante la descarga
	if [ $? -ne 0 ]; then
		rm codigo_http
		cd ../..
		return $?
	fi

	# Si acabamos de descargar el fichero paperclip.jar, realizar tareas de configuración inicial del servidor
	if [ `cat codigo_http` = 200 ]; then
		rm codigo_http
		echo "> Configurando servidor Paper..."
		# Aceptar EULA
		echo 'eula=true' > eula.txt
		# Aplicar configuraciones predeterminadas del servidor
		(cat <<SERVER_PROPERTIES
#Minecraft server properties
generator-settings=
force-gamemode=false
allow-nether=true
enforce-whitelist=false
gamemode=0
broadcast-console-to-ops=true
enable-query=false
player-idle-timeout=0
difficulty=0
spawn-monsters=true
op-permission-level=4
pvp=true
snooper-enabled=false
level-type=DEFAULT
hardcore=false
enable-command-block=true
max-players=2
network-compression-threshold=65536
resource-pack-sha1=
max-world-size=29999984
server-port=25565
server-ip=127.22.12.98
spawn-npcs=true
allow-flight=false
level-name=world
view-distance=4
resource-pack=
spawn-animals=true
white-list=false
generate-structures=true
online-mode=false
max-build-height=256
level-seed=
prevent-proxy-connections=false
motd=Servidor de depuración de plugins de Khron
enable-rcon=false
SERVER_PROPERTIES
) > server.properties
		# Eliminar mensajes verbosos de creación del mundo durante el inicio del servidor
		(cat <<SPIGOT_YML
# This is the main configuration file for Spigot.
# As you can see, there's tons to configure. Some options may impact gameplay, so use
# with caution, and make sure you know what each option does before configuring.
# For a reference for any variable inside this file, check out the Spigot wiki at
# http://www.spigotmc.org/wiki/spigot-configuration/
# 
# If you need help with the configuration or have any questions related to Spigot,
# join us at the IRC or drop by our forums and leave a post.
# 
# IRC: #spigot @ irc.spi.gt ( http://www.spigotmc.org/pages/irc/ )
# Forums: http://www.spigotmc.org/

config-version: 11
settings:
  debug: false
  save-user-cache-on-stop-only: false
  sample-count: 12
  netty-threads: 4
  user-cache-size: 1000
  bungeecord: false
  player-shuffle: 0
  late-bind: false
  timeout-time: 60
  restart-on-crash: true
  restart-script: ./start.sh
  moved-too-quickly-multiplier: 10.0
  moved-wrongly-threshold: 0.0625
  attribute:
    maxHealth:
      max: 2048.0
    movementSpeed:
      max: 2048.0
    attackDamage:
      max: 2048.0
messages:
  whitelist: You are not whitelisted on this server!
  unknown-command: Unknown command. Type "/help" for help.
  server-full: The server is full!
  outdated-client: Outdated client! Please use {0}
  outdated-server: Outdated server! I'm still on {0}
  restart: Server is restarting
stats:
  disable-saving: false
  forced-stats: {}
commands:
  spam-exclusions:
  - /skill
  log: true
  replace-commands:
  - setblock
  - summon
  - testforblock
  - tellraw
  tab-complete: 0
  send-namespaced: true
  silent-commandblock-console: false
advancements:
  disable-saving: false
  disabled:
  - minecraft:story/disabled
world-settings:
  default:
    verbose: false
    enable-zombie-pigmen-portal-spawns: true
    item-despawn-rate: 6000
    view-distance: 4
    arrow-despawn-rate: 1200
    wither-spawn-sound-radius: 0
    nerf-spawner-mobs: false
    hanging-tick-frequency: 100
    zombie-aggressive-towards-villager: true
    mob-spawn-range: 8
    random-light-updates: false
    hopper-amount: 1
    max-tnt-per-tick: 100
    dragon-death-sound-radius: 0
    seed-village: 10387312
    seed-desert: 14357617
    seed-igloo: 14357618
    seed-jungle: 14357619
    seed-swamp: 14357620
    seed-monument: 10387313
    seed-shipwreck: 165745295
    seed-ocean: 14357621
    seed-slime: 987234911
    max-entity-collisions: 8
    merge-radius:
      item: 2.5
      exp: 3.0
    growth:
      cactus-modifier: 100
      cane-modifier: 100
      melon-modifier: 100
      mushroom-modifier: 100
      pumpkin-modifier: 100
      sapling-modifier: 100
      beetroot-modifier: 100
      carrot-modifier: 100
      potato-modifier: 100
      wheat-modifier: 100
      netherwart-modifier: 100
      vine-modifier: 100
      cocoa-modifier: 100
    entity-activation-range:
      animals: 32
      monsters: 32
      misc: 16
      water: 16
      tick-inactive-villagers: true
    hunger:
      jump-walk-exhaustion: 0.05
      jump-sprint-exhaustion: 0.2
      combat-exhaustion: 0.1
      regen-exhaustion: 6.0
      swim-multiplier: 0.01
      sprint-multiplier: 0.1
      other-multiplier: 0.0
    squid-spawn-range:
      min: 45.0
    entity-tracking-range:
      players: 48
      animals: 48
      monsters: 48
      misc: 32
      other: 64
    ticks-per:
      hopper-transfer: 8
      hopper-check: 1
    max-tick-time:
      tile: 50
      entity: 50
SPIGOT_YML
) > spigot.yml
		# Permitir el funcionamiento del comando /restart
		(cat <<SCRIPT_REINICIO
#!/bin/bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar "paperclip.jar"
SCRIPT_REINICIO
) > start.sh
	else
		rm codigo_http
	fi
	
	cd ../..
}

function arrancarPaper {
	cd tasks/servidor-paper
	echo "> Iniciando servidor Paper con soporte para JPDA mediante TCP/IP en el puerto 8000..."
	echo
	echo "**********************************************************************"
	echo "* A PARTIR DE ESTE MOMENTO ES POSIBLE CONECTAR EL DEPURADOR A LA JVM *"
	echo "************************* (Debug Anyway) *****************************"
	echo
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 -jar "paperclip.jar"
}

echo "*************************************************************************************"
echo "* POR FAVOR, NO CONECTES TODAVÍA EL DEPURADOR A LA JVM, PUES ÉSTA NO SE HA INICIADO *"
echo "*************************************************************************************"
echo "Si Visual Studio Code muestra un error acerca de que no se puede vigilar el estado de la tarea (\"The specified task cannot be tracked\"), espera a que arranque la JVM y haz clic en \"Debug Anyway\"."
echo

# Instalar Paper, si no lo está, y si todo va bien abrir el servidor
instalarPaper
if [ $? -eq 0 ]; then
	arrancarPaper
fi
