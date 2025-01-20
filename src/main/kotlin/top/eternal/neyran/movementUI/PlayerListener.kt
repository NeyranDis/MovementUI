package top.eternal.neyran.movementUI

import com.comphenix.protocol.events.ListenerPriority
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.player.*

class PlayerListener(private val plugin: MovementsMain) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.playerStates[player.name] = PlayerState()
    }
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (state.navigationMode) {
            val armorStand = state.armorStand?.let { uuid ->
                Bukkit.getEntity(uuid) as? ArmorStand
            }
            armorStand?.remove()
            state.armorStand = null
            state.navigationMode = false
        }
    }

    @EventHandler
    fun onPlayerInteractWithVehicle(event: PlayerInteractEntityEvent) {
        val player = event.player

        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            state.navigationMode = false
        }
    }
    @EventHandler
    fun onPlayerInteractWithShulkerBox(event: PlayerInteractEvent) {
        val player = event.player
        val clickedBlock = event.clickedBlock ?: return

        if (clickedBlock.type.name.endsWith("SHULKER_BOX")) {
            val state = plugin.playerStates[player.name] ?: return
            if (state.navigationMode) {
                state.navigationMode = false
            }
        }
    }
    @EventHandler
    fun onPlayerInWaterOrLava(event: PlayerMoveEvent) {
        val player = event.player
        val location = player.location

        val blockType = location.block.type

        if (blockType == Material.WATER || blockType == Material.LAVA) {
            val state = plugin.playerStates[player.name] ?: return
            if (state.navigationMode) {
                state.navigationMode = false
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDismount(event: EntityDismountEvent) {
        val entity = event.entity
        if (entity is Player) {
            val player = entity

            val state = plugin.playerStates[player.name] ?: return
            if (state.navigationMode) {
                event.isCancelled = true
            }
        }
    }
    @EventHandler
    fun onPlayerBreakBlock(event: BlockBreakEvent) {
        val player = event.player

        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            state.navigationMode = false
        }
    }
    @EventHandler
    fun onPlayerToggleSprint(event: PlayerToggleSprintEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (state.navigationMode && event.isSprinting) {
            state.navigationMode = false
            plugin.sendDebugMessage(player,"${plugin.langConfig.get("navigation.toggle_disable")}")
            state.lastKeyPressed = "Ctrl"
        }
    }
    @EventHandler
    fun onPlayerToggleFlight(event: PlayerToggleFlightEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (state.navigationMode && event.isFlying) {
            plugin.updatePlayerCoordinates(player, "Space")
        }
    }
    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player

        val bindKey = plugin.settingsConfig.getString("bind") ?: "F"

        if (bindKey != "F") return

        if (!player.isSneaking) return

        event.isCancelled = true

        if (!plugin.settingsConfig.getBoolean("bind-enable", true)) return

        plugin.api.bindActivator(player)
    }
}