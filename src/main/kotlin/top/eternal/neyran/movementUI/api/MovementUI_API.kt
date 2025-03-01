package top.eternal.neyran.movementUI.api

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import top.eternal.neyran.movementUI.MovementsMain

class MovementUI_API(private val plugin: MovementsMain) {

    /**
     * Enables or disables navigation mode for the player and raises the BindActivatorEvent.
     *
     * @param player The player for which the method is executed.
     */
    fun bindActivator(player: Player) {
        val state = plugin.playerStates[player.name] ?: return
        val defaultMenu = plugin.configManager.settingsConfig.getString("default_menu") ?: "default"

        if (state.navigationMode) {
            plugin.closeNavigation(player)
        } else {
            plugin.startNavigation(player, defaultMenu)
            plugin.sendDebugMessage(
                player,
                plugin.configManager.langConfig.getString("debug.navigation.bind_activation") ?: "",
                mapOf("menu" to state.currentMenu)
            )
        }

        val event = BindActivatorEvent(player)
        Bukkit.getServer().pluginManager.callEvent(event)
    }

    /**
     * Event fired when a navigation anchor is activated.
     *
     * @param player The player who activated the binding.
     */
    class BindActivatorEvent(val player: Player) : Event() {
        companion object {
            @JvmStatic
            val HANDLER_LIST = HandlerList()
        }

        override fun getHandlers(): HandlerList {
            return HANDLER_LIST
        }
    }
}
