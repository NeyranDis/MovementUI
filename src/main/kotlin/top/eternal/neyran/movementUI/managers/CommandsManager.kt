package top.eternal.neyran.movementUI.managers

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import top.eternal.neyran.movementUI.MovementsMain
import top.eternal.neyran.movementUI.utils.ChatUtils.toMiniMessageComponent


class CommandsManager(private val plugin: MovementsMain) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("movementui", ignoreCase = true)) {
            if (args.isEmpty()) {
                return true
            }

            when (args[0].lowercase()) {
                "startmenu" -> {
                    if (sender is Player || sender is ConsoleCommandSender) {
                        val menuName = args.getOrNull(1) ?: plugin.configManager.settingsConfig.getString("default_menu") ?: "default"
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
                                plugin.configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                    ?.append(plugin.configManager.langConfig.getString("player.not.found")?.toMiniMessageComponent()
                                        ?: Component.text(""))
                                    ?: Component.text("")
                            )
                            return true
                        }

                        if (sender is Player && !sender.isOp && !sender.hasPermission("movementui.startmenu")) {
                            sender.sendMessage(
                                plugin.configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                    ?.append(plugin.configManager.langConfig.getString("no.permissions")?.toMiniMessageComponent()
                                        ?: Component.text(""))
                                    ?: Component.text("")
                            )
                            return true
                        }

                        plugin.startNavigation(targetPlayer, menuName)
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
                            plugin.configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                ?.append(plugin.configManager.langConfig.getString("player.not.found")?.toMiniMessageComponent()
                                    ?: Component.text(""))
                                ?: Component.text("")
                        )
                        return true
                    }

                    if (sender is Player && !sender.isOp && !sender.hasPermission("movementui.closemenu")) {
                        sender.sendMessage(
                            plugin.configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent()
                                ?.append(plugin.configManager.langConfig.getString("no.permissions")?.toMiniMessageComponent()
                                    ?: Component.text(""))
                                ?: Component.text("")
                        )
                        return true
                    }

                    plugin.closeNavigation(targetPlayer)
                }

                "reload" -> {
                    if (sender is ConsoleCommandSender || sender.isOp || sender.hasPermission("movementui.reload")) {
                        plugin.configManager.reloadConfigs()
                        val message = (Component.text()
                            .append(plugin.configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                            .append(plugin.configManager.langConfig.getString("reload.command")?.toMiniMessageComponent() ?: Component.text(""))
                            .build())
                        sender.sendMessage(message)
                    } else {
                        val message = (Component.text()
                            .append(plugin.configManager.langConfig.getString("plugin.tag")?.toMiniMessageComponent() ?: Component.text(""))
                            .append(plugin.configManager.langConfig.getString("no.permissions")?.toMiniMessageComponent() ?: Component.text(""))
                            .build())
                        sender.sendMessage(message)
                    }
                }
            }
            return true
        }
        return false
    }
    fun processBindCommands(player: Player, bindType: String) {
        val bindSection = plugin.configManager.bindConfig.getConfigurationSection(bindType)
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
                        else -> plugin.logger.warning("Unknown executeType: $executeType in $bindType.$key")
                    }
                }
            }
        } else {
            plugin.logger.warning("No bindType $bindType found in bind.yml")
        }
    }
}