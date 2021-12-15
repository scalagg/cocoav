package gg.scala.cocoa

import com.google.inject.Inject
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
import java.util.logging.Logger

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

}
