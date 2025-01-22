package gg.scala.cocoa.limbo

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.listener.PlayerListener
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent

/**
 * @author GrowlyX
 * @since 10/4/2024
 */
class LimboPrep : LimboAccess
{
   val plugin = CocoaVelocityPlugin.Companion.INSTANCE
   val limboServer = LimboServer(plugin)

    @Subscribe
    fun onLoginLimboRegister(event: LoginLimboRegisterEvent)
    {
        val server = if (event.player.hasPermission("cocoa.staff"))
            plugin.fetchBestRestrictedLobby(event.player, null)
        else plugin.fetchBestLobby(event.player, null)

//        event.setOnKickCallback(PlayerListener::onKick)

        if (server == null)
        {
            event.addOnJoinCallback {
                limboServer.server.spawnPlayer(event.player, LimboHandler(this) {
                    plugin.fetchBestLobby(event.player)
                })
            }
        }
    }

    override fun spawn(player: Player)
    {
        limboServer.server.spawnPlayer(player, LimboHandler(this) {
            plugin.fetchBestLobby(player)
        })
    }
}
