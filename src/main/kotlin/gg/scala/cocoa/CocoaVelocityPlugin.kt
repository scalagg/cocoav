package gg.scala.cocoa

import co.schemati.trevor.api.TrevorService
import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.aware.Aware
import gg.scala.aware.AwareBuilder
import gg.scala.aware.AwareHub
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.scala.cache.uuid.impl.distribution.DistributedRedisUuidCacheTranslator
import gg.scala.cache.uuid.resolver.ResolverType
import gg.scala.cocoa.command.LobbyCommand
import gg.scala.cocoa.command.MfaCommand
import gg.scala.cocoa.command.administration.CocoaCommand
import gg.scala.cocoa.command.administration.CountdownCommand
import gg.scala.cocoa.command.administration.TrafficCommand
import gg.scala.cocoa.discovery.CocoaServerSyncDiscovery
import gg.scala.cocoa.dps.DPSRedisShared
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.handler.LemonRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.lemon.validate.ScalaValidateData
import gg.scala.cocoa.lemon.validate.ScalaValidateUtil
import gg.scala.cocoa.listener.PlayerListener
import gg.scala.cocoa.network.vpnDetectorAPICircuitBreaker
import gg.scala.cocoa.presence.CocoaServerPresenceHandler
import gg.scala.cocoa.presence.CocoaServerPresenceSubscriber
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.MessageKeys
import gg.scala.commons.acf.MessageType
import gg.scala.commons.acf.VelocityCommandManager
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.serializer.serializers.GsonSerializer
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import net.evilblock.cubed.serializers.Serializers
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
@Plugin(
    id = "cocoa",
    name = "cocoa",
    version = "1.1.7",
    url = "https://scala.gg/",
    authors = ["Scala"],
    dependencies = [
        Dependency(id = "store-velocity")
    ]
)
class CocoaVelocityPlugin
@Inject
constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory
    private val directory: Path
)
{
    companion object
    {
        @JvmStatic
        var INSTANCE by Delegates.notNull<CocoaVelocityPlugin>()
    }

    private val directoryFile = directory.toFile()
        .also {
            if (!it.exists())
            {
                it.mkdirs()
            }
        }

    val instanceSettings = File(directoryFile, "instance.settings.json")
        .let {
            if (!it.exists())
            {
                it.createNewFile()
                it.writeText(
                    Serializers.gson.toJson(InstanceSpecificSettings())
                )
            }

            Serializers.gson.fromJson(
                it.readText(),
                InstanceSpecificSettings::class.java
            )
        }

    var proxyChannel by Delegates.notNull<Aware<AwareMessage>>()
    var spigotChannel by Delegates.notNull<Aware<AwareMessage>>()

    init
    {
        INSTANCE = this
    }

    var serverData: ScalaValidateData? = null

    private fun configureVelocityPlugin()
    {
        val executor = Executors.newSingleThreadScheduledExecutor()

        DataStoreHandler.configure()
        startRedisFunctionality()

        CocoaServerPresenceHandler.initialLoad()

        val cached = CocoaVelocitySettingsDataSync.cached()
        if (cached.autoRegisterFromServerSyncDiscovery)
        {
            ServerSync.configureIndependent()

            executor.scheduleAtFixedRate(
                CocoaServerSyncDiscovery,
                0L, 500L,
                TimeUnit.MILLISECONDS
            )
        }

        if (cached.networking.handleNetworkingChecks)
        {
            vpnDetectorAPICircuitBreaker.eventPublisher.onFailureRateExceeded {
                CocoaRedisSubscriber.sendStaffNotification(
                    null,
                    "§3[global] §c§l${it.failureRate}%§c of VPN detector requests have resulted in a failure. Switching to CircuitBreaker OPEN mode.",
                    "lemon.internal"
                )
            }

            vpnDetectorAPICircuitBreaker.eventPublisher.onReset {
                CocoaRedisSubscriber.sendStaffNotification(
                    null,
                    "§3[global] §aCircuitBreaker has been reset. Activity is now normal.",
                    "lemon.internal"
                )
            }

            vpnDetectorAPICircuitBreaker.eventPublisher.onStateTransition {
                if (it.stateTransition != CircuitBreaker.StateTransition.OPEN_TO_HALF_OPEN)
                {
                    return@onStateTransition
                }

                CocoaRedisSubscriber.sendStaffNotification(
                    null,
                    "§3[global] §6Attempting a reset of the CircuitBreaker...",
                    "lemon.internal"
                )
            }
        }
    }

    val commandManager by lazy {
        VelocityCommandManager(server, this)
    }

    @Subscribe(
        order = PostOrder.LAST
    )
    fun onInitialize(event: ProxyInitializeEvent)
    {
        configureVelocityPlugin()

        kotlin.runCatching {
            val properties = Properties()
            properties.load(
                File(this.directoryFile, "acf-core_en.properties").inputStream()
            )

            for (message in MessageKeys.entries)
            {
                val locale = Locale("en")

                commandManager.locales.addMessage(
                    locale, message,
                    properties
                        .getProperty("acf-core.${message.name.lowercase(Locale.getDefault())}")
                        .replace("&", "§")
                )
            }
        }

        logger.info("Loading instance data from the API...")
        serverData = ScalaValidateUtil
            .fetchServerData(
                instanceSettings.apiID,
                instanceSettings.apiUseHTTPS,
                instanceSettings.apiSupplier
            )
            ?: return run {
                logger.info("Failed to load instance data from the API!")
            }

        logger.info("Loaded onto server type ${serverData!!.serverName}!")
        listOf<MessageType>(MessageType.HELP, MessageType.INFO, MessageType.SYNTAX).forEach {
            commandManager.getFormat(it).setColor(3, NamedTextColor.GRAY)
            commandManager.getFormat(it).setColor(2, NamedTextColor.NAMES.value(serverData!!.secondary.lowercase()))
            commandManager.getFormat(it).setColor(1, NamedTextColor.NAMES.value(serverData!!.primary.lowercase()))
            commandManager.getFormat(it).setColor(4, NamedTextColor.WHITE)
        }

        listOf<MessageType>(MessageType.ERROR).forEach {
            commandManager.getFormat(it).setColor(2, NamedTextColor.YELLOW)
            commandManager.getFormat(it).setColor(1, NamedTextColor.RED)
        }

        commandManager.commandContexts.registerContext(Rank::class.java) {
            val firstArg = it.popFirstArg()

            return@registerContext DataStoreHandler.findRank(firstArg)
                ?: throw ConditionFailedException("§cThere is no rank with the name §e$firstArg§c.")
        }

        commandManager.commandCompletions.registerCompletion("ranks") {
            return@registerCompletion DataStoreHandler.ranks.values.map { it.name }
        }

        commandManager.commandCompletions.registerCompletion("servers") {
            return@registerCompletion server.allServers.map { it.serverInfo.name }
        }

        commandManager.commandCompletions.registerCompletion("players") {
            return@registerCompletion server.allPlayers.map { it.username }
        }

        commandManager.enableUnstableAPI("help")

        listOf(
            LobbyCommand, CocoaCommand, TrafficCommand, CountdownCommand, MfaCommand
        ).forEach {
            commandManager.registerCommand(it)
        }

        server.eventManager.register(this, PlayerListener)
        server.eventManager.register(this, CocoaServerPresenceSubscriber)
    }

    private fun startRedisFunctionality()
    {
        GsonSerializer.provideCustomGson {
            Serializers.gson
        }

        DPSRedisShared.keyValueCache
        CocoaVelocitySettingsDataSync.load()

        proxyChannel = AwareBuilder
            .of<AwareMessage>("cocoa")
            .logger(logger)
            .codec(AwareMessageCodec)
            .build()

        spigotChannel = AwareBuilder
            .of<AwareMessage>("lemon")
            .logger(logger)
            .codec(AwareMessageCodec)
            .build()

        proxyChannel.listen(CocoaRedisSubscriber)
        spigotChannel.listen(LemonRedisHandler)

        ScalaStoreUuidCache.configure(
            DistributedRedisUuidCacheTranslator(proxyChannel),
            ScalaStoreUuidCache.resolve(ResolverType.Mojang)
        )

        spigotChannel.connect().toCompletableFuture().join()

        DataStoreObjectControllerCache.create<Guild>()

        val executor = Executors.newSingleThreadScheduledExecutor()

        Runtime.getRuntime().addShutdownHook(Thread {
            executor.shutdown()
            AwareHub.close()
        })

        if (instanceSettings.masterProxy)
        {
            logger.info("Updating the global player count as the master proxy ID")
            executor.scheduleAtFixedRate({
                DPSRedisShared.keyValueCache.sync().set(
                    "global-player-count${
                        if (instanceSettings.externalProxy) instanceSettings.externalProxyPrefix else ""
                    }",
                    TrevorService.getAPI().instanceData.playerCount.toString()
                )
            }, 0L, 500L, TimeUnit.MILLISECONDS)
        }

        executor.scheduleAtFixedRate({
            server.allPlayers.forEach {
                DataStoreHandler.updateRank(it.uniqueId)
            }
        }, 0L, 2L, TimeUnit.SECONDS)
    }

    fun fetchBestLobby(player: Player, filterOut: ServerInfo? = null): RegisteredServer?
    {
        val redirect = CocoaVelocitySettingsDataSync.cached()
            .preferHubServerRedirects.entries.firstOrNull {
                player.currentServer.getOrNull()?.serverInfo?.name?.contains(it.key) == true
            }

        if (redirect != null)
        {
            val result = server.allServers.filter {
                it.serverInfo.name == redirect.value || it.serverInfo.name.contains(redirect.value)
            }.minByOrNull {
                it.playersConnected.size
            }

            if (result != null)
            {
                return result
            }
        }

        return server.allServers
            .filter {
                (filterOut == null || it.serverInfo.name != filterOut.name) && isLobbyServer(it.serverInfo)
            }
            .minByOrNull { it.playersConnected.size }
    }

    fun fetchBestRestrictedLobby(player: Player, filterOut: ServerInfo? = null): RegisteredServer?
    {
        val redirect = CocoaVelocitySettingsDataSync.cached()
            .preferHubServerRedirects.entries.firstOrNull {
                player.currentServer.getOrNull()?.serverInfo?.name?.contains(it.key) == true
            }

        if (redirect != null)
        {
            val result = server.allServers.filter {
                it.serverInfo.name == redirect.value || it.serverInfo.name.contains(redirect.value)
            }.minByOrNull {
                it.playersConnected.size
            }

            if (result != null)
            {
                return result
            }
        }

        return server.allServers
            .filter {
                (filterOut == null || it.serverInfo.name != filterOut.name) && isRestrictedLobbyServer(it.serverInfo)
            }
            .minByOrNull { it.playersConnected.size }
            ?: fetchBestLobby(player, filterOut)
    }

    fun isLobbyServer(info: ServerInfo): Boolean
    {
        return (info.name.startsWith("hub") || info.name.startsWith("lobby"))
    }

    private fun isRestrictedLobbyServer(info: ServerInfo): Boolean
    {
        return (info.name.startsWith("hub") || info.name.startsWith("lobby")) && info.name.endsWith("restricted")
    }

    fun updateMaintenanceRank(lemonSpigotRank: Rank)
    {
        val cached = CocoaVelocitySettingsDataSync.cached()
        cached.maintenance.rank = lemonSpigotRank.uuid

        CocoaVelocitySettingsDataSync.sync(cached)
    }

    fun updateMaintenanceMode(lemonSpigotRank: Rank?)
    {
        val cached = CocoaVelocitySettingsDataSync.cached()
        cached.maintenance.rank = lemonSpigotRank?.uuid
        cached.maintenance.enabled = !cached.maintenance.enabled

        CocoaVelocitySettingsDataSync.sync(cached)
    }

    fun shouldAllowOrStay(uuid: UUID): Boolean
    {
        val maintenanceRank = maintenanceRank()

        val rankOfPlayer = DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uuid]
        ) ?: DataStoreHandler.getDefaultRank()

        if (rankOfPlayer.weight >= maintenanceRank.weight)
        {
            return true
        }

        val settings = CocoaVelocitySettingsDataSync.cached()
        if (settings.ensureTokenWhitelisted && maintenanceRank.weight < settings.tokenWhitelistHighestWeightBeforeNotEffective)
        {
            return DPSRedisShared.keyValueCache.sync()
                .hexists("whitelist-tokens:whitelisted", uuid.toString())
        }

        return false
    }

    fun maintenanceRank() = kotlin.runCatching {
        DataStoreHandler
            .findRank(CocoaVelocitySettingsDataSync.cached().maintenance.rank)
    }.getOrElse {
        DataStoreHandler.getDefaultRank()
    } ?: DataStoreHandler.getDefaultRank()
}
