package gg.scala.cocoa

import co.schemati.trevor.api.TrevorService
import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
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
import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownPresence
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.discovery.CocoaServerSyncDiscovery
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.handler.LemonRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.listener.PlayerListener
import gg.scala.cocoa.presence.CocoaServerPresenceHandler
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.presence.CocoaServerPresenceSubscriber
import gg.scala.cocoa.presence.override.impl.WhitelistedPresenceOverride
import gg.scala.cocoa.settings.CocoaSettingsModel
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.MessageKeys
import gg.scala.commons.acf.MessageType
import gg.scala.commons.acf.VelocityCommandManager
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.store.controller.DataStoreObjectController
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.serializer.serializers.GsonSerializer
import gg.scala.store.storage.impl.RedisDataStoreStorageLayer
import gg.scala.store.storage.type.DataStoreStorageType
import net.evilblock.cubed.serializers.Serializers
import net.kyori.adventure.text.format.NamedTextColor
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
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

    val directoryFile: File = directory.toFile().also {
        if (!it.exists())
        {
            it.mkdirs()
        }
    }

    var proxyChannel by Delegates.notNull<Aware<AwareMessage>>()
    var spigotChannel by Delegates.notNull<Aware<AwareMessage>>()

    var settingsLayer by Delegates.notNull<DataStoreObjectController<CocoaVelocitySettingsEntry>>()
    var cloudLayer by Delegates.notNull<DataStoreObjectController<CocoaSettingsModel>>()

    private val proxySettings = mutableMapOf<String, Boolean>()

    private var maintenanceRank: String? = null

    val maintenance: Boolean
        get() = proxySettings.getOrDefault("maintenance", false)

    init
    {
        INSTANCE = this
    }

    private fun configureVelocityPlugin()
    {
        CocoaSettingsProcessor.initialLoad()

        CocoaSettingsProcessor.loadSettings<CocoaVelocitySettings>("settings")
        CocoaSettingsProcessor.loadSettings<CocoaServerPresenceSettings>("presence")
        CocoaSettingsProcessor.loadSettings<CocoaCountdownSettings>("countdown")

        val executor = Executors
            .newScheduledThreadPool(1)

        DataStoreHandler.configure()

        CocoaServerPresenceHandler.initialLoad()
        CocoaServerPresenceHandler.trackPresence(WhitelistedPresenceOverride)
        CocoaServerPresenceHandler.trackPresence(CocoaCountdownPresence)

        CocoaCountdown.initialLoad()

        startRedisFunctionality()

        if (
            CocoaSettingsProcessor
                .locate<CocoaVelocitySettings>()!!
                .consul.enabled
        )
        {
            ServerSync.configureIndependent()

            executor.scheduleAtFixedRate(
                CocoaServerSyncDiscovery,
                0L, 500L,
                TimeUnit.MILLISECONDS
            )
        }
    }

    private val commandManager by lazy {
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

            for (message in MessageKeys.values())
            {
                val locale = Locale("en")

                commandManager.locales.addMessage(
                    locale, message,
                    properties
                        .getProperty("acf-core.${message.name.toLowerCase()}")
                        .replace("&", "§")
                )
            }
        }

        listOf<MessageType>(
            MessageType.HELP, MessageType.INFO, MessageType.SYNTAX
        ).forEach {
            commandManager.getFormat(it).setColor(3, NamedTextColor.GRAY)
            commandManager.getFormat(it).setColor(2, NamedTextColor.YELLOW)
            commandManager.getFormat(it).setColor(1, NamedTextColor.GOLD)
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
        val settings = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!

        AwareHub.configure(settings.credentials) {
            Serializers.gson
        }

        GsonSerializer.provideCustomGson {
            Serializers.gson
        }

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

        settingsLayer = DataStoreObjectControllerCache.create()
        cloudLayer = DataStoreObjectControllerCache.create()

        DataStoreObjectControllerCache.create<Guild>()

        val executor = Executors
            .newScheduledThreadPool(1)

        Runtime.getRuntime().addShutdownHook(Thread {
            executor.shutdown()

            proxyChannel.shutdown()
            spigotChannel.shutdown()
        })

        executor.scheduleAtFixedRate({
            settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
            {
                this.connection.useResource {
                    kotlin.runCatching {
                        sync().set(
                            "global-player-count${
                                if (settings.externalProxy) settings.externalProxyPrefix else ""
                            }", TrevorService.getAPI().instanceData.playerCount.toString()
                        )
                    }
                }
            }
        }, 0L, 500L, TimeUnit.MILLISECONDS)

        executor.scheduleAtFixedRate({
            server.allPlayers.forEach {
                DataStoreHandler.updateRank(it.uniqueId)
            }
        }, 0L, 2L, TimeUnit.SECONDS)

        executor.scheduleAtFixedRate({
            settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
            {
                this.connection.useResource {
                    for (entry in sync().hgetall(
                        "cocoa:metadata${
                            if (settings.externalProxy) settings.externalProxyPrefix else ""
                        }"
                    ).entries)
                    {
                        proxySettings[entry.key] =
                            entry.value.toBoolean()
                    }

                    for (entry in sync().hgetall(
                        "cocoa:unique-ids${
                            if (settings.externalProxy) settings.externalProxyPrefix else ""
                        }"
                    ).entries)
                    {
                        if (
                            maintenanceRank != null &&
                            maintenanceRank != entry.value
                        )
                        {
                            CocoaRedisSubscriber.buildMessage(
                                "maintenance-kick",
                                mapOf()
                            ).publish(
                                channel = "cocoa"
                            )

                            maintenanceRank = entry.value
                            return@useResource
                        }

                        maintenanceRank = entry.value
                    }
                }
            }
        }, 0L, 500L, TimeUnit.MILLISECONDS)
    }

    fun fetchBestLobby(filterOut: ServerInfo? = null): RegisteredServer?
    {
        return server.allServers
            .filter {
                (filterOut == null || it.serverInfo.name != filterOut.name) && isLobbyServer(it.serverInfo)
            }
            .minByOrNull { it.playersConnected.size }
    }

    fun fetchBestRestrictedLobby(filterOut: ServerInfo? = null): RegisteredServer?
    {
        return server.allServers
            .filter {
                (filterOut == null || it.serverInfo.name != filterOut.name) && isRestrictedLobbyServer(it.serverInfo)
            }
            .minByOrNull { it.playersConnected.size }
            ?: fetchBestLobby(filterOut)
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
        val settings = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!

        settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
        {
            this.connection.useResource {
                sync().hset(
                    "cocoa:unique-ids${
                        if (settings.externalProxy) settings.externalProxyPrefix else ""
                    }", "maintenance-rank", lemonSpigotRank.uuid.toString()
                )
            }
        }
    }

    fun updateMaintenanceMode(lemonSpigotRank: Rank?)
    {
        val newValue = !maintenance
        proxySettings["maintenance"] = newValue

        val settings = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!

        if (lemonSpigotRank != null)
        {
            settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
            {
                this.connection.useResource {
                    sync().hset(
                        "cocoa:unique-ids${
                            if (settings.externalProxy) settings.externalProxyPrefix else ""
                        }", "maintenance-rank", lemonSpigotRank.uuid.toString()
                    )
                }
            }
        }

        settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
        {
            this.connection.useResource {
                sync().hset(
                    "cocoa:metadata${
                        if (settings.externalProxy) settings.externalProxyPrefix else ""
                    }", "maintenance", newValue.toString()
                )
            }
        }
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

        val settings = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!

        if (settings.ensureTokenWhitelisted && maintenanceRank.weight < settings.tokenWhitelistHighestWeightBeforeNotEffective)
        {
            if (
                settingsLayer.useLayerWithReturn<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>, Boolean>(DataStoreStorageType.REDIS)
                {
                    this.connection.useResourceWithReturn {
                        sync().hexists("whitelist-tokens:whitelisted", uuid.toString())
                    } == true
                }
            )
            {
                return true
            }
        }

        return false
    }

    fun maintenanceRank(): Rank
    {
        maintenanceRank ?: return DataStoreHandler.getDefaultRank()

        try
        {
            return DataStoreHandler.findRank(
                UUID.fromString(maintenanceRank)
            ) ?: return DataStoreHandler.getDefaultRank()
        } catch (ignored: Exception)
        {
            return DataStoreHandler.getDefaultRank()
        }
    }
}
