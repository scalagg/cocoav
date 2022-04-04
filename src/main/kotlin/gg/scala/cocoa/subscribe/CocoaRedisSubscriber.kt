package gg.scala.cocoa.subscribe

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.banana.annotate.Subscribe
import gg.scala.banana.message.Message
import gg.scala.banana.subscribe.marker.BananaHandler
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.store.storage.type.DataStoreStorageType
import java.net.InetSocketAddress
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

    @Subscribe("add-server")
    fun onAddServer(message: Message)
    {
        val server = plugin.server.allServers.firstOrNull {
            it.serverInfo.name.equals(message["id"]!!, true)
        }

        if (server != null)
            return

        plugin.server.registerServer(
            ServerInfo(message["id"]!!, InetSocketAddress(message["address"]!!, message["port"]!!.toInt()))
        )
    }

    @Subscribe("remove-server")
    fun onRemoveServer(message: Message)
    {
        val server = plugin.server.allServers.firstOrNull {
            it.serverInfo.name.equals(message["id"]!!, true)
        } ?: throw RuntimeException("That server does not exist.")

        plugin.server.unregisterServer(server.serverInfo)
    }

    // temporary !!!
    val settings = mutableMapOf(
        "countdown" to CocoaCountdownSettings::class,
        "settings" to CocoaVelocitySettings::class,
        "presence" to CocoaServerPresenceSettings::class
    )

    @Subscribe("cloud-sync")
    fun onCloudSync(message: Message)
    {
        println("[Cocoa] Started cloud sync request!")

        settings.forEach {
            CocoaVelocityPlugin.INSTANCE.cloudLayer
                .loadAll(DataStoreStorageType.REDIS)
                .thenAccept { models ->
                    models.forEach { serialized ->
                        CocoaSettingsProcessor.settings[it.value] =
                            serialized.value

                        CocoaSettingsProcessor.saveSettingsToFile(
                            it.value, it.key, commit = false
                        )
                    }
                }

            println("[Cocoa] Processing cloud sync for: ${it.value.java.simpleName}")
        }

        println("[Cocoa] Completed cloud sync request!")
    }

    fun sendStaffNotification(
        sender: Player?,
        notification: String,
        permission: String = "lemon.staff"
    )
    {
        buildMessage(
            "staff-message",
            HashMap<String, String>().also {
                it["sender-fancy"] = sender?.username ?: ""
                it["message"] = notification
                it["permission"] = permission
                it["message-type"] = "NOTIFICATION"

                it["server"] = "proxy"
                it["with-server"] = "false"
            }
        ).dispatch(
            plugin.spigotChannel
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
