package gg.scala.cocoa.countdown

import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.formatIntoDetailedString
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.presence.override.PresenceOverride
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object CocoaCountdownPresence : PresenceOverride
{
    override fun getUniqueId() = "countdown"
    override fun getWeight() = 5

    override fun ableToBeManuallyToggle() = false
    override fun shouldOverride() = CocoaCountdown.INSTANCE.activated

    override fun updatePresence(): TextComponent
    {
        val settings = CocoaSettingsProcessor
            .locate<CocoaServerPresenceSettings>()!!

        val firstLineOfNormal = MiniMessage
            .miniMessage().deserialize(
                settings.normalPresence
                    .split("\n")[0]
            )

        val secondsUntilEnd = TimeUnit.MILLISECONDS.toSeconds(
            (CocoaCountdown.INSTANCE.duration + CocoaCountdown.INSTANCE.addedAt) - System.currentTimeMillis()
        ).toInt()

        val ended = secondsUntilEnd < 0

        val detailed = formatIntoDetailedString(secondsUntilEnd)
        val abbreviated = formatIntoAbbreviatedString(secondsUntilEnd)

        return Component
            .textOfChildren(
                firstLineOfNormal
                    .append(Component.newline())
                    .append(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(
                                if (ended)
                                {
                                    CocoaCountdown.INSTANCE.commencedFormat
                                } else
                                {
                                    CocoaCountdown.INSTANCE.presenceFormat
                                        .replace("[abbrev]", abbreviated)
                                        .replace("[detailed]", detailed)
                                }
                            )
                    )
            )
    }
}
