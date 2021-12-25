package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission

/**
 * @author GrowlyX
 * @since 12/19/2021
 */
object SendCommand
{
    @Command("send")
    @CommandPermission("cocoa.command.send")
    fun onSend(
        actor: VelocityCommandActor,
        player: Player, server: String
    )
    {
        val registered = actor.server.getServer(server).orElse(null)
            ?: throw CommandErrorException("Sorry, that server does not exist.")

        player.createConnectionRequest(registered)
            .connect().thenAccept {
                actor.reply {
                    LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(
                            if (it.isSuccessful)
                            {
                                "&cSorry, we were unable to send this player."
                            } else
                            {
                                "&aThat player was sent successfully!"
                            }
                        )
                }
            }
    }

    @Command("sendall")
    @CommandPermission("cocoa.command.sendall")
    fun onSend(
        actor: VelocityCommandActor, server: String
    )
    {
        val registered = actor.server.getServer(server).orElse(null)
            ?: throw CommandErrorException("Sorry, that server does not exist.")

        var successful = 0
        var declined = 0

        actor.server.allPlayers.forEach {
            it.createConnectionRequest(registered)
                .connect().thenAccept { request ->
                    if (request.isSuccessful)
                    {
                        successful++
                    } else
                    {
                        declined++
                    }
                }
        }

        actor.reply {
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
