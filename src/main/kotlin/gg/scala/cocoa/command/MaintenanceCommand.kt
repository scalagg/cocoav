package gg.scala.cocoa.command

import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.handler.CocoaRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
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
object MaintenanceCommand
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Subcommand("update")
    @Description("Update maintenance mode's rank.")
    fun onUpdateRank(sender: VelocityCommandActor, rank: Rank)
    {
        if (!sender.isConsole)
        {
            if (!sender.requirePlayer().hasCocoaPermission("cocoa.command.maintenance"))
            {
                throw CommandErrorException("You do not have permission to perform this command!")
            }
        }

        val currentStatus = plugin.maintenance

        if (!currentStatus)
        {
            throw CommandErrorException("You must enable maintenance mode to perform this command.")
        }

        plugin.updateMaintenanceRank(rank)

        sender.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&aMaintenance mode rank has been set to: &f${rank.name}")
        }
    }

    @Subcommand("toggle")
    @Description("Toggle maintenance mode.")
    fun onToggle(sender: VelocityCommandActor, rank: Rank?)
    {
        if (!sender.isConsole)
        {
            if (!sender.requirePlayer().hasCocoaPermission("cocoa.command.maintenance"))
            {
                throw CommandErrorException("You do not have permission to perform this command!")
            }
        }

        val currentStatus = plugin.maintenance

        if (!currentStatus && rank == null)
        {
            throw CommandErrorException("You must define a rank to toggle maintenance mode")
        }

        plugin.updateMaintenanceMode(rank).thenRun {
            sender.reply {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(
                        if (!currentStatus)
                        {
                            CocoaRedisHandler.buildMessage(
                                "maintenance-kick",
                                mapOf()
                            ).dispatch(
                                CocoaVelocityPlugin.INSTANCE.proxyBanana,
                            )

                            "&aMaintenance mode has been enabled."
                        } else
                        {
                            "&cMaintenance mode has been disabled."
                        }
                    )
            }
        }
    }

    @Subcommand("info")
    @Description("View maintenance information.")
    fun onList(sender: VelocityCommandActor)
    {
        if (!sender.isConsole)
        {
            if (!sender.requirePlayer().hasCocoaPermission("cocoa.command.maintenance"))
            {
                throw CommandErrorException("You do not have permission to perform this command!")
            }
        }

        val currentStatus = plugin.maintenance

        if (!currentStatus)
        {
            throw CommandErrorException("You must enable maintenance mode to perform this command.")
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
