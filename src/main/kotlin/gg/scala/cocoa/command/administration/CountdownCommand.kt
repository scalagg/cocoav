package gg.scala.cocoa.command.administration

import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.formatIntoDetailedString
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
object CountdownCommand
{
    @Subcommand("staging")
    @CocoaPermission("cocoa.command.countdown")
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
    @CocoaPermission("cocoa.command.countdown")
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
    @CocoaPermission("cocoa.command.countdown")
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
    @CocoaPermission("cocoa.command.countdown")
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
    @CocoaPermission("cocoa.command.countdown")
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

    @Subcommand("info")
    @CocoaPermission("cocoa.command.countdown")
    fun onInfo(actor: VelocityCommandActor)
    {
        val duration = CocoaCountdown.INSTANCE.duration

        val message = """
            &a&lCocoa Countdown:
            
             &aPresence:
              &7● &fStaging:
               ${CocoaCountdown.INSTANCE.presenceFormat}
              &7● &fCommenced:
               ${CocoaCountdown.INSTANCE.commencedFormat}
               
             &fSettings:
              &7● &fActive: ${
                  if (CocoaCountdown.INSTANCE.activated)
                    "&aYes" else "&cNo"
              }
              &7● &fDuration: 
              ${
                  if (duration == 0L)
                  {
                      " &cNot set"
                  } else
                  {
                      """
                           &6- &fDetailed: &7${
                               formatIntoDetailedString(
                                   TimeUnit.MILLISECONDS.toSeconds(duration).toInt()
                               )
                           }
                           &6- &fAbbreviated: &7${
                               formatIntoDetailedString(
                                   TimeUnit.MILLISECONDS.toSeconds(duration).toInt()
                               )
                           }
                      """.trimIndent()
                  }
              }
            
        """.trimIndent()
    }
}
