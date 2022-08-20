#!/bin/sh
# Plugins de Paper del Proyecto Khron
# Copyright (C) 2019 Comunidad Aylas
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.

# El URL desde el que se descargará el JAR principal de Paper
readonly URL_DESCARGA_PAPER=https://api.papermc.io/v2/projects/paper/versions/1.19.2/builds/131/downloads/paper-1.19.2-131.jar
# El ejecutable de Java a usar. Si se deja en blanco, se deducirá
# a partir de la variable de entorno PATH
readonly EJECUTABLE_JAVA=

instalarPaper() {
	# Directorio donde residirá el servidor de Paper
	mkdir -p scripts/servidor-paper
	cd scripts/servidor-paper || return $?

	# Si paperclip.jar existe y es un fichero de tamaño menor de 40 MiB, asumir que es una descarga parcial
	# incorrecta y descartarla
	if [ -f paperclip.jar ] && [ "$(wc -c paperclip.jar | cut -d" " -f1)" -lt 41943040 ]; then
		rm paperclip.jar
	fi

	# Si tenemos un paperclip.jar, solo lo descargaremos si el ofrecido por el servidor es más reciente que el que tenemos
	echo "> Descargando Paper..."
	if [ -f 'paperclip.jar' ]; then
		codigo_http=$(curl -L -z paperclip.jar -o paperclip.jar -w "%{http_code}" $URL_DESCARGA_PAPER)
	else
		codigo_http=$(curl -L -o paperclip.jar -w "%{http_code}" $URL_DESCARGA_PAPER)
	fi

	# Propagar error que pudiese haber ocurrido durante la descarga
	if [ $? -ne 0 ]; then
		cd ../..
		return $?
	fi

	# Si acabamos de descargar el fichero paperclip.jar, realizar tareas de configuración inicial del servidor
	if [ "$codigo_http" -ge 200 ] && [ "$codigo_http" -lt 400 ]; then
		echo "> Configurando servidor Paper..."
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
server-ip=127.0.0.1
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
		# También desactiva el watchdog que reinicia el servidor si tras 60 s no responde,
		# estableciendo el valor de timeout-time
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
  timeout-time: 2147483647
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
      tile: 2147483647
      entity: 2147483647
SPIGOT_YML
) > spigot.yml
		# Evitar que Paper llene la salida del servidor de información de traza en caso de que el servidor se congele,
		# pues ello va a pasar cuando se esté usando el depurador, y es bastante molesto
		(cat <<PAPER_YML
# This is the main configuration file for Paper.
# As you can see, there's tons to configure. Some options may impact gameplay, so use
# with caution, and make sure you know what each option does before configuring.
#
# If you need help with the configuration or have any questions related to Paper,
# join us in our Discord or IRC channel.
#
# Discord: https://paperdiscord.emc.gs
# IRC: #paper @ irc.spi.gt ( http://irc.spi.gt/iris/?channels=paper )
# Website: https://papermc.io/
# Docs: https://paper.readthedocs.org/

verbose: false
config-version: 17
settings:
  player-auto-save-rate: -1
  max-player-auto-save-per-tick: -1
  enable-player-collisions: true
  sleep-between-chunk-saves: false
  use-versioned-world: false
  region-file-cache-size: 256
  save-empty-scoreboard-teams: false
  incoming-packet-spam-threshold: 300
  bungee-online-mode: true
  save-player-data: true
  queue-light-updates-max-loss: 10
  suggest-player-names-when-null-tab-completions: true
  use-alternative-luck-formula: false
  load-permissions-yml-before-plugins: true
  async-chunks:
    enable: true
    generation: true
    thread-per-world-generation: true
    load-threads: -1
  velocity-support:
    enabled: false
    online-mode: false
    secret: ''
  watchdog:
    early-warning-every: 2147483647
    early-warning-delay: 2147483647
  book-size:
    page-max: 2560
    total-multiplier: 0.98
  spam-limiter:
    tab-spam-increment: 1
    tab-spam-limit: 500
timings:
  enabled: true
  verbose: true
  server-name-privacy: false
  hidden-config-entries:
  - database
  - settings.bungeecord-addresses
  history-interval: 300
  history-length: 3600
messages:
  no-permission: '&cI''m sorry, but you do not have permission to perform this command.
    Please contact the server administrators if you believe that this is in error.'
  kick:
    connection-throttle: Connection throttled! Please wait before reconnecting.
    authentication-servers-down: ''
    flying-player: Flying is not enabled on this server
    flying-vehicle: Flying is not enabled on this server
world-settings:
  default:
    disable-thunder: false
    keep-spawn-loaded-range: 4
    auto-save-interval: -1
    keep-spawn-loaded: true
    disable-ice-and-snow: false
    skeleton-horse-thunder-spawn-chance: 0.01
    armor-stands-do-collision-entity-lookups: true
    fire-physics-event-for-redstone: false
    skip-entity-ticking-in-chunks-scheduled-for-unload: true
    allow-non-player-entities-on-scoreboards: false
    nether-ceiling-void-damage: false
    water-over-lava-flow-speed: 5
    use-faster-eigencraft-redstone: false
    container-update-tick-rate: 1
    parrots-are-unaffected-by-player-movement: false
    disable-explosion-knockback: false
    non-player-arrow-despawn-rate: -1
    creative-arrow-despawn-rate: -1
    prevent-tnt-from-moving-in-water: false
    grass-spread-tick-rate: 1
    bed-search-radius: 1
    portal-search-radius: 128
    fixed-chunk-inhabited-time: -1
    delay-chunk-unloads-by: 10s
    queue-light-updates: false
    max-auto-save-chunks-per-tick: 24
    enable-treasure-maps: true
    treasure-maps-return-already-discovered: false
    optimize-explosions: false
    use-vanilla-world-scoreboard-name-coloring: false
    max-chunk-gens-per-tick: 10
    prevent-moving-into-unloaded-chunks: false
    save-queue-limit-for-auto-save: 50
    max-chunk-sends-per-tick: 81
    armor-stands-tick: true
    disable-teleportation-suffocation-check: false
    experience-merge-max-value: -1
    remove-corrupt-tile-entities: false
    falling-block-height-nerf: 0
    tnt-entity-height-nerf: 0
    spawner-nerfed-mobs-should-jump: false
    baby-zombie-movement-speed: 0.5
    allow-leashing-undead-horse: false
    all-chunks-are-slime-chunks: false
    mob-spawner-tick-rate: 1
    duplicate-uuid-resolver: saferegen
    duplicate-uuid-saferegen-delete-range: 32
    max-entity-collisions: 8
    filter-nbt-data-from-spawn-eggs-and-related: true
    disable-creeper-lingering-effect: false
    anti-xray:
      enabled: false
      engine-mode: 1
      chunk-edge-mode: 2
      max-chunk-section-index: 3
      update-radius: 2
      hidden-blocks:
      - gold_ore
      - iron_ore
      - coal_ore
      - lapis_ore
      - mossy_cobblestone
      - obsidian
      - chest
      - diamond_ore
      - redstone_ore
      - lit_redstone_ore
      - clay
      - emerald_ore
      - ender_chest
      replacement-blocks:
      - stone
      - planks
    game-mechanics:
      disable-chest-cat-detection: false
      disable-end-credits: false
      disable-player-crits: false
      shield-blocking-delay: 5
      disable-sprint-interruption-on-attack: false
      villages-load-chunks: false
      scan-for-legacy-ender-dragon: true
      disable-unloaded-chunk-enderpearl-exploit: true
    max-growth-height:
      cactus: 3
      reeds: 3
    lightning-strike-distance-limit:
      sound: -1
      impact-sound: -1
      flash: -1
    despawn-ranges:
      soft: 32
      hard: 128
    fishing-time-range:
      MinimumTicks: 100
      MaximumTicks: 600
    frosted-ice:
      enabled: true
      delay:
        min: 20
        max: 40
    squid-spawn-height:
      maximum: 0.0
    hopper:
      cooldown-when-full: true
      disable-move-event: false
    lootables:
      auto-replenish: false
      restrict-player-reloot: true
      reset-seed-on-fill: true
      max-refills: -1
      refresh-min: 12h
      refresh-max: 2d
PAPER_YML
) > paper.yml
		# Permitir el funcionamiento del comando /restart
		(cat <<SCRIPT_REINICIO
#!/bin/bash
echo Por favor, vuelve a ejecutar el depurador en la configuración de servidor de Paper externo.
sleep 7
exit
SCRIPT_REINICIO
) > start.sh
        # Aumentar el nivel de depuración de los logs
        (cat <<CONFIGURACION_LOG4J
<?xml version="1.0" encoding="UTF-8"?>
<!-- log4j2.xml basado en el incluido en una versión de Paper para Minecraft 1.16.3 -->
<!-- Más información en: https://www.reddit.com/r/admincraft/comments/69271l/guide_controlling_console_and_log_output_with/ -->
<Configuration status="WARN" packages="com.mojang.util" shutdownHook="disable" monitorInterval="15">
    <Appenders>
        <Queue name="ServerGuiConsole">
            <PatternLayout pattern="[%d{HH:mm:ss} %level]: %msg%n" />
        </Queue>
        <TerminalConsole name="TerminalConsole">
            <PatternLayout>
                <LoggerNamePatternSelector defaultPattern="%highlightError{[%d{HH:mm:ss} %level]: [%logger] %minecraftFormatting{%msg}%n%xEx{full}}">
                    <!-- Log root, Minecraft, Mojang and Bukkit loggers without prefix -->
                    <!-- Disable prefix for various plugins that bypass the plugin logger -->
                    <PatternMatch key=",net.minecraft.,Minecraft,com.mojang.,com.sk89q.,ru.tehkode.,Minecraft.AWE"
                                  pattern="%highlightError{[%d{HH:mm:ss} %level]: %minecraftFormatting{%msg}%n%xEx{full}}" />
                </LoggerNamePatternSelector>
            </PatternLayout>
        </TerminalConsole>
        <RollingRandomAccessFile name="File" fileName="logs/latest.log" filePattern="logs/%d{yyyy-MM-dd}-%i.log.gz">
            <PatternLayout>
                <LoggerNamePatternSelector defaultPattern="[%d{HH:mm:ss}] [%t/%level]: [%logger] %minecraftFormatting{%msg}{strip}%n%xEx{full}">
                    <!-- Log root, Minecraft, Mojang and Bukkit loggers without prefix -->
                    <!-- Disable prefix for various plugins that bypass the plugin logger -->
                    <PatternMatch key=",net.minecraft.,Minecraft,com.mojang.,com.sk89q.,ru.tehkode.,Minecraft.AWE"
                                  pattern="[%d{HH:mm:ss}] [%t/%level]: %minecraftFormatting{%msg}{strip}%n%xEx{full}" />
                </LoggerNamePatternSelector>
            </PatternLayout>
            <Policies>
                <TimeBasedTriggeringPolicy />
                <OnStartupTriggeringPolicy />
            </Policies>
            <DefaultRolloverStrategy max="1000"/>
        </RollingRandomAccessFile>
    </Appenders>
    <Loggers>
        <Root level="ALL">
            <filters>
                <MarkerFilter marker="NETWORK_PACKETS" onMatch="DENY" onMismatch="NEUTRAL" />
            </filters>
            <AppenderRef ref="File"/>
            <AppenderRef ref="TerminalConsole"/>
            <AppenderRef ref="ServerGuiConsole"/>
        </Root>
    </Loggers>
</Configuration>
CONFIGURACION_LOG4J
) > log4j2.xml
	fi

	cd ../..
}

