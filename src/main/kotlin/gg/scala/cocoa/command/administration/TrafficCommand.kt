package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.velocity.contexts.OnlinePlayer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/19/2021
 */
object TrafficCommand : BaseCommand()
{
    @Default
    @HelpCommand
    fun onHelp(help: CommandHelp)
    {
        help.showHelp()
    }

    @CommandAlias("redirect|send")
    @CommandCompletion("@players @servers")
    @CommandPermission("cocoa.command.traffic")
    @Description("Redirect a player to a server.")
    fun onSend(
        actor: Player, player: OnlinePlayer, server: String
    )
    {
        val registered = CocoaVelocityPlugin.INSTANCE.server.getServer(server).orElse(null)
            ?: throw ConditionFailedException("Sorry, that server does not exist.")

        val start = System.currentTimeMillis()

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    "&8Attempting to send that player..."
                )
        }

        player.player.createConnectionRequest(registered)
            .connect().thenAccept {
                if (it.isSuccessful)
                {
                    player.player.sendMessage(
                        LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(
                                "&6You were summoned to &l${registered.serverInfo.name}&6."
                            )
                    )
                }

                actor.sendMessage {
                    LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(
                            if (!it.isSuccessful)
                            {
                                "&cSorry, we were unable to send this player."
                            } else
                            {
                                "&aThat player was sent successfully! &8(${System.currentTimeMillis() - start}ms)"
                            }
                        )
                }
            }
    }

    @CommandCompletion("@servers")
    @CommandAlias("redirectall|sendall")
    @CommandPermission("cocoa.command.traffic")
    @Description("Redirect all player to a server.")
    fun onSend(
        actor: Player, server: String
    )
    {
        val registered = CocoaVelocityPlugin.INSTANCE.server.getServer(server).orElse(null)
            ?: throw ConditionFailedException("Sorry, that server does not exist.")

        var successful = 0
        var declined = 0

        CocoaVelocityPlugin.INSTANCE.server.allPlayers.forEach {
            it.createConnectionRequest(registered)
                .connect().join()
                .apply {
                    if (isSuccessful)
                    {
                        successful++

                        it.sendMessage(
                            LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(
                                    "&6You were summoned to &l${registered.serverInfo.name}&6."
                                )
                        )
                    } else
                    {
                        declined++
                    }
                }
        }

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    """
                        &eWe've connected &l$successful&e players to &f$server&e!
                        &7We were unable to connect &l$declined&7 player though.
                    """.trimIndent()
                )
        }
    }

    @CommandCompletion("@servers")
    @CommandPermission("cocoa.command.traffic")
    @CommandAlias("redirectcurrent|sendcurrent")
    @Description("Redirect all player to a server.")
    fun onSendCurrent(
        actor: Player, server: String
    )
    {
        val registered = CocoaVelocityPlugin.INSTANCE.server.getServer(server).orElse(null)
            ?: throw ConditionFailedException("Sorry, that server does not exist.")

        var successful = 0
        var declined = 0

        CocoaVelocityPlugin.INSTANCE.server.allPlayers
            .filter {
                it.currentServer.isPresent &&
                        it.currentServer.get().server.serverInfo.name == actor.currentServer.get().serverInfo.name
            }.forEach {
                it.createConnectionRequest(registered)
                    .connect().join()
                    .apply {
                        if (isSuccessful)
                        {
                            successful++

                            it.sendMessage(
                                LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(
                                        "&6You were summoned to &l${registered.serverInfo.name}&6."
                                    )
                            )
                        } else
                        {
                            declined++
                        }
                    }
            }

        actor.sendMessage {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    """
                        &eWe've connected &l$successful&e players to &f$server&e!
                        &7We were unable to connect &l$declined&7 player though.
                    """.trimIndent()
                )
        }
    }
}
