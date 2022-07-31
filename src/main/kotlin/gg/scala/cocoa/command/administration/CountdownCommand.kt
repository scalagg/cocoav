package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.formatIntoDetailedString
import gg.scala.cocoa.parseDuration
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Single
import gg.scala.commons.acf.annotation.Subcommand
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
@CommandAlias("countdown")
@CommandPermission("cocoa.command.countdown")
object CountdownCommand : BaseCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @Subcommand("presence staging")
    @Description("Set the staging presence of the countdown.")
    fun onStaging(
        actor: Player,
        presence: String
    )
    {
        CocoaCountdown.INSTANCE.presenceFormat = presence

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've set the &lSTAGING&a presence to:\n$presence")
        }
    }

    @Subcommand("presence commenced")
    @Description("Set the commenced presence of the countdown.")
    fun onEnded(
        actor: Player,
        presence: String
    )
    {
        CocoaCountdown.INSTANCE.commencedFormat = presence

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've set the &lENDED&a presence to:\n$presence")
        }
    }

    @Subcommand("manager start")
    @Description("Start the countdown with the current configuration.")
    fun onStartNow(actor: Player)
    {
        CocoaCountdown.INSTANCE.addedAt = System.currentTimeMillis()
        CocoaCountdown.INSTANCE.activated = true

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've started the countdown! &lPresence will update shortly.")
        }
    }

    @Subcommand("manager stop")
    @Description("End the countdown and restore previous presence state.")
    fun onStop(actor: Player)
    {
        CocoaCountdown.INSTANCE.activated = false

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've stopped the countdown.")
        }
    }

    @Subcommand("manager duration")
    @Description("Set the duration of the countdown from its start date.")
    fun onDuration(
        actor: Player,
        @Single duration: String
    )
    {
        try
        {
            CocoaCountdown.INSTANCE.duration = parseDuration(duration)
        } catch (ignored: Exception)
        {
            throw ConditionFailedException("That is an invalid date!")
        }

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaCountdownSettings::class, "countdown"
        )

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aYou've set the &lDURATION&a to:\n${
                    formatIntoAbbreviatedString(
                        TimeUnit.MILLISECONDS.toSeconds(CocoaCountdown.INSTANCE.duration).toInt()
                    )
                }")
        }
    }

    @Subcommand("manager configuration")
    @Description("View the current configuration as a fancy message.")
    fun onInfo(actor: Player)
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


        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message)
        }
    }
}
