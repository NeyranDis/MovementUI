import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.entity.Player
import top.eternal.neyran.movementUI.MovementsMain

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
        val player: Player = event.player
        val state = plugin.playerStates[player.name] ?: return

        if (state.navigationMode) {
            val currentTime = System.currentTimeMillis()
            val delay = plugin.settingsConfig.getInt("detect-delay")
            if (state.lastMoveTime != null && currentTime - state.lastMoveTime!! < delay) {
                return
            }

            state.lastMoveTime = currentTime

            val packet = event.packet
            val sideways = packet.float.read(0)
            val forward = packet.float.read(1)
            val isJumping = packet.booleans.read(0)
            val isDismounting = packet.booleans.read(1)

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
}