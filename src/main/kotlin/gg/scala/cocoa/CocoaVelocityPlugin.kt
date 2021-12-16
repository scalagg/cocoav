package gg.scala.cocoa

import com.google.inject.Inject
import com.solexgames.datastore.commons.connection.impl.redis.AuthRedisConnection
import com.solexgames.datastore.commons.connection.impl.redis.NoAuthRedisConnection
import com.solexgames.datastore.commons.layer.impl.RedisStorageLayer
import com.solexgames.datastore.commons.storage.impl.RedisStorageBuilder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.banana.Banana
import gg.scala.banana.BananaBuilder
import gg.scala.banana.options.BananaOptions
import gg.scala.cocoa.handler.CocoaRedisHandler
import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.cocoa.handler.LemonRedisHandler
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.cocoa.listener.PlayerListener
import gg.scala.cocoa.presence.CocoaServerPresence
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.evilblock.cubed.store.uuidcache.impl.RedisUUIDCache
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger
import kotlin.properties.Delegates


/**
 * @author GrowlyX
 * @since 12/15/2021
 */
@Plugin(
    id = "cocoa",
    name = "cocoa",
    version = "v_stb-1.0.0",
    url = "https://scala.gg",
    authors = [
        "Scala"
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

    val directoryFile: File = directory.toFile()

    var uuidCache by Delegates.notNull<RedisUUIDCache>()

    var proxyBanana by Delegates.notNull<Banana>()
    var spigotBanana by Delegates.notNull<Banana>()

    var maintenanceLayer by Delegates.notNull<RedisStorageLayer<UUID>>()
    var settingsLayer by Delegates.notNull<RedisStorageLayer<Boolean>>()

    val proxySettings = mutableMapOf<String, Boolean>()

    var maintenanceRank: String? = null

    val maintenance: Boolean
        get() = proxySettings.getOrDefault("maintenance", false)

    init
    {
        INSTANCE = this

        enable()
    }

    private fun enable()
    {
        CocoaSettingsProcessor.initialLoad()

        CocoaSettingsProcessor.loadSettings<CocoaVelocitySettings>("settings")
        CocoaSettingsProcessor.loadSettings<CocoaServerPresenceSettings>("presence")

        startRedisFunctionality()
    }

    @Subscribe
    fun onInitialize(event: ProxyInitializeEvent?)
    {
        server.eventManager.register(this, PlayerListener)
        server.eventManager.register(this, CocoaServerPresence)
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

        proxyBanana.registerClass(CocoaRedisHandler)
        spigotBanana.registerClass(LemonRedisHandler)

        proxyBanana.subscribe()
        spigotBanana.subscribe()

        val connection = if (!settings.credentials.authenticate)
        {
            NoAuthRedisConnection(
                settings.credentials.address,
                settings.credentials.port
            )
        } else
        {
            AuthRedisConnection(
                settings.credentials.address,
                settings.credentials.port,
                settings.credentials.password
            )
        }

        maintenanceLayer = RedisStorageBuilder<UUID>()
            .setConnection(connection)
            .setType(UUID::class.java)
            .setSection("cocoa:maintenance")
            .build()

        settingsLayer = RedisStorageBuilder<Boolean>()
            .setConnection(connection)
            .setType(Boolean::class.java)
            .setSection("cocoa:settings")
            .build()
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

    fun isRestrictedLobbyServer(info: ServerInfo): Boolean
    {
        return (info.name.contains("hub") || info.name.contains("lobby")) && info.name.contains("restricted")
    }

    fun addToMaintenanceList(uuid: UUID): CompletableFuture<Void>
    {
        return maintenanceLayer.saveEntry(uuid.toString(), uuid)
    }

    fun removeFromMaintenanceList(uuid: UUID): CompletableFuture<Void>
    {
        return maintenanceLayer.deleteEntry(uuid.toString())
    }

    fun updateMaintenanceRank(rank: Rank)
    {
        settingsLayer.runCommand {
            it.hset("cocoa:metadata", "maintenance-rank", rank.uuid.toString())
        }
    }

    fun updateMaintenanceMode(rank: Rank?): CompletableFuture<Void>
    {
        val newValue = !maintenance
        proxySettings["maintenance"] = newValue

        if (rank != null)
        {
            settingsLayer.runCommand {
                it.hset("cocoa:metadata", "maintenance-rank", rank.uuid.toString())
            }
        }

        return settingsLayer.saveEntry("maintenance", newValue)
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
