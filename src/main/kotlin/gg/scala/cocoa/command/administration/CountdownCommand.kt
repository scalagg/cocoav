package gg.scala.cocoa.command.administration

import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.formatIntoDetailedString
import gg.scala.cocoa.parseDuration
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Single
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
@Command("countdown")
@CommandPermission("cocoa.command.countdown")
object CountdownCommand
{
    @Subcommand("staging")
    fun onTopic(
        actor: VelocityCommandActor,
        presence: String
    )
    {
        CocoaCountdown.INSTANCE.presenceFormat = presence

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
        CocoaCountdown.INSTANCE.commencedFormat = presence

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
        CocoaCountdown.INSTANCE.addedAt = System.currentTimeMillis()
        CocoaCountdown.INSTANCE.activated = true

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
        CocoaCountdown.INSTANCE.activated = false

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
            CocoaCountdown.INSTANCE.duration = parseDuration(duration)
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
                        TimeUnit.MILLISECONDS.toSeconds(CocoaCountdown.INSTANCE.duration).toInt()
                    )
                }")
        }
    }

    @Default
    @Subcommand("info")
    fun onInfo(actor: VelocityCommandActor)
    {
        val duration = CocoaCountdown.INSTANCE.duration

        var message = """
            &a&lCocoa Countdown:
            
             &aPresence:
              &7● &fStaging:
               &e${CocoaCountdown.INSTANCE.presenceFormat}
              &7● &fCommenced:
               &e${CocoaCountdown.INSTANCE.commencedFormat}
               
             &aSettings:
              &7● &fActive: ${
                  if (CocoaCountdown.INSTANCE.activated)
                    "&aYes" else "&cNo"
              }
              &7● &fDuration: 
        """.trimIndent()

        message += if (duration == 0L)
        {
            " &cNot set"
        } else
        {
            "\n   &6- &fDetailed: &7${formatIntoDetailedString(TimeUnit.MILLISECONDS.toSeconds(duration).toInt())}\n   &6- &fAbbreviated: &7${formatIntoAbbreviatedString(TimeUnit.MILLISECONDS.toSeconds(duration).toInt())}"
        }


        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message)
        }
    }
}
