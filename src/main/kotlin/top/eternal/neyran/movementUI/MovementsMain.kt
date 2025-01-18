package top.eternal.neyran.movementUI

import ProtocolListener
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.ProtocolLibrary
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.regex.Pattern

class MovementsMain : JavaPlugin() {
    companion object {
        lateinit var instance: MovementsMain
            private set
    }
    var vers = "1.0.7"
    val playerStates: MutableMap<String, PlayerState> = mutableMapOf()
    lateinit var configFile: File
    lateinit var customConfig: FileConfiguration
    lateinit var settingsFile: File
    lateinit var settingsConfig: FileConfiguration
    lateinit var langFile: File
    lateinit var langConfig: FileConfiguration
    lateinit var bindFile: File
    lateinit var bindConfig: FileConfiguration

    fun String.toMiniMessageComponent(): Component {
        val legacyHexPattern = Pattern.compile("&x((&[A-Fa-f0-9]){6})")
        var result = this

        val matcher = legacyHexPattern.matcher(result)
        while (matcher.find()) {
            val fullMatch = matcher.group()
            val hexSequence = fullMatch.replace("&x", "").replace("&", "")
            val formattedCode = hexSequence.chunked(2).joinToString("") { it }
            result = result.replace(fullMatch, "<#${formattedCode}>")
        }
        result = result.replace("&([0-9a-fA-Fk-oK-OrR])".toRegex(), "<$1>")

        return MiniMessage.miniMessage().deserialize(result)
    }
    lateinit var api: MovementUI_API
    override fun onEnable() {
        saveDefaultConfig()
        api = MovementUI_API(this)

        Bukkit.getServicesManager().register(MovementUI_API::class.java, api, this, ServicePriority.Normal)

        settingsFile = File(dataFolder, "config.yml")
        bindFile = File(dataFolder, "bind.yml")
        configFile = File(dataFolder, "menus.yml")
        langFile = File(dataFolder, "lang.yml")

        if (!settingsFile.exists()) {
            saveResource("config.yml", false)
        }
        if (!bindFile.exists()) {
            saveResource("bind.yml", false)
        }
        if (!configFile.exists()) {
            saveResource("menus.yml", false)
        }
        if (!langFile.exists()) {
            saveResource("lang.yml", false)
        }
        customConfig = YamlConfiguration.loadConfiguration(configFile)
        bindConfig = YamlConfiguration.loadConfiguration(bindFile)
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile)
        langConfig = YamlConfiguration.loadConfiguration(langFile)
        updateSettingsVersion("$vers")
        val protocolListener = ProtocolListener(this)
        protocolListener.registerPacketListeners()
        server.pluginManager.registerEvents(PlayerListener(this), this)
        PlaceholderHook(this).register()
        this.getCommand("movementui")?.setTabCompleter(MovementTabCompleter(configFile))
        validateConfig("config.yml", settingsConfig)
        validateConfig("lang.yml", langConfig)
        logger.info(" " +
                "\n" +
                "  __  __                                     _   _    _ _____ \n" +
                " |  \\/  |                                   | | | |  | |_   _|    MovementUI: $vers\n" +
                " | \\  / | _____   _____ _ __ ___   ___ _ __ | |_| |  | | | |      Build Data: 2025/1/17-23:10\n" +
                " | |\\/| |/ _ \\ \\ / / _ \\ '_  _ \\ / _ \\ '_ \\| __| |  | | | |      Author: Neyran\n" +
                " | |  | | (_) \\ V /  __/ | | | | |  __/ | | | |_| |__| |_| |_ \n" +
                " |_|  |_|\\___/ \\_/ \\___|_| |_| |_|\\___|_| |_|\\__|\\____/|_____|\n" +
                "                                                              ")
    }
    override fun onDisable() {
        playerStates.clear()
    }
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("movementui", ignoreCase = true)) {
            if (args.isEmpty()) {
                return true
            }

            when (args[0].lowercase()) {
                "startmenu" -> {
                    if (sender is Player) {
                        val menuName = args.getOrNull(1) ?: settingsConfig.getString("default_menu") ?: "default"
                        val targetPlayerName = args.getOrNull(2)

                        val targetPlayer = if (targetPlayerName != null) {
                            Bukkit.getPlayerExact(targetPlayerName)
                        } else {
                            if (sender is Player) sender else null
                        }

                        if (targetPlayer != null) {
                            if (sender is Player && !sender.hasPermission("movementui.startmenu")) {
                                val message = Component.text()
                                    .append(
                                        langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                            ?: Component.text("")
                                    )
                                    .append(
                                        langConfig.getString("no.permissions")?.toMiniMessageComponent()
                                            ?: Component.text("")
                                    )
                                    .build()
                                sender.sendMessage(message)
                            } else {
                                startNavigation(targetPlayer, menuName)
                            }
                        }
                    }
                }
                "closemenu" -> {
                    if (sender is Player) {
                        val player = sender

                        closeNavigation(player)
                    }
                }

                "reload" -> {
                    if (sender.hasPermission("movementui.reload")) {
                        reloadConfigs()
                        val message = (Component.text()
                            .append(langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                            .append(langConfig.getString("reload.command")?.toMiniMessageComponent() ?: Component.text(""))
                            .build())
                        sender.sendMessage(message)
                    } else {
                        val message = (Component.text()
                            .append(langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                            .append(langConfig.getString("no.permissions")?.toMiniMessageComponent() ?: Component.text(""))
                            .build())
                        sender.sendMessage(message)
                    }
                }
            }
            return true
        }
        return false
    }
    private fun updateSettingsVersion(newVersion: String) {
        val currentVersion = settingsConfig.getString("version")

        if (currentVersion != newVersion) {
            settingsConfig.set("version", newVersion)
            settingsConfig.save(settingsFile)
            logger.info("[MovementUI] Updated config.yml version to $newVersion")
        }
    }
    private fun validateConfig(resourceName: String, fileConfig: FileConfiguration) {
        val defaultConfig = YamlConfiguration.loadConfiguration(getResource(resourceName)?.reader() ?: return)

        defaultConfig.getKeys(true).forEach { key ->
            if (!fileConfig.contains(key)) {
                fileConfig.set(key, defaultConfig.get(key))
                logger.info("[MovementUI] Added missing key '$key' to $resourceName")
            }
        }

        fileConfig.save(File(dataFolder, resourceName))
    }
    fun startNavigation(player: Player, menuName: String) {

        val state = playerStates[player.name] ?: PlayerState().also { playerStates[player.name] = it }
        if (!player.isOnGround) {
            if (player.isFlying) {
                state.navigationMode = true
                return
            } else {
                state.navigationMode = false
                return
            }
        }

        if (state.navigationMode) {
            val message = (Component.text()
                .append(langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                .append(langConfig.getString("navigation.enabled")?.toMiniMessageComponent() ?: Component.text(""))
                .build())
            player.sendMessage(message)
        } else {
            state.navigationMode = true

            val origin = customConfig.getConfigurationSection(menuName)?.getString("origin", "0 0 0")?.split(" ")?.map { it.toInt() } ?: listOf(0, 0, 0)
            state.x = origin[0]
            state.y = origin[1]
            state.z = origin[2]

            state.currentMenu = menuName
            val placeholders = mapOf(
                "menu" to state.currentMenu,
            )

            sendDebugMessage(player, langConfig.getString("debug.navigation.enter") ?: "", placeholders)
        }
    }
    fun closeNavigation(player: Player) {
        val state = playerStates[player.name] ?: PlayerState().also { playerStates[player.name] = it }

        if (!state.navigationMode) {
            val message = (Component.text()
                .append(langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                .append(langConfig.getString("navigation.disabled")?.toMiniMessageComponent() ?: Component.text(""))
                .build())
            player.sendMessage(message)
        } else {
            state.navigationMode = false
        }
    }
    private fun reloadConfigs() {
        customConfig = YamlConfiguration.loadConfiguration(configFile)
        bindConfig = YamlConfiguration.loadConfiguration(bindFile)
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile)
        langConfig = YamlConfiguration.loadConfiguration(langFile)
    }
    fun processBindCommands(player: Player, bindType: String) {
        val bindSection = bindConfig.getConfigurationSection(bindType)
        if (bindSection != null) {
            val commandsSection = bindSection.getConfigurationSection("commands")
            commandsSection?.getKeys(false)?.forEach { key ->
                val command = commandsSection.getString("$key.command")?.replace("%player%", player.name)
                val executeType = commandsSection.getString("$key.executeType")
                if (command != null && executeType != null) {
                    when (executeType.lowercase()) {
                        "player" -> player.performCommand(command)
                        "op" -> {
                            val wasOp = player.isOp
                            player.isOp = true
                            try {
                                player.performCommand(command)
                            } finally {
                                player.isOp = wasOp
                            }
                        }
                        "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                        else -> logger.warning("Unknown executeType: $executeType in $bindType.$key")
                    }
                }
            }
        } else {
            logger.warning("No bindType $bindType found in bind.yml")
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

        if (!isCoordinateEnabled(state) || isCoordinateBlocked(state) || !isCoordConditionMet(state, player)) {
            state.x = prevX
            state.y = prevY
            state.z = prevZ
            sendDebugMessage(player, "${langConfig.get("debug.navigation.banned_coords")}")
            return
        }

        savePlayerState(state, prevX, prevY, prevZ, state.currentMenu)
        executeCommandForCoordinates(player, state)

        sendDebugMessage(player, formatMessage(langConfig.getString("debug.navigation.coordinates") ?: "", state))
        val savedX = state.savedX
        val savedY = state.savedY
        val savedZ = state.savedZ

        state.savedX = prevX
        state.savedY = prevY
        state.savedZ = prevZ
        if (state.x == savedX && state.y == savedY && state.z == savedZ) {
            state.x = prevX
            state.y = prevY
            state.z = prevZ
        }
    }
    private fun formatMessage(message: String, state: PlayerState): String {
        return message.replace("{x}", "${state.x}")
            .replace("{y}", "${state.y}")
            .replace("{z}", "${state.z}"
                .replace("{menu}", "${state.currentMenu}"))
    }
    private fun isCoordinateEnabled(state: PlayerState): Boolean {
        val menuName = state.currentMenu
        val menuSection = customConfig.getConfigurationSection(menuName) ?: return true

        val enabledCoordinates = menuSection.getConfigurationSection("enabledCoordinates") ?: return true
        for (key in enabledCoordinates.getKeys(false)) {
            val from = enabledCoordinates.getConfigurationSection("$key.from") ?: continue
            val to = enabledCoordinates.getConfigurationSection("$key.to") ?: continue

            val fromX = from.getInt("x")
            val fromY = from.getInt("y")
            val fromZ = from.getInt("z")

            val toX = to.getInt("x")
            val toY = to.getInt("y")
            val toZ = to.getInt("z")

            val minX = minOf(fromX, toX)
            val maxX = maxOf(fromX, toX)
            val minY = minOf(fromY, toY)
            val maxY = maxOf(fromY, toY)
            val minZ = minOf(fromZ, toZ)
            val maxZ = maxOf(fromZ, toZ)

            if (state.x in minX..maxX && state.y in minY..maxY && state.z in minZ..maxZ) {
                return true
            }
        }
        return false
    }
    private fun isCoordinateBlocked(state: PlayerState): Boolean {
        val menuName = state.currentMenu
        val menuSection = customConfig.getConfigurationSection(menuName) ?: return false

        val blockedCoordinates = menuSection.getConfigurationSection("blockedCoordinates") ?: return false
        for (key in blockedCoordinates.getKeys(false)) {
            val from = blockedCoordinates.getConfigurationSection("$key.from") ?: continue
            val to = blockedCoordinates.getConfigurationSection("$key.to") ?: continue

            val fromX = from.getInt("x")
            val fromY = from.getInt("y")
            val fromZ = from.getInt("z")

            val toX = to.getInt("x")
            val toY = to.getInt("y")
            val toZ = to.getInt("z")

            val minX = minOf(fromX, toX)
            val maxX = maxOf(fromX, toX)
            val minY = minOf(fromY, toY)
            val maxY = maxOf(fromY, toY)
            val minZ = minOf(fromZ, toZ)
            val maxZ = maxOf(fromZ, toZ)

            if (state.x in minX..maxX && state.y in minY..maxY && state.z in minZ..maxZ) {
                return true
            }
        }
        return false
    }
    private fun savePlayerState(state: PlayerState, x: Int, y: Int, z: Int, menu: String) {
        state.savedX = x
        state.savedY = y
        state.savedZ = z
        state.savedMenu = menu
    }
    private fun executeCommandForCoordinates(player: Player, state: PlayerState) {
        val menuName = state.currentMenu
        val menuSection = customConfig.getConfigurationSection(menuName) ?: return

        for (key in menuSection.getKeys(false)) {
            if (key == "enabledCoordinates" || key == "blockedCoordinates") continue

            val commandSection = menuSection.getConfigurationSection(key) ?: continue
            val targetX = commandSection.getInt("targetX")
            val targetY = commandSection.getInt("targetY")
            val targetZ = commandSection.getInt("targetZ")

            if (state.x == targetX && state.y == targetY && state.z == targetZ) {
                val command = commandSection.getString("command")
                val nextMenu = commandSection.getString("nextMenu")
                val executionType = commandSection.getString("executionType") ?: "player"

                if (command != null) {
                    object : BukkitRunnable() {
                        override fun run() {
                            when (executionType) {
                                "console" -> server.dispatchCommand(
                                    server.consoleSender,
                                    command.replace("%player%", player.name)
                                )

                                "player" -> player.performCommand(command.replace("%player%", player.name))
                                "op" -> {
                                    val wasOp = player.isOp
                                    player.isOp = true
                                    player.performCommand(command.replace("%player%", player.name))
                                    player.isOp = wasOp
                                }

                                else -> Bukkit.getLogger().warning("Unknown execution type: $executionType")
                            }
                        }
                    }.runTask(MovementsMain.instance)


                    if (nextMenu != null) {
                        state.currentMenu = nextMenu
                        val origin =
                            customConfig.getConfigurationSection(nextMenu)?.getString("origin", "0 0 0")?.split(" ")
                                ?.map { it.toInt() } ?: listOf(0, 0, 0)
                        state.x = origin[0]
                        state.y = origin[1]
                        state.z = origin[2]

                        sendDebugMessage(player, "You have switched to menu: $nextMenu")
                    }
                    return
                }
            }
        }
    }
    private fun isCoordConditionMet(state: PlayerState, player: Player): Boolean {
        val menuName = state.currentMenu
        val menuSection = customConfig.getConfigurationSection(menuName) ?: return false

        for (key in menuSection.getKeys(false)) {
            if (key == "enabledCoordinates" || key == "blockedCoordinates") {
                continue
            }

            val commandSection = menuSection.getConfigurationSection(key) ?: continue

            val targetX = commandSection.getInt("targetX", -999)
            val targetY = commandSection.getInt("targetY", -999)
            val targetZ = commandSection.getInt("targetZ", -999)

            if (state.x == targetX && state.y == targetY && state.z == targetZ) {

                val conditionsSection = commandSection.getConfigurationSection("panel_conditions")
                if (conditionsSection != null) {

                    var finalResult = true
                    for (conditionKey in conditionsSection.getKeys(false)) {
                        val condition = conditionsSection.getConfigurationSection(conditionKey) ?: continue

                        val first = condition.getString("first") ?: continue
                        val second = condition.getString("second") ?: continue
                        val operation = condition.getString("operation") ?: continue
                        val gate = condition.getString("gate") ?: "and"

                        val firstValue = PlaceholderAPI.setPlaceholders(player, first)
                        val secondValue = PlaceholderAPI.setPlaceholders(player, second)

                        val result = compareValues(firstValue, secondValue, operation)

                        finalResult = if (finalResult) {
                            when (gate.lowercase()) {
                                "and" -> finalResult && result
                                "or" -> finalResult || result
                                else -> finalResult
                            }
                        } else {
                            when (gate.lowercase()) {
                                "and" -> false
                                "or" -> finalResult || result
                                else -> finalResult
                            }
                        }

                    }

                    if (!finalResult) {
                        return false
                    }

                    return true
                }

                return true
            }
        }

        return true
    }
    fun compareValues(firstValue: String, secondValue: String, operation: String): Boolean {
        return try {
            when (operation.lowercase()) {
                "equals" -> firstValue == secondValue
                "not_equals" -> firstValue != secondValue
                "greater" -> firstValue.toDouble() > secondValue.toDouble()
                "greater_or_equals" -> firstValue.toDouble() >= secondValue.toDouble()
                "less" -> firstValue.toDouble() < secondValue.toDouble()
                "less_or_equals" -> firstValue.toDouble() <= secondValue.toDouble()
                else -> false
            }
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun sendDebugMessage(player: Player, message: String, placeholders: Map<String, String>? = null) {
        if (settingsConfig.getBoolean("debug", false)) {
            var formattedMessage = message

            // Если placeholders не null, выполняем замену
            placeholders?.forEach { (key, value) ->
                formattedMessage = formattedMessage.replace("{$key}", value)
            }

            player.sendMessage(formattedMessage)
        }
    }
}