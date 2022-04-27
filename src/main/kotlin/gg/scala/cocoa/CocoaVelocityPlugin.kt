package gg.scala.cocoa

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
import gg.scala.cocoa.command.GlobalListCommand
import gg.scala.cocoa.command.LobbyCommand
import gg.scala.cocoa.command.MaintenanceCommand
import gg.scala.cocoa.command.administration.*
import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownPresence
import gg.scala.cocoa.countdown.CocoaCountdownSettings
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
import gg.scala.store.controller.DataStoreObjectController
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.impl.RedisDataStoreStorageLayer
import gg.scala.store.storage.type.DataStoreStorageType
import net.evilblock.cubed.serializers.Serializers
import net.kyori.adventure.text.format.NamedTextColor
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandHandler
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
    version = "1.1.0",
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

    private fun enable()
    {
        CocoaSettingsProcessor.initialLoad()

        CocoaSettingsProcessor.loadSettings<CocoaVelocitySettings>("settings")
        CocoaSettingsProcessor.loadSettings<CocoaServerPresenceSettings>("presence")
        CocoaSettingsProcessor.loadSettings<CocoaCountdownSettings>("countdown")

        DataStoreHandler.configure()

        CocoaServerPresenceHandler.initialLoad()
        CocoaServerPresenceHandler.trackPresence(WhitelistedPresenceOverride)
        CocoaServerPresenceHandler.trackPresence(CocoaCountdownPresence)

        CocoaCountdown.initialLoad()

        startRedisFunctionality()
    }

    @Subscribe(
        order = PostOrder.LAST
    )
    fun onInitialize(event: ProxyInitializeEvent)
    {
        val commandManager = VelocityCommandHandler
            .create(this, server)

        enable()

        commandManager.registerContextResolver(Rank::class.java) {
            val firstArg = it.input().toMutableList()[0]

            return@registerContextResolver DataStoreHandler.findRank(firstArg)
                ?: throw CommandErrorException("There is no rank with the name: ${NamedTextColor.YELLOW}$firstArg${NamedTextColor.RED}.")
        }

        commandManager.autoCompleter.registerParameterSuggestions(Rank::class.java) { _, _, _ ->
            return@registerParameterSuggestions DataStoreHandler.ranks.values.map { it.name }
        }

        commandManager.registerDependency(CocoaVelocityPlugin::class.java, this)

        commandManager.register(
            LobbyCommand, GlobalListCommand,
            MaintenanceCommand, XServerCommand,
            CocoaCommand, SendCommand, CountdownCommand,
            IpInfoCommand
        )

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

        proxyChannel.connect().toCompletableFuture().join()
        spigotChannel.connect().toCompletableFuture().join()

        settingsLayer = DataStoreObjectControllerCache.create()
        cloudLayer = DataStoreObjectControllerCache.create()

        val executor = Executors
            .newScheduledThreadPool(1)

        Runtime.getRuntime().addShutdownHook(Thread {
            executor.shutdown()

            proxyChannel.shutdown()
            spigotChannel.shutdown()
        })

        executor.scheduleAtFixedRate({
            server.allPlayers.forEach {
                DataStoreHandler.updateRank(it.uniqueId)
            }
        }, 0L, 2L, TimeUnit.SECONDS)

        executor.scheduleAtFixedRate({
            settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
            {
                this.connection.useResource {
                    for (entry in sync().hgetall("cocoa:metadata").entries)
                    {
                        proxySettings[entry.key] =
                            entry.value.toBoolean()
                    }

                    for (entry in sync().hgetall("cocoa:unique-ids").entries)
                    {
                        maintenanceRank = entry.value
                    }
                }
            }
        }, 0L, 500L, TimeUnit.MILLISECONDS)
    }

    fun fetchBestLobby(): RegisteredServer?
    {
        return server.allServers
            .sortedBy { it.playersConnected.size }
            .firstOrNull { isLobbyServer(it.serverInfo) }
    }

    fun fetchBestRestrictedLobby(): RegisteredServer?
    {
        return server.allServers
            .sortedBy { it.playersConnected.size }
            .firstOrNull { isRestrictedLobbyServer(it.serverInfo) } ?: fetchBestLobby()
    }

    fun isLobbyServer(info: ServerInfo): Boolean
    {
        return (info.name.contains("hub") || info.name.contains("lobby"))
    }

    private fun isRestrictedLobbyServer(info: ServerInfo): Boolean
    {
        return (info.name.contains("hub") || info.name.contains("lobby")) && info.name.contains("restricted")
    }

    fun updateMaintenanceRank(lemonSpigotRank: Rank)
    {
        settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
        {
            this.connection.useResource {
                sync().hset("cocoa:unique-ids", "maintenance-rank", lemonSpigotRank.uuid.toString())
            }
        }
    }

    fun updateMaintenanceMode(lemonSpigotRank: Rank?)
    {
        val newValue = !maintenance
        proxySettings["maintenance"] = newValue

        if (lemonSpigotRank != null)
        {
            settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
            {
                this.connection.useResource {
                    sync().hset("cocoa:unique-ids", "maintenance-rank", lemonSpigotRank.uuid.toString())
                }
            }
        }

        settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
        {
            this.connection.useResource {
                sync().hset("cocoa:metadata", "maintenance", newValue.toString())
            }
        }
    }

    fun shouldAllowOrStay(uuid: UUID): Boolean
    {
        val maintenanceRank = maintenanceRank()

        val rankOfPlayer = DataStoreHandler.findRank(
            DataStoreHandler.rankOf[uuid]
        ) ?: DataStoreHandler.getDefaultRank()

        return rankOfPlayer.weight >= maintenanceRank.weight
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
