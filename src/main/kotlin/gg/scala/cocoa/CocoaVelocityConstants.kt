package gg.scala.cocoa

import com.velocitypowered.api.event.ResultedEvent
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.store.velocity.settings.VelocitySettingsProcessor
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
//                VelocitySettingsProcessor
//                    .locate<CocoaServerPresenceSettings>()!!
//                    .whitelistedNotification
            """
                &CThe server is whitelisted!
                &cAdditional information may be found in our discord.
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
    val DENIED_MAINTENANCE by lazy {
        ResultedEvent.ComponentResult.denied(MAINTENANCE)!!
    }

    @JvmStatic
    val DENIED_NETWORKING_ERROR = ResultedEvent
        .ComponentResult.denied(NETWORKING_ERROR)!!

    @JvmStatic
    val DENIED_NETWORKING_INVALID = ResultedEvent
        .ComponentResult.denied(NETWORKING_INVALID)!!

    const val DENIED_SERVER_ENTRY = "&cYour connection to server &c&l%s&c has been prevented due to you being %s."
}
