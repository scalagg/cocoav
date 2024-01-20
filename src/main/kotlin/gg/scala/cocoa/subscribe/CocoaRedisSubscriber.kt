package gg.scala.cocoa.subscribe

import com.velocitypowered.api.proxy.Player
import gg.scala.aware.annotation.Subscribe
import gg.scala.aware.message.AwareMessage
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
object CocoaRedisSubscriber
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    @Subscribe("maintenance-kick")
    fun onMaintenanceKick(message: AwareMessage)
    {
        val notification = CocoaVelocitySettingsDataSync.cached()
            .presence.whitelistedNotification

        val component = MiniMessage.miniMessage().deserialize(notification)

        plugin.server.allPlayers
            .filter { !plugin.shouldAllowOrStay(it.uniqueId) }
            .forEach {
                it.disconnect(component)
            }
    }

    fun sendStaffNotification(
        sender: Player?,
        notification: String,
        permission: String = "scstaff.staff-member",
        flag: String? = null
    )
    {
        CocoaVelocityPlugin.INSTANCE.server.scheduler
            .buildTask(CocoaVelocityPlugin.INSTANCE) {
                buildMessage(
                    "staff-message",
                    HashMap<String, String>().also {
                        it["message"] = notification
                        it["from"] = "proxy"
                        it["includeFrom"] = "false"

                        if (flag != null)
                        {
                            it["additionalFlag"] = flag
                        }
                    }
                ).publish(
                    channel = "staff:messages"
                )
            }.delay(100L, TimeUnit.MILLISECONDS).schedule()
    }

    fun buildMessage(
        packet: String,
        message: Map<String, String>
    ): AwareMessage
    {
        return AwareMessage.of(
            packet, plugin.spigotChannel
        ).apply {
            for (entry in message)
            {
                this.assign(entry.key, entry.value)
            }
        }
    }
}
