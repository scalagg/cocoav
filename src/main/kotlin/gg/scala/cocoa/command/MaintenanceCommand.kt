package gg.scala.cocoa.command

import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.*
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
@Command("maintenance")
@CommandPermission("cocoa.command.maintenance")
object MaintenanceCommand
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Subcommand("update")
    @Description("Update maintenance mode's rank.")
    fun onUpdateRank(sender: VelocityCommandActor, lemonSpigotRank: Rank)
    {
        val currentStatus = plugin.maintenance

        if (!currentStatus)
        {
            throw CommandErrorException("You must enable maintenance mode to perform this command.")
        }

        plugin.updateMaintenanceRank(lemonSpigotRank)

        sender.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aMaintenance mode rank has been set to: &f${lemonSpigotRank.name}")
        }
    }

    @Subcommand("toggle")
    @Description("Toggle maintenance mode.")
    fun onToggle(sender: VelocityCommandActor, @Optional lemonSpigotRank: Rank?)
    {
        val currentStatus = plugin.maintenance

        if (!currentStatus && lemonSpigotRank == null)
        {
            throw CommandErrorException("You must define a rank to toggle maintenance mode")
        }

        plugin.updateMaintenanceMode(lemonSpigotRank)

        sender.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    if (!currentStatus)
                    {
                        CocoaRedisSubscriber.buildMessage(
                            "maintenance-kick",
                            mapOf()
                        ).publish(
                            channel = "cocoa"
                        )

                        "&aMaintenance mode has been enabled."
                    } else
                    {
                        "&cMaintenance mode has been disabled."
                    }
                )
        }
    }

    @Default
    @Subcommand("info")
    @Description("View maintenance information.")
    fun onList(sender: VelocityCommandActor)
    {
        val currentStatus = plugin.maintenance

        if (!currentStatus)
        {
            throw CommandErrorException("Maintenance mode is not enabled.")
        }

        val maintenanceRank = plugin.maintenanceRank()

        sender.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    "&aThe server is currently restricted to players with a rank weight above: &f${maintenanceRank.name}"
                )
        }
    }
}
