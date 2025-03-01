package top.eternal.neyran.movementUI.managers

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import top.eternal.neyran.movementUI.MovementsMain
import top.eternal.neyran.movementUI.PlayerState

class ConditionManager(private val plugin: MovementsMain) {

    fun isCoordinateEnabled(state: PlayerState): Boolean {
        val menuName = state.currentMenu
        val menuSection = plugin.configManager.customConfig.getConfigurationSection(menuName) ?: return true

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

    fun isCoordinateBlocked(state: PlayerState): Boolean {
        val menuName = state.currentMenu
        val menuSection = plugin.configManager.customConfig.getConfigurationSection(menuName) ?: return false

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

    fun isCoordConditionMet(state: PlayerState, player: Player): Boolean {
        val menuName = state.currentMenu
        val menuSection = plugin.configManager.customConfig.getConfigurationSection(menuName) ?: return false

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
    fun isCommandConditionMet(commandSection: ConfigurationSection, player: Player): Boolean {
        val conditionsSection = commandSection.getConfigurationSection("conditions")
        return conditionsSection?.let { evaluateConditions(it, player) } ?: true
    }

    fun evaluateConditions(conditionsSection: ConfigurationSection, player: Player): Boolean {
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
    fun applyGateLogic(currentResult: Boolean, newResult: Boolean, gate: String): Boolean {
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
}