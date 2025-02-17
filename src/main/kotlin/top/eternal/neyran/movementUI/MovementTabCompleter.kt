package top.eternal.neyran.movementUI

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MovementTabCompleter(private val menusDir: File) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        if (command.name.equals("movementui", ignoreCase = true) || command.name.equals("mui", ignoreCase = true)) {
            return when (args.size) {
                1 -> {
                    listOf("startmenu", "closemenu", "reload").filter { it.startsWith(args[0], ignoreCase = true) }
                }
                2 -> {
                    if (args[0].equals("startmenu", ignoreCase = true)) {
                        loadAllMenuNames().filter { it.startsWith(args[1], ignoreCase = true) }
                    } else {
                        null
                    }
                }
                else -> null
            }
        }
        return null
    }

    private fun loadAllMenuNames(): List<String> {
        if (!menusDir.exists() || !menusDir.isDirectory) return emptyList()

        return menusDir.listFiles { file -> file.extension.equals("yml", ignoreCase = true) }
            ?.flatMap { file ->
                val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)
                config.getKeys(false)
            }
            ?.distinct()
            ?: emptyList()
    }
}
