package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.velocity.contexts.OnlinePlayer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/19/2021
 */
@CommandAlias("traffic|trfk|proxytraffic")
@CommandPermission("cocoa.command.traffic")
object TrafficCommand : BaseCommand()
{
    @CommandAlias("redirect")
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

    @CommandAlias("redirectall")
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
