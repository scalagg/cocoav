package gg.scala.cocoa.listener

import co.schemati.trevor.api.TrevorAPI
import co.schemati.trevor.api.TrevorService
import co.schemati.trevor.velocity.TrevorVelocity
import com.google.common.cache.CacheBuilder
import com.velocitypowered.api.event.Continuation
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.permission.PermissionsSetupEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import gg.scala.aware.message.AwareMessage
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.scala.cocoa.CocoaVelocityConstants
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.legacy
import gg.scala.cocoa.lemon.LemonPlayer
import gg.scala.cocoa.lemon.TwoFactorModel
import gg.scala.cocoa.lemon.category.PunishmentCategory
import gg.scala.cocoa.network.CocoaNetworkAddressHandler
import gg.scala.cocoa.permission.CocoaPermissionEngine
import gg.scala.cocoa.permission.CocoaPermissionEngine.hasCocoaPermission
import gg.scala.cocoa.permission.CocoaPermissionProvider
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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

    private val connectedThroughMiddleware = CacheBuilder
        .newBuilder().build<UUID, Boolean>()

    @Subscribe
    fun onCommand(event: CommandExecuteEvent)
    {
        val label = event.command
            .split(" ").first()
            .toLowerCase()

        if (event.commandSource is Player && !label.contains("mfa"))
        {
            if (!this.allowAction(event.commandSource as Player, "performing commands"))
            {
                event.result = CommandExecuteEvent.CommandResult.denied()
                return
            }
        }
    }

    @Subscribe
    fun onChat(event: PlayerChatEvent)
    {
        if (!this.allowAction(event.player, "sending chat messages"))
        {
            event.result = PlayerChatEvent.ChatResult.allowed()
        }
    }

    @Subscribe
    fun onLogout(event: DisconnectEvent)
    {
        this.connectedThroughMiddleware
            .invalidate(event.player.uniqueId)

        val twoFactor = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()

        val model = twoFactor
            .localCache()[event.player.uniqueId]

        if (model != null)
        {
            if (model.persistIpAddress)
            {
                model.previousIpAddress = event.player
                    .remoteAddress.address.hostAddress
            }

            twoFactor.save(model, DataStoreStorageType.REDIS)
        }

        if (event.player.currentServer.isPresent)
        {
            produce(
                "network-disconnect",
                "uniqueId" to event.player.uniqueId
            ).apply {
                publish(channel = "cougar:backbone")
                publish(channel = "party:backbone")
            }

            produce(
                "friend-network-update",
                "uniqueId" to event.player.uniqueId,
                "type" to "left",
            ).apply {
                publish(channel = "friends")
            }
        }
    }

    @JvmStatic
    fun produce(
        packet: String,
        vararg arguments: Pair<String, Any>
    ): AwareMessage
    {
        val message = AwareMessage
            .of(packet, CocoaVelocityPlugin.INSTANCE.proxyChannel)

        for (argument in arguments)
        {
            message.assign(
                argument.first, argument.second
            )
        }

        return message
    }

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
            kotlin.runCatching scope@{
                val loadedRank = DataStoreHandler
                    .loadRankOfNow(event.player.uniqueId)

                DataStoreHandler.rankOf[uniqueId] = loadedRank.uuid

                if (plugin.maintenance)
                {
                    if (!plugin.shouldAllowOrStay(uniqueId))
                    {
                        val notification = CocoaSettingsProcessor
                            .locate<CocoaServerPresenceSettings>()
                            ?.whitelistedNotification
                            ?: "§cWe're currently whitelisted."

                        event.result = ResultedEvent.ComponentResult.denied(
                            LegacyComponentSerializer.legacySection().deserialize(notification)
                        )

                        DataStoreHandler.rankOf.remove(uniqueId)
                        continuation.resume()
                        return@execute
                    }
                }

                val settings = CocoaSettingsProcessor
                    .locate<CocoaVelocitySettings>()!!

                if (
                    settings.networking.handleNetworkingChecks &&
                    !event.player.hasCocoaPermission("cocoa.vpn.bypass")
                )
                {
                    kotlin.runCatching {
                        val networkModel = CocoaNetworkAddressHandler
                            .requestHostData(
                                event.player.remoteAddress
                                    .address.hostAddress
                            )
                            .join()

                        if (networkModel == null)
                        {
                            event.result = CocoaVelocityConstants
                                .DENIED_NETWORKING_ERROR
                            return@runCatching
                        }

                        if (networkModel.shouldBlock)
                        {
                            event.result = CocoaVelocityConstants
                                .DENIED_NETWORKING_INVALID

                            CocoaRedisSubscriber.sendStaffNotification(
                                null, "§3[global] §4${
                                    event.player.username
                                }§c tried logging on with a VPN.",
                                "lemon.vpn.notifications",
                                "vpn-alerts-disabled"
                            )
                        }
                    }.onFailure {
                        it.printStackTrace()

                        event.result = ResultedEvent.ComponentResult
                            .denied(
                                Component.text("Something went wrong")
                            )
                    }
                }

                val presenceSettings = CocoaSettingsProcessor
                    .locate<CocoaServerPresenceSettings>()

                if (
                    presenceSettings != null &&
                    !event.player.hasCocoaPermission("cocoa.playercap.bypass") &&
                    TrevorService.getAPI().instanceData.playerCount >= presenceSettings.playerCap
                )
                {
                    event.result = ResultedEvent.ComponentResult
                        .denied(
                            LegacyComponentSerializer
                                .legacyAmpersand()
                                .deserialize(
                                    """
                                        &cSorry, the server is full.
                                        &cPlease try joining again later!
                                    """.trimIndent()
                                )
                        )
                }

                if (!event.result.isAllowed)
                {
                    DataStoreHandler
                        .rankOf.remove(uniqueId)

                    continuation.resume()
                    return@execute
                }

                ScalaStoreUuidCache.useTranslator {
                    update(event.player.uniqueId to  event.player.username)
                }

                val lemonPlayer = DataStoreObjectControllerCache
                    .findNotNull<LemonPlayer>()
                    .loadOptimalCopy(uniqueId) {
                        LemonPlayer(
                            uniqueId,
                            event.player.username
                        )
                    }
                    .join()

                lemonPlayer.recalculatePunishments().join()
                lemonPlayer.checkForIpRelative().join()

                CocoaPermissionEngine
                    .calculatePermissions(
                        event.player.uniqueId
                    )

                if (event.player.hasCocoaPermission("cocoa.2fa"))
                {
                    var model = DataStoreObjectControllerCache
                        .findNotNull<TwoFactorModel>()
                        .load(event.player.uniqueId, DataStoreStorageType.REDIS)
                        .join()

                    if (model == null)
                    {
                        model = TwoFactorModel(
                            event.player.uniqueId,
                            event.player.remoteAddress.address.hostAddress
                        )
                    }

                    DataStoreObjectControllerCache
                        .findNotNull<TwoFactorModel>()
                        .localCache()[event.player.uniqueId] = model

                    if (model.persistIpAddress)
                    {
                        model.currentIpAddress = event.player
                            .remoteAddress.address.hostAddress
                    }
                }

                produce(
                    "friend-network-update",
                    "uniqueId" to event.player.uniqueId,
                    "type" to "joined",
                ).apply {
                    publish(channel = "friends")
                }
            }.onFailure {
                event.result = ResultedEvent.ComponentResult.denied(
                    """
                        &cAn internal server error occurred.
                        &cContact an administrator immediately.
                    """.trimIndent().legacy()
                )
            }

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
                "§b${color(event.player.uniqueId) + event.player.username}§3 has disconnected from §b$serverName§3."
            )
        }

        DataStoreHandler.rankOf
            .remove(event.player.uniqueId)

        DataStoreObjectControllerCache
            .findNotNull<LemonPlayer>()
            .delete(
                event.player.uniqueId,
                DataStoreStorageType.CACHE
            )
    }

    @Subscribe
    fun onChooseInitialServer(event: PlayerChooseInitialServerEvent)
    {
        val isStaffMember = event.player.hasPermission("cocoa.staff")

        val settings = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!

        if (
            settings.middleware != null &&
            !this.connectedThroughMiddleware
                .asMap().contains(event.player.uniqueId)
        )
        {
            this.connectedThroughMiddleware
                .put(event.player.uniqueId, true)

            event.setInitialServer(
                plugin.server.getServer(settings.middleware).get()
            )
            return
        }

        val server = if (isStaffMember)
            plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

        if (server != null)
        {
            event.setInitialServer(server)
            return
        }

        event.player.disconnect(
            "&cSorry! We couldn't find any lobby servers for you to join.".legacy()
        )
    }

    @Subscribe
    fun onServerPreConnect(event: ServerPreConnectEvent)
    {
        if (event.player.currentServer.isPresent)
        {
            val lemonPlayer = DataStoreHandler
                .findPlayerNullable(event.player.uniqueId)
                ?: return

            val server = event.player.currentServer.get()

            if (
                plugin.isLobbyServer(server.serverInfo) &&
                plugin.isLobbyServer(event.originalServer.serverInfo)
            )
            {
                return
            }

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
                    } player"
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
                return
            }

            if (!this.allowAction(event.player, "joining a server"))
            {
                event.result = ServerPreConnectEvent
                    .ServerResult.denied()
            }
        }
    }

    @Subscribe
    fun onPermissionSetup(event: PermissionsSetupEvent)
    {
        val subject = event.subject

        if (subject is Player)
        {
            event.provider = CocoaPermissionProvider
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
                    this.ensureAuthenticated(event.player)

                    CocoaRedisSubscriber.sendStaffNotification(
                        event.player,
                        "§b${color(event.player.uniqueId) + event.player.username}§3 has connected to §b$serverName§3."
                    )
                }.delay(HUNDRED_MILLI).schedule()
            }
        }
    }

    fun allowAction(player: Player, action: String = "performing this action"): Boolean
    {
        val model = DataStoreObjectControllerCache
            .findNotNull<TwoFactorModel>()
            .localCache()[player.uniqueId]
            ?: return true

        if (!player.hasPermission("cocoa.2fa"))
        {
            return true
        }

        if (model.exempt)
        {
            return true
        }

        if (!model.sessionAuthenticated)
        {
            player.sendMessage("&cYou must authenticate through &l${
                if (model.secret == null) "/mfa configure" else "/mfa authenticate"
            }&c before $action.".legacy())
            return false
        }

        return true
    }

    fun ensureAuthenticated(player: Player)
    {
        if (player.hasCocoaPermission("cocoa.2fa"))
        {
            val model = DataStoreObjectControllerCache
                .findNotNull<TwoFactorModel>()
                .localCache()[player.uniqueId]
                ?: return

            if (model.exempt)
            {
                model.sessionAuthenticated = true

                player.sendMessage("&b[MFA] &aYou've been exempt from two-factor-authentication.".legacy())
                return
            }

            if (model.secret == null)
            {
                player.sendMessage("&b[MFA] &aThe server requires you to configure two-factor-authentication on your account. &7(Use /mfa configure)".legacy())
                return
            }

            if (model.currentIpAddress != model.previousIpAddress)
            {
                model.persistIpAddress = false
                model.sessionAuthenticated = false
                model.save().join()

                player.sendMessage("&b[MFA] &cThe server requires you to authenticate. &7(Use /mfa authenticate)".legacy())
                return
            }

            model.sessionAuthenticated = true

            player.sendMessage("&b[MFA] &a&lYour account has been authenticated!".legacy())
        }
    }

    @Subscribe
    fun onKick(event: KickedFromServerEvent)
    {
        if ("pack" in event.server.serverInfo.name)
        {
            event.result = KickedFromServerEvent
                .DisconnectPlayer.create(
                    event.serverKickReason.orElse(
                        Component
                            .text("We were unable to apply our pack to your client.")
                            .color(NamedTextColor.RED)
                    )
                )
            return
        }

        val newServer = if (event.player.hasPermission("cocoa.staff"))
            plugin.fetchBestRestrictedLobby() else plugin.fetchBestLobby()

        if (
            newServer != null &&
            (event.kickedDuringServerConnect() || event.server != newServer)
        )
        {
            if (event.serverKickReason.isPresent)
            {
                val thing = LegacyComponentSerializer.legacySection()
                    .serialize(
                        event.serverKickReason.get()!!
                    )
                    .toLowerCase()

                if (
                    thing.contains("ban") ||
                    thing.contains("kick") ||
                    thing.contains("blacklist")
                )
                {
                    event.result = KickedFromServerEvent
                        .DisconnectPlayer.create(
                            event.serverKickReason.get()!!
                        )
                    return
                }
            }

            if (event.kickedDuringServerConnect() )
            {
                event.result = KickedFromServerEvent
                    .Notify.create(
                        event.serverKickReason.orElse(
                            Component
                                .text("You are not allowed to connected to this server.")
                                .color(NamedTextColor.RED)
                        )
                    )
                return
            }

            event.result = KickedFromServerEvent
                .RedirectPlayer.create(newServer)
        } else
        {
            event.result = KickedFromServerEvent
                .DisconnectPlayer.create(
                    event.serverKickReason.orElse(
                        Component
                            .text("Oops! There are no lobby servers for you to join.")
                            .color(NamedTextColor.RED)
                    )
                )
        }
    }

    private fun color(uuid: UUID): String
    {
        return DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uuid] ?: DataStoreHandler.getDefaultRank().uuid
        )?.color ?: ""
    }
}
