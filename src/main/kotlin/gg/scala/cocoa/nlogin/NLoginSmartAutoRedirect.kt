package gg.scala.cocoa.nlogin

import com.nickuc.login.api.event.velocity.auth.AuthenticateEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.time.Duration

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
        checkRedirectAvailability(player)
    }

    fun checkRedirectAvailability(player: Player)
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
        } else
        {
            player.sendMessage(Component
                .text("There are no servers online! We're searching for an available one right now...")
                .color(NamedTextColor.RED))

            plugin.server.scheduler.buildTask(plugin) {
                if (player.isActive)
                {
                    checkRedirectAvailability(player)
                }
            }.delay(Duration.ofSeconds(1L)).schedule()
        }
    }
}
