package gg.scala.cocoa

import com.google.inject.Inject
import com.velocitypowered.api.proxy.ProxyServer
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocityPlugin
    @Inject
    constructor(
        val server: ProxyServer,
        val logger: Logger
    )
{

}
