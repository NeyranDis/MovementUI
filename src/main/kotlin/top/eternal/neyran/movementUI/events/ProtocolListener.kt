package top.eternal.neyran.movementUI.events

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import top.eternal.neyran.movementUI.MovementsMain
import top.eternal.neyran.movementUI.PlayerState

class ProtocolListener(private val plugin: MovementsMain) {
    private val protocolManager = ProtocolLibrary.getProtocolManager()

    fun registerPacketListeners() {
        protocolManager.addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.HIGHEST,
            PacketType.Play.Client.STEER_VEHICLE
        ) {
            override fun onPacketReceiving(event: PacketEvent) {
                handlePacket(event)
            }
        })

        protocolManager.addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.HIGHEST,
            PacketType.Play.Client.HELD_ITEM_SLOT
        ) {
            override fun onPacketReceiving(event: PacketEvent) {
                handleHotbarScrollPacket(event)
            }
        })
    }

    private fun handlePacket(event: PacketEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (state.navigationMode) {
            val currentTime = System.currentTimeMillis()
            val delay = plugin.configManager.settingsConfig.getInt("detect-delay")
            if (state.lastMoveTime?.let { currentTime - it < delay } == true) return

            state.lastMoveTime = currentTime

            val serverVersion = getServerVersion()
            when {
                serverVersion >= 1213 -> processMovement_1_21_3(event, state, player)
                serverVersion in 1201..1212 -> processMovement_legacy(event, state, player)
            }
        }
    }
    private fun handleHotbarScrollPacket(event: PacketEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (state.navigationMode) {
            val currentTime = System.currentTimeMillis()
            val delay = plugin.configManager.settingsConfig.getInt("scroll-detect-delay")
            if (state.lastMoveTime?.let { currentTime - it < delay } == true) return

            state.lastMoveTime = currentTime

            val packet = event.packet
            val newSlot = packet.integers.read(0)
            val oldSlot = player.inventory.heldItemSlot

            if (newSlot != oldSlot) {
                val scrollDirection = if ((newSlot - oldSlot + 9) % 9 > 4) -1 else 1
                val menuName = state.currentMenu
                val yScrollEnabled = plugin.configManager.customConfig.getBoolean("$menuName.y_scroll", false)

                if (yScrollEnabled) {
                    val direction = if (scrollDirection > 0) "S" else "W"
                    plugin.updatePlayerCoordinates(player, direction)
                }
            }
        }
    }
    private fun processMovement_1_21_3(event: PacketEvent, state: PlayerState, player: Player) {
        val packetString = event.packet.handle.toString()
        val forward = packetString.contains("forward=true")
        val backward = packetString.contains("backward=true")
        val left = packetString.contains("left=true")
        val right = packetString.contains("right=true")
        val isJumping = packetString.contains("jump=true")
        val isDismounting = packetString.contains("shift=true")

        if (isDismounting) {
            event.isCancelled = true
            plugin.updatePlayerCoordinates(player, "Shift")
            state.lastKeyPressed = "Shift"
        }

        if (isJumping) {
            plugin.updatePlayerCoordinates(player, "Space")
            state.lastKeyPressed = "Space"
        }

        val direction = when {
            forward -> "W"
            backward -> "S"
            right -> "D"
            left -> "A"
            else -> null
        }

        direction?.let {
            state.lastKeyPressed = it
            plugin.updatePlayerCoordinates(player, it)
        }
    }

    private fun processMovement_legacy(event: PacketEvent, state: PlayerState, player: Player) {
        val packet = event.packet
        val sideways = packet.float.read(0)
        val forward = packet.float.read(1)
        val isJumping = packet.booleans.read(0)
        val isDismounting = packet.booleans.read(1)

        if (isDismounting) {
            event.isCancelled = true
            plugin.updatePlayerCoordinates(player, "Shift")
            state.lastKeyPressed = "Shift"
        }

        if (isJumping) {
            plugin.updatePlayerCoordinates(player, "Space")
            state.lastKeyPressed = "Space"
        }

        val direction = when {
            forward > 0.0f -> "W"
            forward < 0.0f -> "S"
            sideways < 0.0f -> "D"
            sideways > 0.0f -> "A"
            else -> null
        }
        direction?.let {
            state.lastKeyPressed = it
            plugin.updatePlayerCoordinates(player, it)
        }
    }

    private fun getServerVersion(): Int {
        val serverVersion = Bukkit.getBukkitVersion().split("-")[0]
        val versionParts = serverVersion.split(".").mapNotNull { it.toIntOrNull() }
        return when (versionParts.size) {
            3 -> versionParts[0] * 1000 + versionParts[1] * 10 + versionParts[2]
            2 -> versionParts[0] * 1000 + versionParts[1] * 10
            else -> 0
        }
    }
}
