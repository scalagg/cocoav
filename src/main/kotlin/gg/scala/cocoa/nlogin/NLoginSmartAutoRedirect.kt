package gg.scala.cocoa.nlogin

import com.nickuc.login.api.event.velocity.auth.AuthenticateEvent
import com.nickuc.login.api.event.velocity.auth.LoginEvent
import com.velocitypowered.api.event.Subscribe
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.listener.PlayerListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * @author GrowlyX
 * @since 10/4/2024
 */
object NLoginSmartAutoRedirect
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    @Subscribe
    fun AuthenticateEvent.on()
    {
        val newServer = if (player.hasPermission("cocoa.staff"))
            plugin.fetchBestRestrictedLobby(player) else plugin.fetchBestLobby(player)

        if (newServer != null)
        {
            player.sendMessage(
                Component.text("Sending you to a lobby...")
                    .color(NamedTextColor.GRAY)
            )

            player.createConnectionRequest(newServer).connect()
        }
    }
}
