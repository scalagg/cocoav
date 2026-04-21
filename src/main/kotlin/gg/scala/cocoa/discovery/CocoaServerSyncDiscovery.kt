package gg.scala.cocoa.discovery

import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettingsDataSync
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
        kotlin.runCatching {
            val healthy = CocoaVelocitySettingsDataSync.cached()
                .listeningGroups
                .flatMap {
                    ServerContainer.getServersInGroup(it)
                }
                .filterIsInstance<GameServer>()

            val mapped = healthy
                .map {
                    it.id
                }

            val registered = CocoaVelocityPlugin.INSTANCE.server.allServers
            val registeredNames = registered.map { it.serverInfo.name }

            registered
                .filter {
                    !mapped
                        .contains(
                            it.serverInfo.name
                        )
                }
                .filter {
                    !CocoaVelocitySettingsDataSync.cached()
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
                .forEach {
                    val host = if (CocoaVelocityPlugin.INSTANCE.instanceSettings.docker) "172.18.0.1" else it.getHost()
                    val port = (it.getMetadata("server", "port")?.value as Double? ?: return@forEach).toInt()
                    val address = InetSocketAddress(host, port)
                    val info = ServerInfo(it.id, address)

                    val existing = CocoaVelocityPlugin.INSTANCE.server
                        .getServer(it.id).orElse(null)

                    if (existing == null)
                    {
                        CocoaVelocityPlugin
                            .INSTANCE.server
                            .registerServer(info)

                        CocoaVelocityPlugin
                            .INSTANCE.logger
                            .info(
                                "[ServerSync] Registered healthy server: ${it.id}"
                            )
                    } else if (existing.serverInfo.address != address)
                    {
                        CocoaVelocityPlugin
                            .INSTANCE.server
                            .unregisterServer(existing.serverInfo)

                        CocoaVelocityPlugin
                            .INSTANCE.server
                            .registerServer(info)

                        CocoaVelocityPlugin
                            .INSTANCE.logger
                            .info(
                                "[ServerSync] Updated server address: ${it.id} -> $host:$port"
                            )
                    }
                }
        }.onFailure {
            it.printStackTrace()
        }
    }
}
