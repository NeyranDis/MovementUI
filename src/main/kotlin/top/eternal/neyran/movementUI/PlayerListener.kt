package top.eternal.neyran.movementUI

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.*
import org.spigotmc.event.entity.EntityDismountEvent

class PlayerListener(private val plugin: MovementsMain) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.playerStates[event.player.name] = PlayerState()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerName = event.player.name
        val state = plugin.playerStates.remove(playerName) ?: return

        if (state.navigationMode) {
            state.armorStand?.let { uuid ->
                (Bukkit.getEntity(uuid) as? ArmorStand)?.remove()
            }
        }
    }

    @EventHandler
    fun onPlayerInteractWithEntity(event: PlayerInteractEntityEvent) {
        disableNavigationIfActive(event.player)
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        if (clickedBlock.type.name.endsWith("SHULKER_BOX")) {
            disableNavigationIfActive(event.player)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val blockType = event.player.location.block.type
        if (blockType == Material.WATER || blockType == Material.LAVA) {
            disableNavigationIfActive(event.player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDismount(event: EntityDismountEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            val state = plugin.playerStates[player.name] ?: return
            if (state.navigationMode) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        disableNavigationIfActive(event.player)
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val bindKey = plugin.settingsConfig.getString("bind") ?: "F"

        if (bindKey.equals("F", ignoreCase = true) && player.isSneaking && plugin.settingsConfig.getBoolean("bind-enable", true)) {
            event.isCancelled = true
            plugin.api.bindActivator(player)
        }
    }

    private fun disableNavigationIfActive(player: Player) {
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            plugin.closeNavigation(player)
        }
    }

}
