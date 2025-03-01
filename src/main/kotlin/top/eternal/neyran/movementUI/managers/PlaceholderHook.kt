package top.eternal.neyran.movementUI.managers

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import top.eternal.neyran.movementUI.MovementsMain

class PlaceholderHook(private val plugin: MovementsMain) : PlaceholderExpansion() {
    override fun getIdentifier(): String = "movementui"
    override fun getAuthor(): String = plugin.description.authors.joinToString()
    override fun getVersion(): String = plugin.description.version
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        val state = plugin.playerStates[player.name] ?: return "None"

        if (!state.navigationMode) {
            return if (params.lowercase() == "navigation") {
                "false"
            } else {
                "None"
            }
        }
        return when (params.lowercase()) {
            "last_key" -> state.lastKeyPressed ?: "None"
            "current_coordinates" -> "${state.x}, ${state.y}, ${state.z}"
            "current_coordinates_full" -> "${state.x}, ${state.y}, ${state.z}, ${state.currentMenu}"
            "currentmenu" -> state.currentMenu
            "navigation" -> if (state.navigationMode) "true" else "false"
            else -> null
        }
    }
}
