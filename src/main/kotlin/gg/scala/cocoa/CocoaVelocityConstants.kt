package gg.scala.cocoa

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

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
    val DOWNED = Component.text(
        "The server you are on is now down for:"
    ).color(
        TextColor.fromHexString("#ee6a6a")
    )
}
