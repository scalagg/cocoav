package gg.scala.cocoa.command.administration

import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
import gg.scala.cocoa.permission.annotation.CocoaPermission
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Dependency
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.exception.NoPermissionException
import revxrsal.commands.velocity.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
object CocoaCommand
{
    @Command("center-presence")
    @CocoaPermission("cocoa.command.reload")
    fun onCenter(actor: VelocityCommandActor)
    {
        CocoaSettingsProcessor.locate<CocoaServerPresenceSettings>()?.let {
            it.shouldCenterPresence = !it.shouldCenterPresence

            actor.reply {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&aYou've toggled presence centering. &7(${it.shouldCenterPresence})")
            }
        }

        CocoaSettingsProcessor.saveSettingsToFile(
            CocoaServerPresenceSettings::class, "presence"
        )
    }

    @Command("cocoa-reload")
    @CocoaPermission("cocoa.command.reload")
    fun onReload(actor: VelocityCommandActor)
    {
        val start = System.currentTimeMillis()

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&7Attempting reload of configs...")
        }

        CocoaSettingsProcessor.loadSettings<CocoaVelocitySettings>("settings")

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" &eReloaded &fsettings.json&e... &8(${System.currentTimeMillis() - start}ms)")
        }

        CocoaSettingsProcessor.loadSettings<CocoaServerPresenceSettings>("presence")

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" &eReloaded &fpresence.json&e... &8(${System.currentTimeMillis() - start}ms)")
        }

        CocoaSettingsProcessor.loadSettings<CocoaCountdownSettings>("countdown")
        CocoaCountdown.initialLoad()

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" &eReloaded &7countdown.json&e... &8(${System.currentTimeMillis() - start}ms)")
        }

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&a&lFinished!&a We took ${System.currentTimeMillis() - start}ms in total.")
        }
    }
}
