package gg.scala.cocoa.listener

import co.schemati.trevor.api.TrevorService
import com.google.common.cache.CacheBuilder
import com.mongodb.client.model.Filters
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.permission.PermissionsSetupEvent
import com.velocitypowered.api.event.player.*
import com.velocitypowered.api.proxy.Player
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.scala.cocoa.*
import gg.scala.cocoa.countdown.CocoaCountdownPresence
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.lemon.LemonPlayer
import gg.scala.cocoa.lemon.TwoFactorModel
import gg.scala.cocoa.lemon.category.PunishmentCategory
import gg.scala.cocoa.limbo.LimboHandler
import gg.scala.cocoa.limbo.NoOpLimboAccess
import gg.scala.cocoa.network.CocoaNetworkAddressHandler
import gg.scala.cocoa.network.NetworkCheckException
import gg.scala.cocoa.network.vpnDetectorAPICircuitBreaker
import gg.scala.cocoa.permission.CocoaPermissionEngine
import gg.scala.cocoa.permission.CocoaPermissionEngine.hasCocoaPermission
import gg.scala.cocoa.permission.CocoaPermissionProvider
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.impl.MongoDataStoreStorageLayer
import gg.scala.store.storage.type.DataStoreStorageType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bson.conversions.Bson
import java.time.Duration
import java.util.*

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
        if (!allowAction(event.player, "sending chat messages"))
        {
            event.result = PlayerChatEvent.ChatResult.denied()
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

    fun findSingleWithFilter(or: Bson) =
        (DataStoreObjectControllerCache
            .findNotNull<Guild>()
            .localLayerCache[DataStoreStorageType.MONGO]!! as MongoDataStoreStorageLayer<Guild>)
            .loadWithFilter(or)

    fun guildByUser(user: UUID) =
        findSingleWithFilter(
            Filters.or(
                Filters.eq("creator.uniqueId", user.toString()),
                Filters.exists("members.$user")
            )
        )

    @Subscribe(order = PostOrder.FIRST)
    fun onLogin(event: LoginEvent)
    {
        val uniqueId = event.player.uniqueId
        val settings = CocoaVelocitySettingsDataSync.cached()

        if (settings.shouldRestrictAllowedVersions && settings.allowedVersions != null)
        {
            if (event.player.protocolVersion.protocol !in settings.allowedVersions!!)
            {
                event.result = ResultedEvent.ComponentResult.denied(
                    Component.text("You cannot join the server on this version!")
                        .color(NamedTextColor.RED)
                )
                return
            }
        }

        kotlin.runCatching scope@{
            val loadedRank = DataStoreHandler
                .loadRankOfNow(event.player.uniqueId)

            DataStoreHandler.rankOf[uniqueId] = loadedRank.uuid
            if (settings.maintenance.enabled)
            {
                if (!plugin.shouldAllowOrStay(uniqueId))
                {
                    val stagingFooter = CocoaCountdownPresence.getStagingFooter()
                    var notification = MiniMessage.miniMessage()
                        .deserialize(
                            settings.presence.whitelistedNotification
                        )

                    if (stagingFooter != null)
                    {
                        notification = notification
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(stagingFooter)
                    }

                    DataStoreHandler.rankOf.remove(uniqueId)
                    event.result = ResultedEvent.ComponentResult.denied(notification)
                    return@scope
                }
            }

            if (
                settings.networking.handleNetworkingChecks &&
                !event.player.hasCocoaPermission("cocoa.vpn.bypass")
            )
            {
                val networkCheckSupplier = CircuitBreaker.decorateSupplier(vpnDetectorAPICircuitBreaker) {
                    CocoaNetworkAddressHandler
                        .requestHostData(event.player.remoteAddress.address.hostAddress)
                        .join()
                        ?: throw NetworkCheckException("Network model is null")
                }

                kotlin.runCatching {
                    val result = networkCheckSupplier.get()
                    if (result.shouldBlock)
                    {
                        CocoaRedisSubscriber.sendStaffNotification(
                            null,
                            "§3[global] §4${rankFormat(event.player.uniqueId) + event.player.username}§c tried logging on with a VPN.",
                            "lemon.vpn.notifications",
                            "vpn-alerts-disabled"
                        )
                        event.result = CocoaVelocityConstants.DENIED_NETWORKING_INVALID
                        return@runCatching
                    }
                }.onFailure { throwable ->
                    if (throwable is NetworkCheckException)
                    {
                        throwable.printStackTrace()
                        event.result = CocoaVelocityConstants.DENIED_NETWORKING_ERROR
                    }
                }
            }

            if (
                !event.player.hasCocoaPermission("cocoa.playercap.bypass") &&
                TrevorService.getAPI().instanceData.playerCount >= settings.presence.playerCap
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
                return@scope
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
                    .load(event.player.uniqueId, DataStoreStorageType.MONGO)
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

            guildByUser(event.player.uniqueId)
                .thenAccept {
                    it?.sendMessage(
                        "§a${event.player.username}§e joined."
                    )
                }
                .join()
        }.onFailure {
            it.printStackTrace()
            event.result = ResultedEvent.ComponentResult.denied(
                """
                        &cAn internal server error occurred.
                        &cContact an administrator immediately.
                    """.trimIndent().legacy()
            )
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

        val proxyName = TrevorService.getAPI().platform.instanceConfiguration.id

        if (isStaffMember && serverName != null)
        {
            CocoaRedisSubscriber.sendStaffNotification(
                event.player,
                "§b${rankFormat(event.player.uniqueId) + event.player.username} §cdisconnected §bfrom the network §7($proxyName)"
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

            twoFactor.save(model, DataStoreStorageType.MONGO)
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
                "network-logout",
                "player" to event.player.uniqueId
            ).apply {
                publish(
                    context = AwareThreadContext.SYNC,
                    channel = "queue:backbone"
                )
            }

            guildByUser(event.player.uniqueId)
                .thenAccept {
                    it?.sendMessage(
                        "§c${event.player.username}§e left."
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
        val settings = CocoaVelocitySettingsDataSync.cached()

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
            plugin.fetchBestRestrictedLobby(event.player, null) else plugin.fetchBestLobby(event.player, null)

        if (server != null)
        {
            event.setInitialServer(server)
        }
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
            val proxyName = TrevorService.getAPI().platform.instanceConfiguration.id

            if (previousServer != null)
            {
                if (
                    !(previousServer.serverInfo.name.contains("practice") &&
                        serverName.contains("practice"))
                )
                {
                    CocoaRedisSubscriber.sendStaffNotification(
                        event.player,
                        "§b${rankFormat(event.player.uniqueId) + event.player.username}§e switched §bto $serverName§7 (from ${previousServer.serverInfo.name})"
                    )
                }
            } else
            {
                plugin.server.scheduler.buildTask(plugin) {
                    this.ensureAuthenticated(event.player)

                    produce(
                        "network-login",
                        "player" to event.player.uniqueId
                    ).apply {
                        publish(
                            context = AwareThreadContext.SYNC,
                            channel = "queue:backbone"
                        )
                    }

                    CocoaRedisSubscriber.sendStaffNotification(
                        event.player,
                        "§b${rankFormat(event.player.uniqueId) + event.player.username}§a joined §bthe network §7($proxyName)"
                    )
                }.delay(HUNDRED_MILLI).schedule()
            }
        }

        plugin.server.scheduler.buildTask(plugin) {
            produce(
                "network-login",
                "player" to event.player.uniqueId
            ).apply {
                publish(
                    context = AwareThreadContext.SYNC,
                    channel = "queue:backbone"
                )
            }
        }.delay(HUNDRED_MILLI).schedule()
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
            plugin.fetchBestRestrictedLobby(
                event.player,
                event.server.serverInfo
            ) else plugin.fetchBestLobby(event.player, event.server.serverInfo)

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
            return
        }

        if (plugin.limboAccess !is NoOpLimboAccess)
        {
            plugin.limboAccess.spawn(event.player)
        } else
        {

            event.result = KickedFromServerEvent
                .DisconnectPlayer.create(Component.text("Sorry, we could not find a server to connect you to!").color(
                    NamedTextColor.RED))
        }
        return
    }

    private fun rankFormat(uuid: UUID, includePrefix: Boolean = true): String {
        val rankUuid = DataStoreHandler.rankOf[uuid] ?: DataStoreHandler.getDefaultRank().uuid
        val rank = DataStoreHandler.findRank(rankUuid) ?: return ""

        val prefixPart = if (includePrefix && !rank.prefix.isNullOrBlank()) {
            "${rank.prefix}${rank.color} "
        } else {
            ""
        }

        return "$prefixPart${rank.color}"
    }
}
