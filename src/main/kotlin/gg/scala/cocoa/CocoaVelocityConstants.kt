package gg.scala.cocoa

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaVelocityConstants
{
    @JvmStatic
    val MAINTENANCE = Component.text(
        "Sorry, the server is whitelisted.\nCheck our Discord for more information!"
    ).color(
        TextColor.fromHexString("#ee6a6a")
    )

    @JvmStatic
    val NEW_LINE = LegacyComponentSerializer
        .legacyAmpersand().deserialize("&r\n")
}
