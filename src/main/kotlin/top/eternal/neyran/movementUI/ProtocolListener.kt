
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.entity.Player
import org.bukkit.Location
import org.bukkit.scheduler.BukkitRunnable
import top.eternal.neyran.movementUI.MovementsMain
import kotlin.math.atan2

class ProtocolListener(private val plugin: MovementsMain) {

    private val protocolManager = ProtocolLibrary.getProtocolManager()

    fun registerPacketListeners() {
        protocolManager.addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.HIGHEST,
            PacketType.Play.Client.POSITION,
            PacketType.Play.Client.POSITION_LOOK
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
            player.walkSpeed = 0.005f
            val currentTime = System.currentTimeMillis()

            if (state.lastMoveTime != null && currentTime - state.lastMoveTime!! < 100) {
                return
            }

            state.lastMoveTime = currentTime

            val currentLocation = player.location
            val packet = event.packet

            val x = packet.doubles.read(0)
            val z = packet.doubles.read(2)

            val dx = x - currentLocation.x
            val dz = z - currentLocation.z
            if (dx == 0.0 && dz == 0.0) {
                return
            }

            val moveAngle = Math.toDegrees(atan2(-dx, dz))

            val yaw = currentLocation.yaw
            val normalizedYaw = ((yaw % 360) + 360) % 360
            val normalizedMoveAngle = ((moveAngle % 360) + 360) % 360
            val angleDiff = (normalizedMoveAngle - normalizedYaw + 360) % 360

            val direction = when {
                angleDiff in 315.0..360.0 || angleDiff in 0.0..45.0 -> "W"
                angleDiff in 135.0..225.0 -> "S"
                angleDiff in 45.0..135.0 -> "D"
                angleDiff in 225.0..315.0 -> "A"
                else -> null
            }

            direction?.let {
                state.lastKeyPressed = it
                plugin.updatePlayerCoordinates(player, it)
            }
        } else {
            player.walkSpeed = 0.2f
        }
    }
}