package gg.scala.cocoa.subscribe

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.aware.annotation.Subscribe
import gg.scala.aware.message.AwareMessage
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.store.storage.type.DataStoreStorageType
import net.kyori.adventure.text.minimessage.MiniMessage
import java.net.InetSocketAddress
import java.util.*
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
        val notification = CocoaSettingsProcessor
            .locate<CocoaServerPresenceSettings>()
            ?.whitelistedNotification
            ?: "<red>We're currently whitelisted."

        val component = MiniMessage.miniMessage().deserialize(notification)

        plugin.server.allPlayers
            .filter { !plugin.shouldAllowOrStay(it.uniqueId) }
            .forEach {
                it.disconnect(component)
            }
    }

    @Subscribe("add-server-DEPRECATED")
    fun onAddServer(message: AwareMessage)
    {
        val id = message
            .retrieve("id", String::class)

        val server = plugin.server.allServers.firstOrNull {
            it.serverInfo.name.equals(id, true)
        }

        if (server != null)
            return

        plugin.server.registerServer(
            ServerInfo(
                id, InetSocketAddress(
                    message.retrieve("address", String::class),
                    message.retrieve("port", Int::class)
                )
            )
        )
    }

    @Subscribe("remove-server-DEPRECATED")
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

    //    @Subscribe("cloud-sync")
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
