package top.eternal.neyran.movementUI.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.regex.Pattern

object ChatUtils {
    fun String.toMiniMessageComponent(): Component {
        val legacyHexPattern = Pattern.compile("&x((&[A-Fa-f0-9]){6})")
        var result = this
        val matcher = legacyHexPattern.matcher(result)
        while (matcher.find()) {
            val fullMatch = matcher.group()
            val hexSequence = fullMatch.replace("&x", "").replace("&", "")
            result = result.replace(fullMatch, "<#${hexSequence}>")
        }
        result = result.replace("&([0-9a-fA-Fk-oK-OrR])".toRegex()) { matchResult ->
            val colorCode = matchResult.groupValues[1]
            when (colorCode.lowercase()) {
                "k" -> "<obfuscate>"
                "l" -> "<bold>"
                "m" -> "<strikethrough>"
                "n" -> "<underline>"
                "o" -> "<italic>"
                "r" -> "<reset>"
                "0" -> "<black>"
                "1" -> "<dark_blue>"
                "2" -> "<dark_green>"
                "3" -> "<dark_aqua>"
                "4" -> "<dark_red>"
                "5" -> "<dark_purple>"
                "6" -> "<gold>"
                "7" -> "<gray>"
                "8" -> "<dark_gray>"
                "9" -> "<blue>"
                "a" -> "<green>"
                "b" -> "<aqua>"
                "c" -> "<red>"
                "d" -> "<light_purple>"
                "e" -> "<yellow>"
                "f" -> "<white>"
                else -> "<reset>"
            }
        }
        return MiniMessage.miniMessage().deserialize(result)
    }
}
