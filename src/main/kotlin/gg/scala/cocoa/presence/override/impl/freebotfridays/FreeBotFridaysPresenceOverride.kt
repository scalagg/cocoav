package gg.scala.cocoa.presence.override.impl.freebotfridays

import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.presence.override.PresenceOverride
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object FreeBotFridaysPresenceOverride : PresenceOverride
{
    override fun getUniqueId() = "freebotfridays"
    override fun getWeight() = 20000

    override fun shouldOverride() = FreeBotFridays.isFreeBotFriday()
    override fun updatePresence(): Component
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        val firstLineOfNormal = MiniMessage
            .miniMessage().deserialize(
                settings.presence.normalPresence.split("\n")[0]
            )

        return firstLineOfNormal
            .append(Component.newline())
            .append(
                Component.text {
                    it.append(Component
                        .text("FREE BOT FRIDAYS! ")
                        .decorate(TextDecoration.BOLD)
                        .color(NamedTextColor.GOLD))

                    it.append(Component
                        .text("Custom bots unlocked!")
                        .color(NamedTextColor.GOLD))
                }
            )
    }
}
