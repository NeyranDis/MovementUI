package top.eternal.neyran.movementUI

import top.eternal.neyran.movementUI.events.ProtocolListener
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.plugin.ServicePriority
import org.bukkit.entity.ArmorStand
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import top.eternal.neyran.movementUI.api.MovementUI_API
import top.eternal.neyran.movementUI.events.PlayerListener
import top.eternal.neyran.movementUI.managers.*
import top.eternal.neyran.movementUI.utils.ChatUtils.toMiniMessageComponent

class MovementsMain : JavaPlugin() {
    var vers = "1.1.5"
    val playerStates: MutableMap<String, PlayerState> = mutableMapOf()

    lateinit var conditionsManager: ConditionManager
    lateinit var executiveManager: ExecutiveManager
    lateinit var commandsManager: CommandsManager
    lateinit var configManager: ConfigManager
    fun key(name: String): NamespacedKey {
        return NamespacedKey(this, name)
    }
    lateinit var api: MovementUI_API
    override fun onEnable() {
        conditionsManager = ConditionManager(this)
        executiveManager = ExecutiveManager(this)
        commandsManager = CommandsManager(this)
        configManager = ConfigManager(this)
        saveDefaultConfig()

        updateSettingsVersion("$vers")

        val protocolListener = ProtocolListener(this)
        protocolListener.registerPacketListeners()
        server.pluginManager.registerEvents(PlayerListener(this), this)
        PlaceholderHook(this).register()
        this.getCommand("movementui")?.setTabCompleter(MovementTabCompleter(configManager.configFile))
        api = MovementUI_API(this)

        Bukkit.getServicesManager().register(MovementUI_API::class.java, api, this, ServicePriority.Normal)
        configManager.loadLanguage()
        getCommand("movementui")?.setExecutor(CommandsManager(this))
        logger.info(" \n" +
                "  __  __                                     _   _    _ _____ \n" +
                " |  \\/  |                                   | | | |  | |_   _|     MovementUI: ${vers}\n" +
                " | \\  / | _____   _____ _ __ ___   ___ _ __ | |_| |  | | | |       Build Data: 2025/3/4-18:38\n" +
                " | |\\/| |/ _ \\ \\ / / _ \\ '_ ` _ \\ / _ \\ '_ \\| __| |  | | | |       Author: Neyran\n" +
                " | |  | | (_) \\ V /  __/ | | | | |  __/ | | | |_| |__| |_| |_ \n" +
                " |_|  |_|\\___/ \\_/ \\___|_| |_| |_|\\___|_| |_|\\__|\\____/|_____|\n" +
                "                                                              \n" +
                "                                                              ")
    }
    override fun onDisable() {
        playerStates.clear()
    }
    private fun updateSettingsVersion(newVersion: String) {
        val currentVersion = configManager.settingsConfig.getString("version")

        if (currentVersion != newVersion) {
            configManager.settingsConfig.set("version", newVersion)
            configManager.settingsConfig.save(configManager.settingsFile)
            logger.info("[MovementUI] Updated config.yml version to $newVersion")
        }
    }
    fun startNavigation(player: Player, menuName: String) {
        val state = playerStates.getOrPut(player.name) { PlayerState() }

        if (!state.navigationMode) {
            if (configManager.settingsConfig.getBoolean("air_fix", true) && !player.isFlying) {
                val blockBelow = player.location.add(0.0, -1.0, 0.0).block
                if (blockBelow.type.isAirCompatible()) {
                    closeNavigation(player)
                    return
                }
            }

            val menuConfig = configManager.customConfig.getConfigurationSection(menuName)
            val origin = menuConfig?.getString("origin", "0 0 0")
                ?.split(" ")
                ?.mapNotNull { it.toIntOrNull() }
                ?: listOf(0, 0, 0)

            val standSpawn = menuConfig?.getBoolean("stand_spawn", true) ?: true

            state.x = origin.getOrElse(0) { 0 }
            state.y = origin.getOrElse(1) { 0 }
            state.z = origin.getOrElse(2) { 0 }
            state.currentMenu = menuName

            state.navigationMode = true

            if (standSpawn) {
                val world = player.world
                val location = player.location.clone().apply { y += 0.6 }

                val armorStand = world.spawn(location, ArmorStand::class.java).apply {
                    isVisible = false
                    isSmall = true
                    isMarker = true
                    isInvulnerable = true
                    isCustomNameVisible = false
                    setGravity(false)
                    customName = "navigation_stand_${player.uniqueId}"
                    persistentDataContainer.set(key("navigationOwner"), PersistentDataType.STRING, player.uniqueId.toString())
                }

                armorStand.addPassenger(player)

                object : BukkitRunnable() {
                    override fun run() {
                        state.armorStand = armorStand.uniqueId

                        if (configManager.settingsConfig.getBoolean("bind-command", false)) {
                            commandsManager.processBindCommands(player, "bind_enter")
                        }

                        sendDebugMessage(player, configManager.langConfig.getString("debug.navigation.enter") ?: "", mapOf("menu" to state.currentMenu))
                    }
                }.runTaskLater(this, 5L)
            }
        }
    }
    private fun Material.isAirCompatible(): Boolean =
        this == Material.AIR || this == Material.CAVE_AIR || this == Material.VOID_AIR
    fun closeNavigation(player: Player) {
        val state = playerStates.getOrPut(player.name) { PlayerState() }

        if (state.navigationMode) {
            state.apply {
                navigationMode = false
                x = 0
                y = 0
                z = 0
                currentMenu = "default"
                lastKeyPressed = null
                lastMoveTime = null
            }

            (Bukkit.getEntity(state.armorStand ?: return) as? ArmorStand)?.remove()

            state.armorStand = null

            if (configManager.settingsConfig.getBoolean("bind-command", false)) {
                commandsManager.processBindCommands(player, "bind_exit")
            }
        }
    }
    fun updatePlayerCoordinates(player: Player, direction: String) {
        val state = playerStates[player.name] ?: return

        val prevX = state.x
        val prevY = state.y
        val prevZ = state.z

        when (direction) {
            "W" -> state.y += 1
            "S" -> state.y -= 1
            "A" -> state.x -= 1
            "D" -> state.x += 1
            "Space" -> state.z += 1
            "Shift" -> state.z -= 1
        }

        if (!conditionsManager.isCoordinateEnabled(state) || conditionsManager.isCoordinateBlocked(state) || !conditionsManager.isCoordConditionMet(state, player)) {
            state.x = prevX
            state.y = prevY
            state.z = prevZ
            sendDebugMessage(player, "${configManager.langConfig.get("debug.navigation.banned_coords")}")
            return
        }

        val menuSection = configManager.customConfig.getConfigurationSection(state.currentMenu) ?: return

        val nextMenu = menuSection.getKeys(false)
            .mapNotNull { key ->
                if (key == "enabledCoordinates" || key == "blockedCoordinates") return@mapNotNull null

                val section = menuSection.getConfigurationSection(key) ?: return@mapNotNull null
                val targetX = section.getInt("targetX")
                val targetY = section.getInt("targetY")
                val targetZ = section.getInt("targetZ")

                if (state.x == targetX && state.y == targetY && state.z == targetZ) {
                    section.getString("nextMenu")
                } else {
                    null
                }
            }.firstOrNull()

        if (nextMenu != null) {
            val nextMenuSection = configManager.customConfig.getConfigurationSection(nextMenu)
            val permission = nextMenuSection?.getString("permission")
            val conditionsSection = nextMenuSection?.getConfigurationSection("menu_conditions")

            if (permission != null && !player.hasPermission(permission)) {
                state.x = prevX
                state.y = prevY
                state.z = prevZ
                val message = (Component.text()
                    .append(configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                    .append(configManager.langConfig.getString("no.permissions_menu")?.toMiniMessageComponent() ?: Component.text(""))
                    .build())
                player.sendMessage(message)
                return
            }
            if (conditionsSection != null && !conditionsManager.evaluateConditions(conditionsSection, player)) {
                state.x = prevX
                state.y = prevY
                state.z = prevZ
                val message = (Component.text()
                    .append(configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                    .append(configManager.langConfig.getString("no.menu_conditions")?.toMiniMessageComponent() ?: Component.text(""))
                    .build())
                player.sendMessage(message)
                return
            }
        }
        executiveManager.executeCommandForCoordinates(player, state)
        sendDebugMessage(player, formatMessage(configManager.langConfig.getString("debug.navigation.coordinates") ?: "", state))
    }
    private fun formatMessage(message: String, state: PlayerState): String {
        return message.replace("{x}", "${state.x}")
            .replace("{y}", "${state.y}")
            .replace("{z}", "${state.z}"
                .replace("{menu}", "${state.currentMenu}"))
    }
    fun sendMessage(player: Player, message: String, placeholders: Map<String, String>? = null) {
        var formattedMessage = message

        placeholders?.forEach { (key, value) ->
            formattedMessage = formattedMessage.replace("{$key}", value)
        }

        player.sendMessage(formattedMessage)
    }
    fun sendDebugMessage(player: Player, message: String, placeholders: Map<String, String>? = null) {
        if (configManager.settingsConfig.getBoolean("debug", false)) {
            var formattedMessage = message

            placeholders?.forEach { (key, value) ->
                formattedMessage = formattedMessage.replace("{$key}", value)
            }

            player.sendMessage(formattedMessage)
        }
    }
}