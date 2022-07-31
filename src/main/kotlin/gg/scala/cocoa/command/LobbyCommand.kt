package gg.scala.cocoa.command

import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.commons.acf.BaseCommand
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.Dependency

/**
 * @author GrowlyX
 * @since 12/16/2021
 */
object LobbyCommand : BaseCommand()
{
    @Dependency
    lateinit var plugin: CocoaVelocityPlugin

    @CommandAlias("hub")
    fun onHub(player: Player)
    {
        val currentServer = player.currentServer.orElse(null)
            ?: throw ConditionFailedException("You're not connected to a server.")

        if (plugin.isLobbyServer(currentServer.serverInfo))
        {
            throw ConditionFailedException("You're already connected to a hub.")
        }

        val isStaffMember = player
            .hasPermission("cocoa.staff")

        val bestHubServer = if (isStaffMember)
            plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

        if (bestHubServer == null)
        {
            throw ConditionFailedException("There are no lobbies available.")
        }

        player.createConnectionRequest(bestHubServer)
            .connectWithIndication()
    }
}
