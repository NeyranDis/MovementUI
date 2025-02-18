package top.eternal.neyran.movementUI

import ProtocolListener
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.FileConfigurationOptions
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.ServicePriority
import org.bukkit.entity.ArmorStand
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.regex.Pattern

class MovementsMain : JavaPlugin() {
    var vers = "1.1.1"
    val playerStates: MutableMap<String, PlayerState> = mutableMapOf()
    lateinit var configFile: File
    lateinit var customConfig: FileConfiguration
    lateinit var settingsFile: File
    lateinit var settingsConfig: FileConfiguration
    lateinit var langFile: File
    lateinit var langConfig: FileConfiguration
    lateinit var bindFile: File
    lateinit var bindConfig: FileConfiguration
    fun key(name: String): NamespacedKey {
        return NamespacedKey(this, name)
    }

    fun String.toMiniMessageComponent(): Component {
        val legacyHexPattern = Pattern.compile("&x((&[A-Fa-f0-9]){6})")
        var result = this
        val matcher = legacyHexPattern.matcher(result)
        while (matcher.find()) {
            val fullMatch = matcher.group()
            val hexSequence = fullMatch.replace("&x", "").replace("&", "")
            result = result.replace(fullMatch, "<#${hexSequence}>")
        }
        result = result.replace("&([0-9a-fA-Fk-oK-OrR])".toRegex()) { matchResult ->
            val colorCode = matchResult.groupValues[1]
            when (colorCode.lowercase()) {
                "k" -> "<obfuscate>"
                "l" -> "<bold>"
                "m" -> "<strikethrough>"
                "n" -> "<underline>"
                "o" -> "<italic>"
                "r" -> "<reset>"
                "0" -> "<black>"
                "1" -> "<dark_blue>"
                "2" -> "<dark_green>"
                "3" -> "<dark_aqua>"
                "4" -> "<dark_red>"
                "5" -> "<dark_purple>"
                "6" -> "<gold>"
                "7" -> "<gray>"
                "8" -> "<dark_gray>"
                "9" -> "<blue>"
                "a" -> "<green>"
                "b" -> "<aqua>"
                "c" -> "<red>"
                "d" -> "<light_purple>"
                "e" -> "<yellow>"
                "f" -> "<white>"
                else -> "<reset>"
            }
        }
        return MiniMessage.miniMessage().deserialize(result)
    }

