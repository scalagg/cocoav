package gg.scala.cocoa.listener

import co.schemati.trevor.api.TrevorService
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
import gg.scala.aware.thread.AwareThreadContext
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
import net.kyori.adventure.text.minimessage.MiniMessage
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

    private val connection = CocoaVelocityPlugin.INSTANCE
        .proxyChannel.internal().connect()

    @Subscribe
    fun onCommand(event: CommandExecuteEvent)
    {
        val label = event.command
            .split(" ").first()
            .lowercase(Locale.getDefault())

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
                            ?: "<red>We're currently whitelisted."

                        event.result = ResultedEvent.ComponentResult.denied(
                            MiniMessage.miniMessage().deserialize(notification)
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
                                    color(event.player.uniqueId) + event.player.username
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
                                        &cThe server is currently at max capacity!
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
                    update(event.player.uniqueId to event.player.username)
                }

                val lemonPlayer = DataStoreObjectControllerCache
                    .findNotNull<LemonPlayer>()
                    .loadOptimalCopy(uniqueId) {
                        LemonPlayer(
                            uniqueId,
                            event.player.username,
                            event.player.remoteAddress.address.hostAddress
                        )
                    }
                    .join()

                kotlin.runCatching {
                    lemonPlayer.recalculatePunishments().join()
                    lemonPlayer.checkForIpRelative().join()
                }.onFailure {
                    it.printStackTrace()
                }

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
                it.printStackTrace()
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
                "§b${color(event.player.uniqueId) + event.player.username}§3 disconnected from §b$serverName§3."
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

        val profile = connection.sync()
            .hget(
                "disguise:disguised-players",
                event.player.uniqueId.toString()
            )

        if (profile != null)
        {
            connection.sync()
                .hdel(
                    "disguise:in-use",
                    profile
                )

            connection.sync()
                .hdel(
                    "disguise:disguised-players",
                    event.player.uniqueId.toString()
                )
        }

        if (event.loginStatus == DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN)
        {
            produce(
                "network-disconnect",
                "uniqueId" to event.player.uniqueId
            ).apply {
                publish(
                    context = AwareThreadContext.SYNC,
                    channel = "party:backbone"
                )
            }

            produce(
                "leave",
                "player" to event.player.uniqueId
            ).apply {
                publish(
                    context = AwareThreadContext.SYNC,
                    channel = "queue:backbone"
                )
            }

            produce(
                "friend-network-update",
                "uniqueId" to event.player.uniqueId,
                "type" to "left",
            ).apply {
                publish(
                    context = AwareThreadContext.SYNC,
                    channel = "friends"
                )
            }
        }
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
            plugin.fetchBestRestrictedLobby(null) else plugin.fetchBestLobby(null)

        if (server != null)
        {
            event.setInitialServer(server)
            return
        }

        event.player.disconnect(
            "&cWhoops! We don't have any lobby servers for you to join!".legacy()
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
                    LegacyComponentSerializer
                        .legacyAmpersand()
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
                        "§b${color(event.player.uniqueId) + event.player.username}§3 connected to §b$serverName§3."
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
            player.sendMessage(
                "&cYou must authenticate through &l${
                    if (model.secret == null) "/mfa configure" else "/mfa authenticate"
                }&c before $action.".legacy()
            )
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

                player.sendMessage("&7You've been authenticated as you logged in from a 2FA exempt account.".legacy())
                return
            }

            if (model.secret == null)
            {
                player.sendMessage("&aThe server requires you to configure two-factor-authentication on your account. &f(Use /mfa configure)".legacy())
                return
            }

            if (model.currentIpAddress != model.previousIpAddress)
            {
                model.persistIpAddress = false
                model.sessionAuthenticated = false
                model.save().join()

                player.sendMessage("&cThe server requires you to authenticate yourself. &f(Use /mfa authenticate)".legacy())
                return
            }

            model.sessionAuthenticated = true

            player.sendMessage("&7You've been authenticated as you logged in from the same address.".legacy())
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
            plugin.fetchBestRestrictedLobby(event.server.serverInfo) else plugin.fetchBestLobby(event.server.serverInfo)

        if (
            newServer != null &&
            (event.kickedDuringServerConnect() || event.server != newServer)
        )
        {
            if (event.serverKickReason.isPresent)
            {
                val thing = LegacyComponentSerializer.legacySection()
                    .serialize(
                        event.serverKickReason.get()
                    )
                    .lowercase(Locale.getDefault())

                if (
                    thing.contains("ban") ||
                    thing.contains("kick") ||
                    thing.contains("blacklist")
                )
                {
                    event.result = KickedFromServerEvent
                        .DisconnectPlayer.create(
                            event.serverKickReason.get()
                        )
                    return
                }
            }

            if (event.kickedDuringServerConnect())
            {
                event.result = KickedFromServerEvent
                    .Notify.create(
                        event.serverKickReason.orElse(
                            Component
                                .text("Sorry! We were unable to redirect you to: ${event.server.serverInfo.name}")
                                .color(NamedTextColor.RED)
                                .append(
                                    Component.newline()
                                )
                                .append(
                                    Component.text("Please try logging in again, or contact an administrator for help.")
                                        .color(NamedTextColor.RED)
                                )
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
