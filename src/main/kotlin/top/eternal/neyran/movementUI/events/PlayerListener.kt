package top.eternal.neyran.movementUI.events

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.spigotmc.event.entity.EntityDismountEvent
import top.eternal.neyran.movementUI.MovementsMain
import top.eternal.neyran.movementUI.PlayerState


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
    fun onPlayerDead(event: PlayerDeathEvent) {
        disableNavigationIfActive(event.player)
    }
    @EventHandler
    fun onPlayerInteractWithEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
                event.isCancelled = true
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
    fun onPlayerClick(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                val menuName = state.currentMenu

                val clickDetectEnabled = plugin.configManager.customConfig.getBoolean("$menuName.click_detect", false)

                if (clickDetectEnabled) {
                    plugin.updatePlayerCoordinates(player, "Space")
                    event.isCancelled = true
                }
            }
        }
    }
    @EventHandler
    fun onBlockPlaced(event: BlockPlaceEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            event.isCancelled = true
        }
    }
    @EventHandler
    fun onHotbarSwitch(event: PlayerItemHeldEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (!state.navigationMode) return

        val menuName = state.currentMenu
        val menuConfig = plugin.configManager.customConfig.getConfigurationSection(menuName)
        val blacklist = menuConfig?.getStringList("command_blacklist") ?: emptyList()

        val args = event.message.removePrefix("/").split(" ")
        val baseCommand = args[0]
        val fullCommand = args.joinToString(" ")

        if (blacklist.contains(baseCommand) || blacklist.contains(fullCommand)) {
            event.isCancelled = true
            plugin.sendMessage(player, plugin.configManager.langConfig.getString("command_block") ?: "")
        }
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val bindKey = plugin.configManager.settingsConfig.getString("bind") ?: "F"

        if (bindKey.equals("F", ignoreCase = true) && player.isSneaking && plugin.configManager.settingsConfig.getBoolean("bind-enable", true)) {
            event.isCancelled = true

            val state = plugin.playerStates[player.name] ?: return

            if (state.navigationMode) {
                val menuName = state.currentMenu
                val menuConfig = plugin.configManager.customConfig.getConfigurationSection(menuName)
                val bindAllowed = menuConfig?.getBoolean("closeable", true) ?: true

                if (bindAllowed) {
                    disableNavigationIfActive(player)
                }
            } else {
                plugin.api.bindActivator(player)
            }
        }
    }

    private fun disableNavigationIfActive(player: Player) {
        val state = plugin.playerStates[player.name] ?: return
        if (state.navigationMode) {
            plugin.closeNavigation(player)
        }
    }
}
