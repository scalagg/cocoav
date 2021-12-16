package gg.scala.cocoa

import com.google.inject.Inject
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
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
        val directory: Path
    )
{
    companion object
    {
        @JvmStatic
        var INSTANCE by Delegates.notNull<CocoaVelocityPlugin>()
    }

    init
    {
        INSTANCE = this
    }

    private val directoryFile = directory.toFile()
}
