package top.eternal.neyran.movementUI.managers

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import top.eternal.neyran.movementUI.MovementsMain
import top.eternal.neyran.movementUI.PlayerState

class ExecutiveManager(private val plugin: MovementsMain) {
    fun executeCommandForCoordinates(player: Player, state: PlayerState) {
        val menuName = state.currentMenu
        val menuSection = plugin.configManager.customConfig.getConfigurationSection(menuName) ?: return

        val permission = menuSection.getString("permission")
        if (permission != null && !player.hasPermission(permission)) {
            plugin.sendDebugMessage(player, "You do not have permission to access this menu.")
            return
        }

        for (key in menuSection.getKeys(false)) {
            if (key == "enabledCoordinates" || key == "blockedCoordinates") continue

            val commandSection = menuSection.getConfigurationSection(key) ?: continue
            val targetX = commandSection.getInt("targetX")
            val targetY = commandSection.getInt("targetY")
            val targetZ = commandSection.getInt("targetZ")

            if (state.x == targetX && state.y == targetY && state.z == targetZ && plugin.conditionsManager.isCoordConditionMet(state, player)) {
                findAndExecuteCommands(commandSection, player, state)
                return
            }
        }
    }
     fun findAndExecuteCommands(section: ConfigurationSection, player: Player, state: PlayerState) {
        val commands = section.getConfigurationSection("commands")
        val nextMenu = section.getString("nextMenu")
        val swap = section.getString("swap")

        commands?.let {
            it.getKeys(false).forEach { key ->
                val command = it.getString("$key.command")
                val executionType = it.getString("$key.executionType") ?: "player"
                if (command != null && plugin.conditionsManager.isCommandConditionMet(it.getConfigurationSection(key)!!, player)) {
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
                val origin = plugin.configManager.customConfig.getConfigurationSection(menuName)
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
    fun executeCommand(command: String, player: Player, executionType: String) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            when (executionType) {
                "console" -> plugin.server.dispatchCommand(
                    plugin.server.consoleSender,
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
                else -> plugin.logger.warning("Unknown execution type: $executionType")
            }
        })
    }
}