package gg.scala.cocoa.discovery

import com.velocitypowered.api.proxy.server.ServerInfo
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import java.net.InetSocketAddress

/**
 * @author GrowlyX
 * @since 7/2/2022
 */
object CocoaConsulRefresh : Runnable
{
    override fun run()
    {
        val health = CocoaConsulDiscovery
            .consul()
            .healthClient()

        val healthy = CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!
            .listeningGroups
            .flatMap {
                health.getHealthyServiceInstances(it)
                    .response
            }

        val mapped = healthy
            .map {
                it.service.id
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
            .forEach {
                CocoaVelocityPlugin
                    .INSTANCE.server
                    .unregisterServer(
                        it.serverInfo
                    )

                CocoaVelocityPlugin
                    .INSTANCE.logger
                    .info(
                        "[Consul] Unregistered dead server: ${it.serverInfo.name}"
                    )
            }

        healthy
            .filter {
                !registeredNames
                    .contains(
                        it.service.id
                    )
            }
            .forEach {
                val info = ServerInfo(
                    it.service.id,
                    InetSocketAddress(
                        it.service.address,
                        it.service.port
                    )
                )

                CocoaVelocityPlugin
                    .INSTANCE.server
                    .registerServer(info)

                CocoaVelocityPlugin
                    .INSTANCE.logger
                    .info(
                        "[Consul] Registered healthy server: \${it.service.id}"
                    )
            }
    }
}
