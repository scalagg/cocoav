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
    val NETWORKING_ERROR = LegacyComponentSerializer
        .legacyAmpersand()
        .deserialize("&8»&7»&f»   &6&lSolara   &f«&7«&8«&r\n&r\n&cWe were unable to contact an internal service.\n&cPlease contact a system administrator!\n&r\n&cThink this is a mistake: https://discord.solara.gg")

    @JvmStatic
    val NETWORKING_INVALID = LegacyComponentSerializer
        .legacyAmpersand()
        .deserialize("&8»&7»&f»   &6&lSolara   &f«&7«&8«&r\n&r\n&fYour IP address has been flagged as malicious.\n&fPlease disable your VPN / Proxy.\n&r\n&cThink this is a mistake: https://discord.solara.gg")

    @JvmStatic
    val DENIED_NETWORKING_ERROR = ResultedEvent
        .ComponentResult.denied(NETWORKING_ERROR)!!

    @JvmStatic
    val DENIED_NETWORKING_INVALID = ResultedEvent
        .ComponentResult.denied(NETWORKING_INVALID)!!

    const val DENIED_SERVER_ENTRY = "&8»&7»&f»   &6&lSolara   &f«&7«&8«&r\n&r\n&cYour connection to &c&l%s&c has been prevented due to you being %s.\n&r\n&cThink this is a mistake: https://discord.solara.gg"
}