package top.eternal.neyran.movementUI

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class PlaceholderHook(private val plugin: MovementsMain) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "movements"

    override fun getAuthor(): String = plugin.description.authors.joinToString()

    override fun getVersion(): String = plugin.description.version

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        val state = plugin.playerStates[player.name] ?: return "None"

        if (!state.navigationMode) return "None"

        return when (params.lowercase()) {
            "last" -> state.lastKeyPressed ?: "None"
            "coordinates" -> "${state.x}, ${state.y}, ${state.z}"
            "full_coordinates" -> "${state.x}, ${state.y}, ${state.z}, ${state.currentMenu}"
            "menu" -> "${state.currentMenu}"
            "enable" -> "${state.navigationMode}"
            else -> null
        }
    }
}