    lateinit var api: MovementUI_API
    override fun onEnable() {
        saveDefaultConfig()

        settingsFile = File(dataFolder, "config.yml")
        bindFile = File(dataFolder, "bind.yml")
        langFile = File(dataFolder, "lang.yml")
        val menusDir = File(dataFolder, "menus")

        if (!settingsFile.exists()) {
            saveResource("config.yml", false)
        }
        if (!bindFile.exists()) {
            saveResource("bind.yml", false)
        }
        if (!langFile.exists()) {
            saveResource("lang.yml", false)
        }
        if (!menusDir.exists()) {
            menusDir.mkdirs()
            saveResource("menus/default_menus.yml", false)
        }

        configFile = menusDir
        customConfig = loadVirtualConfig(menusDir)

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
        api = MovementUI_API(this)

        Bukkit.getServicesManager().register(MovementUI_API::class.java, api, this, ServicePriority.Normal)
        logger.info(" \n" +
                "  __  __                                     _   _    _ _____ \n" +
                " |  \\/  |                                   | | | |  | |_   _|     MovementUI: ${vers}\n" +
                " | \\  / | _____   _____ _ __ ___   ___ _ __ | |_| |  | | | |       Build Data: 2025/2/18-15:37\n" +
                " | |\\/| |/ _ \\ \\ / / _ \\ '_ ` _ \\ / _ \\ '_ \\| __| |  | | | |       Author: Neyran\n" +
                " | |  | | (_) \\ V /  __/ | | | | |  __/ | | | |_| |__| |_| |_ \n" +
                " |_|  |_|\\___/ \\_/ \\___|_| |_| |_|\\___|_| |_|\\__|\\____/|_____|\n" +
                "                                                              \n" +
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
                    if (sender is Player || sender is ConsoleCommandSender) {
                        val menuName = args.getOrNull(1) ?: settingsConfig.getString("default_menu") ?: "default"
                        val targetPlayerName = args.getOrNull(2)

                        val targetPlayer = if (targetPlayerName != null) {
                            Bukkit.getPlayerExact(targetPlayerName)
                        } else if (sender is Player) {
                            sender
                        } else {
                            null
                        }

                        if (targetPlayer == null || !targetPlayer.isOnline) {
                            sender.sendMessage(
                                langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                    ?.append(langConfig.getString("player.not.found")?.toMiniMessageComponent()
                                        ?: Component.text(""))
                                    ?: Component.text("")
                            )
                            return true
                        }

                        if (sender is Player && !sender.isOp && !sender.hasPermission("movementui.startmenu")) {
                            sender.sendMessage(
                                langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                    ?.append(langConfig.getString("no.permissions")?.toMiniMessageComponent()
                                        ?: Component.text(""))
                                    ?: Component.text("")
                            )
                            return true
                        }

                        startNavigation(targetPlayer, menuName)
                    }
                }

                "closemenu" -> {
                    val targetPlayerName = args.getOrNull(1)

                    val targetPlayer = if (targetPlayerName != null) {
                        Bukkit.getPlayerExact(targetPlayerName)
                    } else if (sender is Player) {
                        sender
                    } else {
                        null
                    }

                    if (targetPlayer == null || !targetPlayer.isOnline) {
                        sender.sendMessage(
                            langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                ?.append(langConfig.getString("player.not.found")?.toMiniMessageComponent()
                                    ?: Component.text(""))
                                ?: Component.text("")
                        )
                        return true
                    }

                    if (sender is Player && !sender.isOp && !sender.hasPermission("movementui.closemenu")) {
                        sender.sendMessage(
                            langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                ?.append(langConfig.getString("no.permissions")?.toMiniMessageComponent()
                                    ?: Component.text(""))
                                ?: Component.text("")
                        )
                        return true
                    }

                    closeNavigation(targetPlayer)
                }

                "reload" -> {
                    if (sender is ConsoleCommandSender || sender.isOp || sender.hasPermission("movementui.reload")) {
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
    private fun loadVirtualConfig(dir: File): YamlConfiguration {
        val config = YamlConfiguration()
        val configs = dir.listFiles { _, name -> name.endsWith(".yml") }
            ?.map { YamlConfiguration.loadConfiguration(it) } ?: emptyList()

        configs.forEach { yaml ->
            yaml.getKeys(true).forEach { key ->
                if (!config.isSet(key)) {
                    config.set(key, yaml.get(key))
                }
            }
        }
        return config
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
        val state = playerStates.getOrPut(player.name) { PlayerState() }

        if (!state.navigationMode) {
            if (settingsConfig.getBoolean("air_fix", true) && !player.isFlying) {
                val blockBelow = player.location.add(0.0, -1.0, 0.0).block
                if (blockBelow.type.isAirCompatible()) {
                    closeNavigation(player)
                    return
                }
            }

            val origin = customConfig.getConfigurationSection(menuName)
                ?.getString("origin", "0 0 0")
                ?.split(" ")
                ?.mapNotNull { it.toIntOrNull() }
                ?: listOf(0, 0, 0)

            state.x = origin.getOrElse(0) { 0 }
            state.y = origin.getOrElse(1) { 0 }
            state.z = origin.getOrElse(2) { 0 }
            state.currentMenu = menuName

            state.navigationMode = true
            val world = player.world
            val location = player.location.clone().apply {
                y += 0.6
            }

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

                    if (settingsConfig.getBoolean("bind-command", false)) {
                        processBindCommands(player, "bind_enter")
                    }

                    sendDebugMessage(player, langConfig.getString("debug.navigation.enter") ?: "", mapOf("menu" to state.currentMenu))
                }
            }.runTaskLater(this, 5L)
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

            if (settingsConfig.getBoolean("bind-command", false)) {
                processBindCommands(player, "bind_exit")
            }
        }
    }


    private fun reloadConfigs() {
        customConfig = loadVirtualConfig(File(dataFolder, "menus"))
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

        val menuSection = customConfig.getConfigurationSection(state.currentMenu) ?: return

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
            val nextMenuSection = customConfig.getConfigurationSection(nextMenu)
            val permission = nextMenuSection?.getString("permission")

            if (permission != null && !player.hasPermission(permission)) {
                state.x = prevX
                state.y = prevY
                state.z = prevZ
                val message = (Component.text()
                    .append(langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                    .append(langConfig.getString("no.permissions_menu")?.toMiniMessageComponent() ?: Component.text(""))
                    .build())
                player.sendMessage(message)
                return
            }
        }
        executeCommandForCoordinates(player, state)
        sendDebugMessage(player, formatMessage(langConfig.getString("debug.navigation.coordinates") ?: "", state))
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

    private fun executeCommandForCoordinates(player: Player, state: PlayerState) {
        val menuName = state.currentMenu
        val menuSection = customConfig.getConfigurationSection(menuName) ?: return

        val permission = menuSection.getString("permission")
        if (permission != null && !player.hasPermission(permission)) {
            sendDebugMessage(player, "You do not have permission to access this menu.")
            return
        }

        for (key in menuSection.getKeys(false)) {
            if (key == "enabledCoordinates" || key == "blockedCoordinates") continue

            val commandSection = menuSection.getConfigurationSection(key) ?: continue
            val targetX = commandSection.getInt("targetX")
            val targetY = commandSection.getInt("targetY")
            val targetZ = commandSection.getInt("targetZ")

            if (state.x == targetX && state.y == targetY && state.z == targetZ && isCoordConditionMet(state, player)) {
                findAndExecuteCommands(commandSection, player, state)
                return
            }
        }
    }

    private fun findAndExecuteCommands(section: ConfigurationSection, player: Player, state: PlayerState) {
        val commands = section.getConfigurationSection("commands")
        val nextMenu = section.getString("nextMenu")
        val swap = section.getString("swap")

        commands?.let {
            it.getKeys(false).forEach { key ->
                val command = it.getString("$key.command")
                val executionType = it.getString("$key.executionType") ?: "player"
                if (command != null && isCommandConditionMet(it.getConfigurationSection(key)!!, player)) {
                    executeCommand(command, player, executionType)
                }
            }
        }

        swap?.let {
            val swapCoordinates = it.split(" ").mapNotNull { it.toIntOrNull() }
            if (swapCoordinates.size == 3) {
                state.x = swapCoordinates[0]
                state.y = swapCoordinates[1]
                state.z = swapCoordinates[2]
            }
        }

        nextMenu?.let { nextMenuValue ->
            val parts = nextMenuValue.split(" ")
            val menuName = parts[0]
            val newCoordinates = if (parts.size == 4) {
                parts.subList(1, 4).mapNotNull { it.toIntOrNull() }
            } else {
                null
            }

            state.currentMenu = menuName

            if (newCoordinates != null && newCoordinates.size == 3) {
                state.x = newCoordinates[0]
                state.y = newCoordinates[1]
                state.z = newCoordinates[2]
            } else {
                val origin = customConfig.getConfigurationSection(menuName)
                    ?.getString("origin", "0 0 0")
                    ?.split(" ")
                    ?.mapNotNull { it.toIntOrNull() }
                    ?: listOf(0, 0, 0)

                if (origin.size == 3) {
                    state.x = origin[0]
                    state.y = origin[1]
                    state.z = origin[2]
                } else {
                    state.x = 0
                    state.y = 0
                    state.z = 0
                }
            }
        }
    }

    private fun isCoordConditionMet(state: PlayerState, player: Player): Boolean {
        val menuName = state.currentMenu
        val menuSection = customConfig.getConfigurationSection(menuName) ?: return false

        for (key in menuSection.getKeys(false)) {
            if (key in setOf("enabledCoordinates", "blockedCoordinates", "permission")) {
                continue
            }

            val commandSection = menuSection.getConfigurationSection(key) ?: continue

            val targetX = commandSection.getInt("targetX", -999)
            val targetY = commandSection.getInt("targetY", -999)
            val targetZ = commandSection.getInt("targetZ", -999)

            if (state.x == targetX && state.y == targetY && state.z == targetZ) {
                val conditionsSection = commandSection.getConfigurationSection("panel_conditions")
                return conditionsSection?.let { evaluateConditions(it, player) } ?: true
            }
        }
        return true
    }
    private fun executeCommand(command: String, player: Player, executionType: String) {
        Bukkit.getScheduler().runTask(this, Runnable {
            when (executionType) {
                "console" -> this.server.dispatchCommand(
                    this.server.consoleSender,
                    command.replace("%player%", player.name)
                )
                "player" -> player.performCommand(command.replace("%player%", player.name))
                "op" -> {
                    val wasOp = player.isOp
                    player.isOp = true
                    try {
                        player.performCommand(command.replace("%player%", player.name))
                    } finally {
                        player.isOp = wasOp
                    }
                }
                else -> this.logger.warning("Unknown execution type: $executionType")
            }
        })
    }

    private fun isCommandConditionMet(commandSection: ConfigurationSection, player: Player): Boolean {
        val conditionsSection = commandSection.getConfigurationSection("conditions")
        return conditionsSection?.let { evaluateConditions(it, player) } ?: true
    }

    private fun evaluateConditions(conditionsSection: ConfigurationSection, player: Player): Boolean {
        var finalResult = true

        for (conditionKey in conditionsSection.getKeys(false)) {
            val condition = conditionsSection.getConfigurationSection(conditionKey) ?: continue

            val first = condition.getString("first") ?: continue
            val second = condition.getString("second") ?: continue
            val operation = condition.getString("operation") ?: continue
            val gate = condition.getString("gate")?.lowercase() ?: "and"

            val firstValue = PlaceholderAPI.setPlaceholders(player, first)
            val secondValue = PlaceholderAPI.setPlaceholders(player, second)

            val result = compareValues(firstValue, secondValue, operation)

            finalResult = applyGateLogic(finalResult, result, gate)
            if (!finalResult && gate == "and") return false
        }

        return finalResult
    }

    private fun applyGateLogic(currentResult: Boolean, newResult: Boolean, gate: String): Boolean {
        return when (gate) {
            "and" -> currentResult && newResult
            "or" -> currentResult || newResult
            else -> currentResult
        }
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

            placeholders?.forEach { (key, value) ->
                formattedMessage = formattedMessage.replace("{$key}", value)
            }

            player.sendMessage(formattedMessage)
        }
    }
}