package gg.scala.cocoa

import com.google.inject.Inject
import com.solexgames.datastore.commons.connection.impl.redis.AuthRedisConnection
import com.solexgames.datastore.commons.connection.impl.redis.NoAuthRedisConnection
import com.solexgames.datastore.commons.layer.impl.RedisStorageLayer
import com.solexgames.datastore.commons.storage.impl.RedisStorageBuilder
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import gg.scala.banana.Banana
import gg.scala.banana.BananaBuilder
import gg.scala.banana.options.BananaOptions
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.evilblock.cubed.store.uuidcache.impl.RedisUUIDCache
import java.nio.file.Path
import java.util.*
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

    val directoryFile = directory.toFile()

    var uuidCache by Delegates.notNull<RedisUUIDCache>()

    var proxyBanana by Delegates.notNull<Banana>()
    var spigotBanana by Delegates.notNull<Banana>()

    var maintenanceLayer by Delegates.notNull<RedisStorageLayer<UUID>>()
    var settingsLayer by Delegates.notNull<RedisStorageLayer<Boolean>>()

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

//        proxyBanana.registerClass(CocoaRedisHandler)
//        spigotBanana.registerClass(LemonRedisHandler)

        proxyBanana.subscribe()
        spigotBanana.subscribe()

        val connection = if (!settings.credentials.authenticate) {
            NoAuthRedisConnection(
                settings.credentials.address,
                settings.credentials.port
            )
        } else {
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
}
