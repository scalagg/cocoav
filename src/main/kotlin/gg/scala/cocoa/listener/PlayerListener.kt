package gg.scala.cocoa.listener

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.network.CocoaNetworkAddressHandler
import gg.scala.cocoa.permission.CocoaPlayerPermissions
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.time.Duration
import java.util.*

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object PlayerListener
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    @Subscribe(
        order = PostOrder.FIRST
    )
    fun onLogin(event: LoginEvent)
    {
        val uniqueId = event.player.uniqueId

        val loadedRank = DataStoreHandler.loadRankOfNow(event.player.uniqueId)
        DataStoreHandler.rankOf[uniqueId] = loadedRank.uuid

        if (plugin.maintenance)
        {
            if (!plugin.shouldAllowOrStay(uniqueId))
            {
                event.result = CocoaVelocityConstants
                    .DENIED_MAINTENANCE

                DataStoreHandler.rankOf.remove(uniqueId)
                return
            }
        }

        val settings = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!

        if (settings.networking.handleNetworkingChecks)
        {
            try
            {
                val networkModel = CocoaNetworkAddressHandler
                    .requestHostData(
                        event.player.remoteAddress
                            .address.hostAddress
                    )

                if (networkModel == null)
                {
                    event.result = CocoaVelocityConstants
                        .DENIED_NETWORKING_ERROR
                } else
                {
                    if (networkModel.shouldBlock)
                    {
                        event.result = CocoaVelocityConstants
                            .DENIED_NETWORKING_INVALID

                        CocoaRedisSubscriber.sendStaffNotification(
                            null, "§4§o${
                                event.player.username
                            }§c tried logging on with a VPN.",
                            "lemon.networking"
                        )
                    }
                }
            } catch (ignored: Exception)
            {
                event.result = CocoaVelocityConstants
                    .DENIED_NETWORKING_ERROR
            }
        }

        if (!event.result.isAllowed)
        {
            DataStoreHandler.rankOf.remove(uniqueId)
        }
    }

    @JvmStatic
    val HUNDRED_MILLI: Duration = Duration.ofMillis(100L)

    @JvmStatic
    val TWO_HUNDRED_MILLI: Duration = Duration.ofMillis(200L)

    @Subscribe
    fun onDisconnect(event: DisconnectEvent)
    {
        val isStaffMember = event.player
            .hasCocoaPermission("cocoa.staff")

        val serverName = event.player.currentServer
            .orElse(null)?.serverInfo?.name ?: "unknown"

        if (isStaffMember)
        {
            CocoaRedisSubscriber.sendStaffNotification(
                event.player,
                "§b${color(event.player.uniqueId) + event.player.username}§3 disconnected from §b$serverName§3."
            )
        }

        DataStoreHandler.rankOf.remove(event.player.uniqueId)
    }

    @Subscribe(
        order = PostOrder.FIRST
    )
    fun onPermissionSetup(event: ServerPreConnectEvent)
    {
        CocoaPlayerPermissions.calculatePermissions(event.player.uniqueId)
    }

    @Subscribe
    fun onServerPreConnect(event: ServerPreConnectEvent)
    {
        val isStaffMember = event.player.hasCocoaPermission("cocoa.staff")

        if (event.originalServer == null)
        {
            event.result = ServerPreConnectEvent
                .ServerResult.allowed(
                    if (isStaffMember)
                        plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()
                )
        }
    }

    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent)
    {
        val previousServer = event.previousServer
            .orElse(null)

        if (event.player.hasCocoaPermission("cocoa.staff"))
        {
            val serverName = event.server.serverInfo.name

            if (previousServer != null)
            {
                CocoaRedisSubscriber.sendStaffNotification(
                    event.player,
                    "§b${color(event.player.uniqueId) + event.player.username}§3 joined §b$serverName§3 from §b${previousServer.serverInfo.name}§3."
                )
            } else
            {
                plugin.server.scheduler.buildTask(plugin) {
                    CocoaRedisSubscriber.sendStaffNotification(
                        event.player,
                        "§b${color(event.player.uniqueId) + event.player.username}§3 connected to §b$serverName§3."
                    )
                }.delay(HUNDRED_MILLI).schedule()
            }
        }
    }

    @JvmStatic
    val NOTHING = Component.text("N/A")

    @Subscribe
    fun onKick(event: KickedFromServerEvent)
    {
        val kickedServer = event.server.serverInfo

        val newServer = if (event.player.hasCocoaPermission("cocoa.staff"))
            plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

        val newComponent = LegacyComponentSerializer.legacyAmpersand()
            .deserialize("&cYou've been kicked from ${kickedServer.name}:&r\n")
            .append(event.serverKickReason.orElse(NOTHING))

        if (newServer != null)
        {
            event.result = KickedFromServerEvent
                .RedirectPlayer.create(newServer, newComponent)
        } else
        {
            event.result = KickedFromServerEvent
                .DisconnectPlayer.create(newComponent)
        }
    }

    private fun color(uuid: UUID): String
    {
        return DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uuid] ?: DataStoreHandler.getDefaultRank().uuid
        )?.color ?: ""
    }
}
