package gg.scala.cocoa.discovery

import com.orbitz.google.common.net.HostAndPort
import com.orbitz.consul.Consul
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor

/**
 * @author GrowlyX
 * @since 7/2/2022
 */
object CocoaConsulDiscovery
{
    private val settings by lazy {
        CocoaSettingsProcessor.locate<CocoaVelocitySettings>()!!
    }

    private val consul = Consul
        .builder()
        .withHostAndPort(
            HostAndPort.fromParts(
                settings.consul.address,
                settings.consul.port
            )
        )
        .withReadTimeoutMillis(1000L)
        .build()!!

    fun consul() = consul
}
