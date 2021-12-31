package gg.scala.cocoa

import co.schemati.trevor.api.TrevorService
import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.banana.Banana
import gg.scala.banana.BananaBuilder
import gg.scala.banana.options.BananaOptions
import gg.scala.cocoa.command.GlobalListCommand
import gg.scala.cocoa.command.LobbyCommand
import gg.scala.cocoa.command.MaintenanceCommand
import gg.scala.cocoa.command.administration.*
import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.countdown.CocoaCountdownPresence
import gg.scala.cocoa.countdown.CocoaCountdownSettings
import gg.scala.cocoa.subscribe.CocoaRedisSubscriber
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.handler.LemonRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.listener.PlayerListener
import gg.scala.cocoa.permission.CocoaPlayerPermissionsAdapter
import gg.scala.cocoa.presence.CocoaServerPresenceHandler
import gg.scala.cocoa.presence.CocoaServerPresenceSubscriber
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.presence.override.impl.WhitelistedPresenceOverride
import gg.scala.cocoa.settings.CocoaSettingsModel
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.store.controller.DataStoreObjectController
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.impl.RedisDataStoreStorageLayer
import gg.scala.store.storage.type.DataStoreStorageType
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.store.uuidcache.impl.RedisUUIDCache
import revxrsal.commands.exception.CommandErrorException
import revxrsal.commands.velocity.VelocityCommandHandler
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.properties.Delegates


/**
 * @author GrowlyX
 * @since 12/15/2021
 */
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

    var uuidCache by Delegates.notNull<RedisUUIDCache>()

    var proxyBanana by Delegates.notNull<Banana>()
    var spigotBanana by Delegates.notNull<Banana>()

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

        DataStoreHandler.initialLoad()

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
            val firstArg = it.input().removeFirst()

            return@registerContextResolver DataStoreHandler.findRank(firstArg)
                ?: throw CommandErrorException("Sorry, that rank does not exist.")
        }

        commandManager.autoCompleter.registerParameterSuggestions(Rank::class.java) { _, _, _ ->
            return@registerParameterSuggestions DataStoreHandler.ranks.values.map { it.name }
        }

        commandManager.registerPermissionReader(CocoaPlayerPermissionsAdapter)
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
        val settings = CocoaSettingsProcessor.locate<CocoaVelocitySettings>()!!

        proxyBanana = BananaBuilder()
            .options(
                BananaOptions(
                    channel = "cocoa",
                    async = true
                )
            )
            .credentials(
                settings.credentials
            )
            .build()

        spigotBanana = BananaBuilder()
            .options(
                BananaOptions(
                    channel = "lemon:spigot",
                    async = true,
                    ignoreNonExistentHandlers = true
                )
            )
            .credentials(
                settings.credentials
            )
            .build()

        proxyBanana.registerClass(CocoaRedisSubscriber)
        spigotBanana.registerClass(LemonRedisHandler)

        proxyBanana.subscribe()
        spigotBanana.subscribe()

        settingsLayer = DataStoreObjectControllerCache.create(Serializers.gson)
        cloudLayer = DataStoreObjectControllerCache.create(Serializers.gson)

        val executor = Executors.newScheduledThreadPool(1)

        executor.scheduleAtFixedRate({
            server.allPlayers.forEach {
                DataStoreHandler.updateRank(it.uniqueId)
            }
        }, 0L, 2L, TimeUnit.SECONDS)

        executor.scheduleAtFixedRate({
            settingsLayer
                .loadAll(DataStoreStorageType.REDIS)
                .thenAccept {
                    proxySettings.clear()

                    it.forEach { entry ->
                        proxySettings[entry.value.id] = entry.value.value
                    }
                }

            try
            {
                settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
                {
                    this.connection.useResource {
                        hset("cocoa:players", "global", TrevorService.getAPI().instanceData.playerCount.toString())

                        maintenanceRank = hget("cocoa:metadata", "maintenance-rank")
                    }
                }
            } catch (e: Exception)
            {
                logger.warning("Failed to update online players! Is Trevor not enabled?")
            }
        }, 0L, 1L, TimeUnit.SECONDS)
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
                hset("cocoa:metadata", "maintenance-rank", lemonSpigotRank.uuid.toString())
            }
        }
    }

    fun updateMaintenanceMode(lemonSpigotRank: Rank?): CompletableFuture<Void>
    {
        val newValue = !maintenance
        proxySettings["maintenance"] = newValue

        if (lemonSpigotRank != null)
        {
            settingsLayer.useLayer<RedisDataStoreStorageLayer<CocoaVelocitySettingsEntry>>(DataStoreStorageType.REDIS)
            {
                this.connection.useResource {
                    hset("cocoa:metadata", "maintenance-rank", lemonSpigotRank.uuid.toString())
                }
            }
        }

        return settingsLayer.save(
            CocoaVelocitySettingsEntry("maintenance", newValue),
            DataStoreStorageType.REDIS
        )
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
