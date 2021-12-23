package gg.scala.cocoa.command.administration

import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.parseDuration
import gg.scala.cocoa.permission.annotation.CocoaPermission
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Single
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
@Command("countdown")
@CocoaPermission("cocoa.command.countdown")
object CountdownCommand
{
    @Subcommand("staging")
    fun onTopic(
        actor: VelocityCommandActor,
        presence: String
    )
    {
        CocoaCountdown.presenceFormat = presence

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've set the &lSTAGING&a presence to:\n$presence")
        }
    }

    @Subcommand("commenced")
    fun onEnded(
        actor: VelocityCommandActor,
        presence: String
    )
    {
        CocoaCountdown.commencedFormat = presence

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've set the &lENDED&a presence to:\n$presence")
        }
    }

    @Subcommand("start-now")
    fun onStartNow(actor: VelocityCommandActor)
    {
        CocoaCountdown.addedAt = System.currentTimeMillis()
        CocoaCountdown.activated = true

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've started the countdown! &lPresence will update shortly.")
        }
    }

    @Subcommand("stop")
    fun onStop(actor: VelocityCommandActor)
    {
        CocoaCountdown.activated = false

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've stopped the countdown.")
        }
    }

    @Subcommand("duration")
    fun onDuration(
        actor: VelocityCommandActor,
        @Single duration: String
    )
    {
        try
        {
            CocoaCountdown.duration = parseDuration(duration)
        } catch (ignored: Exception)
        {
            throw CommandErrorException("That is an invalid date!")
        }

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've set the &lDURATION&a to:\n${
                    formatIntoAbbreviatedString(
                        TimeUnit.MILLISECONDS.toSeconds(CocoaCountdown.duration).toInt()
                    )
                }")
        }
    }
}
