package gg.scala.cocoa.discovery

import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import gg.scala.commons.agnostic.sync.server.ServerContainer
import gg.scala.commons.agnostic.sync.server.impl.GameServer
import java.net.InetSocketAddress

/**
 * @author GrowlyX
 * @since 7/2/2022
 */
object CocoaServerSyncDiscovery : Runnable
{
    override fun run()
    {
        val healthy = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!
            .listeningGroups
            .flatMap {
                ServerContainer.getServersInGroup(it)
            }
            .filterIsInstance<GameServer>()

        val mapped = healthy
            .map {
                it.id
            }

        val registered = CocoaVelocityPlugin
            .INSTANCE.server.allServers

        val registeredNames = registered
            .map { it.serverInfo.name }

        registered
            .filter {
                !mapped
                    .contains(
                        it.serverInfo.name
                    )
            }
            .filter {
                !CocoaSettingsProcessor
                    .locate<CocoaVelocitySettings>()!!
                    .listeningExemptions
                    .contains(it.serverInfo.name)
            }
            .forEach {
                CocoaVelocityPlugin
                    .INSTANCE.server
                    .unregisterServer(
                        it.serverInfo
                    )

                CocoaVelocityPlugin
                    .INSTANCE.logger
                    .info(
                        "[ServerSync] Unregistered dead server: ${it.serverInfo.name}"
                    )
            }

        healthy
            .filter {
                !registeredNames
                    .contains(
                        it.id
                    )
            }
            .forEach {
                val info = ServerInfo(
                    it.id,
                    InetSocketAddress(
                        it.getHost(), (it.getMetadata("server", "port")?.value as Double? ?: return@forEach).toInt()
                    )
                )

                CocoaVelocityPlugin
                    .INSTANCE.server
                    .registerServer(info)

                CocoaVelocityPlugin
                    .INSTANCE.logger
                    .info(
                        "[ServerSync] Registered healthy server: ${it.id}"
                    )
            }
    }
}
