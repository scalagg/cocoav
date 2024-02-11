package gg.scala.cocoa.countdown

import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.formatIntoDetailedString
import gg.scala.cocoa.presence.override.PresenceOverride
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
    override fun getWeight() = 30000

    override fun ableToBeManuallyToggle() = false
    override fun shouldOverride() = CocoaVelocitySettingsDataSync.cached().countdown.active

    fun getStagingFooter(): TextComponent?
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        val secondsUntilEnd = TimeUnit.MILLISECONDS.toSeconds(
            (settings.countdown.duration + settings.countdown.addedAt) - System.currentTimeMillis()
        ).toInt()

        val ended = secondsUntilEnd < 0

        val detailed = formatIntoDetailedString(secondsUntilEnd)
        val abbreviated = formatIntoAbbreviatedString(secondsUntilEnd)

        if (ended)
        {
            return null
        }

        return LegacyComponentSerializer.legacyAmpersand()
            .deserialize(
                settings.countdown.presenceStaging
                    .replace("[abbrev]", abbreviated)
                    .replace("[detailed]", detailed)
            )
    }

    override fun updatePresence(): TextComponent
    {
        val settings = CocoaVelocitySettingsDataSync.cached()

        val firstLineOfNormal = MiniMessage
            .miniMessage().deserialize(
                settings.presence.normalPresence.split("\n")[0]
            )

        val secondsUntilEnd = TimeUnit.MILLISECONDS.toSeconds(
            (settings.countdown.duration + settings.countdown.addedAt) - System.currentTimeMillis()
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
                                    settings.countdown.presenceCommenced
                                } else
                                {
                                    settings.countdown.presenceStaging
                                        .replace("[abbrev]", abbreviated)
                                        .replace("[detailed]", detailed)
                                }
                            )
                    )
            )
    }
}
