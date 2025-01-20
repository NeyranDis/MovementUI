package top.eternal.neyran.movementUI

import java.util.UUID

data class PlayerState(
    var navigationMode: Boolean = false,
    var lastKeyPressed: String? = null,
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0,
    var armorStand: UUID? = null,
    var currentMenu: String = "default",
    var lastMoveTime: Long? = null,
)
