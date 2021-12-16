package gg.scala.cocoa.handler

import com.velocitypowered.api.proxy.Player
import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import gg.scala.banana.subscribe.marker.BananaHandler
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import java.util.*

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
object CocoaRedisHandler : BananaHandler
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    @Subscribe("maintenance-kick")
    fun onMaintenanceKick(message: Message)
    {
        plugin.server.allPlayers
            .filter { !plugin.shouldAllowOrStay(it.uniqueId) }
            .forEach {
                it.disconnect(CocoaVelocityConstants.MAINTENANCE)
            }
    }

    fun sendStaffNotification(
        sender: Player?,
        notification: String,
    )
    {
        buildMessage(
            "staff-message",
            HashMap<String, String>().also {
                it["sender-fancy"] = sender?.username ?: ""
                it["message"] = notification
                it["permission"] = "lemon.staff"
                it["message-type"] = "NOTIFICATION"

                it["server"] = "proxy"
                it["with-server"] = "false"
            }
        ).dispatch(
            plugin.spigotBanana
        )
    }

    fun buildMessage(packet: String, message: Map<String, String>): Message
    {
        return Message(packet).also {
            message.forEach { (key, value) ->
                it[key] = value
            }
        }
    }
}
