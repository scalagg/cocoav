package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.formatIntoAbbreviatedString
import gg.scala.cocoa.formatIntoDetailedString
import gg.scala.cocoa.parseDuration
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
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
    ) = CocoaVelocitySettingsDataSync
        .useThenSave {
            countdown.presenceStaging = presence
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aYou've set the &6&lSTAGING&a presence to:\n$presence")
            }
        }

    @Subcommand("presence commenced")
    @Description("Set the commenced presence of the countdown.")
    fun onEnded(
        actor: Player,
        presence: String
    ) = CocoaVelocitySettingsDataSync
        .useThenSave {
            countdown.presenceCommenced = presence
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aYou've set the &d&lCOMMENCED&a presence to:\n$presence")
            }
        }

    @Subcommand("manager start")
    @Description("Start the countdown with the current configuration.")
    fun onStartNow(actor: Player) = CocoaVelocitySettingsDataSync
        .useThenSave {
            countdown.addedAt = System.currentTimeMillis()
            countdown.active = true
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aYou've started the countdown! The settings  will propagate on all registered proxies shortly.")
            }
        }

    @Subcommand("manager stop")
    @Description("End the countdown and restore previous presence state.")
    fun onStop(actor: Player) = CocoaVelocitySettingsDataSync
        .useThenSave {
            countdown.addedAt = System.currentTimeMillis()
            countdown.active = false
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&cYou've stopped the countdown! The presence on all registered proxies will update shortly.")
            }
        }

    @Subcommand("manager duration")
    @Description("Set the duration of the countdown from its start date.")
    fun onDuration(
        actor: Player,
        @Single duration: String
    ) = CocoaVelocitySettingsDataSync
        .useThenSave {
            try
            {
                countdown.duration = parseDuration(duration)
            } catch (ignored: Exception)
            {
                throw ConditionFailedException("That is an invalid duration!")
            }
        }
        .thenRun {
            actor.sendMessage {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(
                        "&aYou've set the &lDURATION&a to:\n$duration"
                    )
            }
        }

    @Subcommand("manager configuration")
    @Description("View the current configuration as a fancy message.")
    fun onInfo(actor: Player)
    {
        val countdown = CocoaVelocitySettingsDataSync.cached().countdown

        var message = """
            &a&lCocoa Countdown:
            
             &aPresence:
              &7● &fStaging:
               &e${countdown.presenceStaging}
              &7● &fCommenced:
               &e${countdown.presenceCommenced}
               
             &aSettings:
              &7● &fActive: ${
            if (countdown.active)
                "&aYes" else "&cNo"
        }
              &7● &fDuration: 
        """.trimIndent()

        message += if (countdown.duration == 0L)
        {
            " &cNot set"
        } else
        {
            "\n   &6- &fDetailed: &7${
                formatIntoDetailedString(
                    TimeUnit.MILLISECONDS.toSeconds(countdown.duration).toInt()
                )
            }\n   &6- &fAbbreviated: &7${
                formatIntoAbbreviatedString(
                    TimeUnit.MILLISECONDS.toSeconds(countdown.duration).toInt()
                )
            }"
        }

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message)
        }
    }
}
