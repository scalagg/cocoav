package gg.scala.cocoa.listener

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.handler.CocoaRedisHandler
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.permission.CocoaPlayerPermissions
import gg.scala.cocoa.permission.CocoaPlayerPermissions.hasCocoaPermission
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
                event.result = ResultedEvent.ComponentResult.denied(
                    CocoaVelocityConstants.MAINTENANCE
                )

                DataStoreHandler.rankOf.remove(uniqueId)
                return
            }
        }
    }

    @JvmStatic
    val TWO_MILLI: Duration = Duration.ofMillis(2L)

    @JvmStatic
    val HUNDRED_MILLI: Duration = Duration.ofMillis(100L)

    @Subscribe
    fun onDisconnect(event: DisconnectEvent)
    {
        val isStaffMember = event.player.hasPermission("cocoa.staff")

        plugin.server.scheduler.buildTask(plugin) {
            val player = plugin.server
                .getPlayer(event.player.uniqueId)

            if (player == null)
            {
                val serverName = event.player.currentServer.orElse(null)?.serverInfo?.name ?: "unknown"

                if (isStaffMember)
                {
                    CocoaRedisHandler.sendStaffNotification(
                        event.player,
                        "§b${color(event.player.uniqueId) + event.player.username}§3 disconnected from §b$serverName§3."
                    )
                }

                DataStoreHandler.rankOf.remove(event.player.uniqueId)
            }
        }.delay(TWO_MILLI).schedule()
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

        if (event.player.hasPermission("cocoa.staff"))
        {
            val serverName = event.server.serverInfo.name

            if (previousServer != null)
            {
                CocoaRedisHandler.sendStaffNotification(
                    event.player,
                    "§b${color(event.player.uniqueId) + event.player.username}§3 joined §b$serverName§3 from §b${previousServer.serverInfo.name}§3."
                )
            } else
            {
                plugin.server.scheduler.buildTask(plugin) {
                    CocoaRedisHandler.sendStaffNotification(
                        event.player,
                        "§b${color(event.player.uniqueId) + event.player.username}§3 connected to §b$serverName§3."
                    )
                }.delay(HUNDRED_MILLI).schedule()
            }
        }
    }

    @Subscribe
    fun onKick(event: KickedFromServerEvent)
    {
        if (!plugin.isLobbyServer(event.server.serverInfo))
        {
            val newServer = if (event.player.hasCocoaPermission("cocoa.staff"))
                plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

            event.serverKickReason.ifPresent {
                event.player.sendMessage(CocoaVelocityConstants.DOWNED)
                event.player.sendMessage(it)
            }

            event.result = KickedFromServerEvent
                .RedirectPlayer.create(newServer)
        }
    }
}

fun color(uuid: UUID): String
{
    return DataStoreHandler.findRank(
        DataStoreHandler.rankOf[uuid] ?: DataStoreHandler.getDefaultRank().uuid
    )?.color ?: ""
}
