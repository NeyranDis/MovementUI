import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
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

        if (state.navigationMode) {
            val currentTime = System.currentTimeMillis()
            val delay = plugin.settingsConfig.getInt("detect-delay")
            if (state.lastMoveTime?.let { currentTime - it < delay } == true) return

            state.lastMoveTime = currentTime
            processMovement(event, state, player)
        }
    }

    private fun processMovement(event: PacketEvent, state: PlayerState, player: Player) {
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

    private fun disableNavigationMode(state: PlayerState) {
        state.navigationMode = false
        object : BukkitRunnable() {
            override fun run() {
                (Bukkit.getEntity(state.armorStand ?: return) as? ArmorStand)?.remove()
                state.armorStand = null
            }
        }.runTaskLater(plugin, 5L)
    }

    private fun Material.isAirCompatible(): Boolean =
        this == Material.AIR || this == Material.CAVE_AIR || this == Material.VOID_AIR
}