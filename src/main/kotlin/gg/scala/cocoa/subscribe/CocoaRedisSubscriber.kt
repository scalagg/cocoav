package gg.scala.cocoa.subscribe

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.aware.annotation.Subscribe
import gg.scala.aware.message.AwareMessage
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
object CocoaRedisSubscriber
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    @Subscribe("maintenance-kick")
    fun onMaintenanceKick(message: AwareMessage)
    {
        plugin.server.allPlayers
            .filter { !plugin.shouldAllowOrStay(it.uniqueId) }
            .forEach {
                it.disconnect(CocoaVelocityConstants.MAINTENANCE)
            }
    }

    @Subscribe("add-server")
    fun onAddServer(message: AwareMessage)
    {
        val id = message.retrieve<String>("id")

        val server = plugin.server.allServers.firstOrNull {
            it.serverInfo.name.equals(id, true)
        }

        if (server != null)
            return

        plugin.server.registerServer(
            ServerInfo(id, InetSocketAddress(
                message.retrieve<String>("address"),
                message.retrieve("port")
            ))
        )
    }

    @Subscribe("remove-server")
    fun onRemoveServer(message: AwareMessage)
    {
        val server = plugin.server.allServers.firstOrNull {
            it.serverInfo.name.equals(message.retrieve("id"), true)
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
    fun onCloudSync(message: AwareMessage)
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
        ).publish()
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
