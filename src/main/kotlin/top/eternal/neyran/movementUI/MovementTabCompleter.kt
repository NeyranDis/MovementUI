package top.eternal.neyran.movementUI

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MovementTabCompleter(private val configFile: File) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        if (command.name.equals("movementui", ignoreCase = true) || command.name.equals("mui", ignoreCase = true)) {
            return when (args.size) {
                1 -> {
                    // Предлагаем основные команды
                    listOf("startmenu", "closemenu", "reload").filter { it.startsWith(args[0], ignoreCase = true) }
                }
                2 -> {
                    if (args[0].equals("startmenu", ignoreCase = true)) {
                        val menusConfig: FileConfiguration = YamlConfiguration.loadConfiguration(configFile)
                        menusConfig.getKeys(false).filter { it.startsWith(args[1], ignoreCase = true) }.toList()
                    } else {
                        null
                    }
                }
                else -> null
            }
        }
        return null
    }
}
