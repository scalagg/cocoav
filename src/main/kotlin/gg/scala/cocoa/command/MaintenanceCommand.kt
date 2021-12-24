package gg.scala.cocoa.command

import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.cocoa.lemon.rank.LemonSpigotRank
import gg.scala.cocoa.permission.annotation.CocoaPermission
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Dependency
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor

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
    @CocoaPermission("cocoa.command.xserver")
    fun onUpdateRank(sender: VelocityCommandActor, lemonSpigotRank: LemonSpigotRank)
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
    @CocoaPermission("cocoa.command.xserver")
    fun onToggle(sender: VelocityCommandActor, lemonSpigotRank: LemonSpigotRank?)
    {
        val currentStatus = plugin.maintenance

        if (!currentStatus && lemonSpigotRank == null)
        {
            throw CommandErrorException("You must define a rank to toggle maintenance mode")
        }

        plugin.updateMaintenanceMode(lemonSpigotRank).thenRun {
            sender.reply {
                LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(
                        if (!currentStatus)
                        {
                            CocoaRedisSubscriber.buildMessage(
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
    @CocoaPermission("cocoa.command.xserver")
    @Description("View maintenance information.")
    fun onList(sender: VelocityCommandActor)
    {
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
