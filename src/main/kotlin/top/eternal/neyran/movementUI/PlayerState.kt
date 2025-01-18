package top.eternal.neyran.movementUI

data class PlayerState(
    var navigationMode: Boolean = false,
    var lastKeyPressed: String? = null,
    var x: Int = 0,
    var y: Int = 0,
    var z: Int = 0,
    var currentMenu: String = "default",
    var lastMoveTime: Long? = null,
)