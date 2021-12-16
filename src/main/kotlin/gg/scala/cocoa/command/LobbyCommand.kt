package gg.scala.cocoa.command

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Dependency
import revxrsal.commands.exception.CommandErrorException

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
object LobbyCommand
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @Command("hub")
    fun onHub(player: Player)
    {
        val currentServer = player.currentServer.orElse(null)
            ?: throw CommandErrorException("You're not connected to a server.")

        if (plugin.isLobbyServer(currentServer.serverInfo))
        {
            throw CommandErrorException("You're already connected to a hub.")
        }

        val isStaffMember = player
            .hasCocoaPermission("cocoa.staff")

        val bestHubServer = if (isStaffMember)
            plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

        player.createConnectionRequest(bestHubServer)
            .connect()
    }
}
