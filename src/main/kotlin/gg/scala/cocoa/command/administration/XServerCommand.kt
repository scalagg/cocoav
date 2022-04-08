package gg.scala.cocoa.command.administration

import com.velocitypowered.api.proxy.server.ServerInfo
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Default
import revxrsal.commands.annotation.Single
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission
import java.net.InetSocketAddress

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
@Command("xserver")
@CommandPermission("cocoa.command.xserver")
object XServerCommand
{
    @Subcommand("register")
    fun onServerRegister(
        actor: VelocityCommandActor,
        @Single id: String,
        @Single hostName: String,
        port: Int
    )
    {
        actor.server.registerServer(
            ServerInfo(id, InetSocketAddress(hostName, port))
        )

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&e$id&a has been registered.")
        }
    }

    @Subcommand("unregister")
    fun onServerUnregister(
        actor: VelocityCommandActor, @Single id: String
    )
    {
        val server = actor.server.allServers.firstOrNull {
            it.serverInfo.name.equals(id, true)
        } ?: throw CommandErrorException("That server does not exist.")

        actor.server.unregisterServer(server.serverInfo)

        actor.reply {
            LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&e$id&a has been unregistered.")
        }
    }
}
