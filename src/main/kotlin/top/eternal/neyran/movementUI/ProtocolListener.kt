import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
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
    }

    private fun handlePacket(event: PacketEvent) {
        val player = event.player
        val state = plugin.playerStates[player.name] ?: return

        val currentTime = System.currentTimeMillis()
        if (state.navigationMode && (state.lastMoveTime == null || currentTime - state.lastMoveTime!! >= plugin.settingsConfig.getInt("detect-delay"))) {
            state.lastMoveTime = currentTime
            handleSteering(event, state, player)
        }
    }

    private fun handleSteering(event: PacketEvent, state: PlayerState, player: Player) {
        val packet = event.packet
        val (sideways, forward) = packet.float.run { read(0) to read(1) }
        val (isJumping, isDismounting) = packet.booleans.run { read(0) to read(1) }

        if (isDismounting) {
            event.isCancelled = true
            plugin.updatePlayerCoordinates(player, "Shift")
        }

        if (isJumping) {
            plugin.updatePlayerCoordinates(player, "Space")
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
}
