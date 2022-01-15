package gg.scala.cocoa

import com.velocitypowered.api.event.ResultedEvent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaVelocityConstants
{
    @JvmStatic
    val MAINTENANCE by lazy {
        LegacyComponentSerializer.legacyAmpersand()
            .deserialize(
                """
                    &cSorry, we're currently whitelisted.
                    &cAdditional info may be found on discord.
                """.trimIndent()
            )
    }

    @JvmStatic
    val NETWORKING_ERROR = LegacyComponentSerializer
        .legacyAmpersand()
        .deserialize("&cWe were unable to contact the API.\n&cPlease contact a system administrator!")

    @JvmStatic
    val NETWORKING_INVALID = LegacyComponentSerializer
        .legacyAmpersand()
        .deserialize("&cYou cannot join with a VPN!")

    @JvmStatic
    val DENIED_MAINTENANCE = ResultedEvent
        .ComponentResult.denied(MAINTENANCE)!!

    @JvmStatic
    val DENIED_NETWORKING_ERROR = ResultedEvent
        .ComponentResult.denied(NETWORKING_ERROR)!!

    @JvmStatic
    val DENIED_NETWORKING_INVALID = ResultedEvent
        .ComponentResult.denied(NETWORKING_INVALID)!!

    @JvmStatic
    val NEW_LINE = LegacyComponentSerializer
        .legacyAmpersand().deserialize("&r\n")
}