arrancarPaper() {
	cd scripts/servidor-paper || return $?
	echo "> Iniciando servidor Paper con soporte para JPDA mediante TCP/IP en el puerto 8000..."
	echo
	echo "************************************************************************"
	echo "* CONECTAR EL DEPURADOR A LA JVM CUANDO SE INDIQUE QUE ESTÁ ESCUCHANDO *"
	echo "************************************************************************"
	echo
	"${EJECUTABLE_JAVA:-java}" -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000 -Dcom.mojang.eula.agree=true -Dlog4j.configurationFile="log4j2.xml" -Xmx1G -jar "paperclip.jar" nogui
	cd ../..
	return $?
}

copiarPlugins() {
	# Crear directorio donde residirán los plugins del servidor de Paper si hace falta
	if [ ! -d "scripts/servidor-paper/plugins" ]; then
		echo "> Creando directorio de plugins del servidor..."

		# Propagar error que pudiese haber ocurrido con el sistema de ficheros
		if ! mkdir -p scripts/servidor-paper/plugins; then
			return $?
		fi
	fi

	# Copiar ficheros JAR en el directorio de salida, si hay
	if [ -d "jar" ]; then
		echo "> Limpiando directorio de plugins del servidor..."

		# Avisar de los errores que pudiesen haber ocurrido
		if ! rm -rf tasks/servidor-paper/plugins/*; then
			echo "! No se ha podido limpiar el directorio de plugins del servidor de ficheros existentes. Es posible que la depuración parta de un estado inconsistente o difícilmente reproducible."
		fi

		echo "> Copiando plugins empaquetados al directorio de plugins del servidor..."

		if ! cp jar/*.jar scripts/servidor-paper/plugins; then
			echo "! Ha ocurrido un error al copiar los plugins empaquetados al directorio de plugins del servidor. Es muy posible que no se hayan colocado y, por tanto, no se carguen."
		fi
	fi
}

echo "*************************************************************************************"
echo "* POR FAVOR, NO CONECTES TODAVÍA EL DEPURADOR A LA JVM, PUES ÉSTA NO SE HA INICIADO *"
echo "*************************************************************************************"
echo

# Instalar Paper, si no lo está, y si todo va bien abrir el servidor
if instalarPaper; then
	copiarPlugins
	if (! arrancarPaper) && [ -f "scripts/servidor-paper/paperclip.jar" ]; then
		echo "! Ha ocurrido un error arrancando el servidor de Paper. Esto puede deberse a un fichero paperclip.jar corrupto. ¿Quieres descargarlo de nuevo? (S/N) "
		read -r r
		case "$r" in
			S|s)
				clear
				rm scripts/servidor-paper/paperclip.jar
				if instalarPaper; then
					copiarPlugins
					arrancarPaper
				fi;;
			*)
				;;
		esac
	fi
fi
