package gg.scala.cocoa.subscribe

import com.velocitypowered.api.proxy.Player
import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import gg.scala.banana.subscribe.marker.BananaHandler
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.countdown.CocoaCountdownPresence
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.settings.CocoaSettingsModel
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.evilblock.cubed.serializers.Serializers
import java.util.*

/**
 * @author GrowlyX
 * @since 9/12/2021
 */
object CocoaRedisSubscriber : BananaHandler
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

    // temporary !!!
    val settings = mutableMapOf(
        "countdown" to CocoaCountdownSettings::class,
        "settings" to CocoaVelocitySettings::class,
        "presence" to CocoaCountdownPresence::class
    )

    @Subscribe("cloud-sync")
    fun onCloudSync(message: Message)
    {
        println("[Cocoa] Started cloud sync request!")

        settings.forEach {
            CocoaVelocityPlugin.INSTANCE.cloudLayer
                .fetchEntryByKey(it.key).thenAccept { serialized ->
                    val settings = Serializers.gson.fromJson(
                        serialized, it.value.java
                    )

                    CocoaSettingsProcessor.settings[it.value] =
                        settings as CocoaSettingsModel

                    CocoaSettingsProcessor.saveSettingsToFile(
                        it.value, it.key
                    )
                }

            println("[Cocoa] Processing cloud sync for: ${it.value.java.simpleName}")
        }

        println("[Cocoa] Completed cloud sync request!")
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
