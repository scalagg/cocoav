package gg.scala.cocoa.command.administration

import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
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
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Command("cocoa-reload")
    fun onReload(actor: VelocityCommandActor)
    {
        if (!actor.isConsole)
        {
            if (!actor.requirePlayer().hasCocoaPermission("cocoa.command.reload"))
            {
                throw CommandErrorException("You do not have permission to perform this command!")
            }
        }

        val start = System.currentTimeMillis()

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&7Attempting reload of configs...")
        }

        // temporary !!!
        CocoaSettingsProcessor.loadSettings<CocoaVelocitySettings>("settings")

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" &eReloaded &fsettings.json&e...")
        }

        CocoaSettingsProcessor.loadSettings<CocoaServerPresenceSettings>("presence")

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(" &eReloaded &fpresence.json&e...")
        }

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&a&lFinished!&a We took ${System.currentTimeMillis() - start}ms.")
        }
    }
}
