package gg.scala.cocoa.listener

import com.velocitypowered.api.event.Continuation
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.permission.PermissionsSetupEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.lemon.LemonPlayer
import gg.scala.cocoa.lemon.category.PunishmentCategory
import gg.scala.cocoa.network.CocoaNetworkAddressHandler
import gg.scala.cocoa.permission.CocoaPermissionEngine
import gg.scala.cocoa.permission.CocoaPermissionProvider
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.time.Duration
import java.util.*
import java.util.concurrent.ForkJoinPool

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
    fun onLogin(
        event: LoginEvent,
        continuation: Continuation
    )
    {
        val uniqueId = event.player.uniqueId

        ForkJoinPool.commonPool().execute {
            val loadedRank = DataStoreHandler
                .loadRankOfNow(event.player.uniqueId)

            DataStoreHandler.rankOf[uniqueId] = loadedRank.uuid

            if (plugin.maintenance)
            {
                if (!plugin.shouldAllowOrStay(uniqueId))
                {
                    event.result = CocoaVelocityConstants
                        .DENIED_MAINTENANCE

                    DataStoreHandler.rankOf.remove(uniqueId)
                    continuation.resume()
                    return@execute
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

            val lemonPlayer = DataStoreObjectControllerCache.findNotNull<LemonPlayer>()
                .loadAndCache(
                    uniqueId,
                    { LemonPlayer(uniqueId, event.player.username) },
                    DataStoreStorageType.MONGO)
                .join()

            lemonPlayer.recalculatePunishments().join()
            lemonPlayer.checkForIpRelative().join()

            continuation.resume()
        }
    }

    @JvmStatic
    val HUNDRED_MILLI: Duration = Duration.ofMillis(100L)

    @Subscribe
    fun onDisconnect(event: DisconnectEvent)
    {
        val isStaffMember = event.player
            .hasPermission("cocoa.staff")

        val serverName = event.player.currentServer
            .orElse(null)?.serverInfo?.name

        if (isStaffMember && serverName != null)
        {
            CocoaRedisSubscriber.sendStaffNotification(
                event.player,
                "§b${color(event.player.uniqueId) + event.player.username}§3 disconnected from §b$serverName§3."
            )
        }

        DataStoreHandler.rankOf
            .remove(event.player.uniqueId)

        DataStoreObjectControllerCache
            .findNotNull<LemonPlayer>()
            .remove(event.player.uniqueId)
    }

    @Subscribe
    fun onServerPreConnect(event: ServerPreConnectEvent)
    {
        val isStaffMember = event.player.hasPermission("cocoa.staff")

        if (!event.player.currentServer.isPresent)
        {
            event.result = ServerPreConnectEvent
                .ServerResult.allowed(
                    if (isStaffMember)
                        plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()
                )
        } else
        {
            val lemonPlayer = DataStoreHandler
                .findPlayerNullable(event.player.uniqueId)
                ?: return

            for (category in PunishmentCategory.WEIGHTED_DENIED)
            {
                val punishment = lemonPlayer
                    .punishments[category]
                    ?: continue

                val serverName = event
                    .originalServer
                    .serverInfo.name

                val extension = if (category == PunishmentCategory.IP_RELATIVE)
                {
                    "in relation to a ${
                        punishment.category.previous
                    }"
                } else
                {
                    punishment.category.previous
                }

                event.player.sendMessage(
                    LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(
                            String.format(
                                CocoaVelocityConstants.DENIED_SERVER_ENTRY, serverName, extension
                            )
                        )
                )

                event.result = ServerPreConnectEvent
                    .ServerResult.denied()
            }
        }
    }

    @Subscribe
    fun onPermissionSetup(
        event: PermissionsSetupEvent,
        continuation: Continuation
    )
    {
        val subject = event.subject

        if (subject !is Player)
        {
            continuation.resume()
            return
        }

        ForkJoinPool.commonPool().execute {
            CocoaPermissionEngine
                .calculatePermissions(
                    subject.uniqueId
                )

            event.provider =
                CocoaPermissionProvider

            continuation.resume()
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

    @Subscribe
    fun onKick(event: KickedFromServerEvent)
    {
        val newServer = if (event.player.hasPermission("cocoa.staff"))
            plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

        event.result = KickedFromServerEvent
            .RedirectPlayer.create(newServer)
    }

    private fun color(uuid: UUID): String
    {
        return DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uuid] ?: DataStoreHandler.getDefaultRank().uuid
        )?.color ?: ""
    }
}
