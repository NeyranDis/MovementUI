package top.eternal.neyran.movementUI

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.configuration.file.YamlConfiguration

class MovementUI_API(private val plugin: MovementsMain) {


    /**
     * Активирует или деактивирует режим навигации для игрока и вызывает событие BindActivatorEvent.
     *
     * @param player Игрок, для которого выполняется метод.
     */
    fun bindActivator(player: Player) {
        val state = plugin.playerStates[player.name] ?: return
        val defaultMenu = plugin.settingsConfig.getString("default_menu") ?: "default"

        if (state.navigationMode) {
            plugin.closeNavigation(player)
        } else {
            plugin.startNavigation(player, defaultMenu)
            plugin.sendDebugMessage(
                player,
                plugin.langConfig.getString("debug.navigation.bind_activation") ?: "",
                mapOf("menu" to state.currentMenu)
            )
        }

        val event = BindActivatorEvent(player)
        Bukkit.getServer().pluginManager.callEvent(event)
    }


    class BindActivatorEvent(val player: Player) : Event() {
        companion object {
            val HANDLER_LIST = HandlerList()
        }

        override fun getHandlers(): HandlerList {
            return HANDLER_LIST
        }
    }
}
