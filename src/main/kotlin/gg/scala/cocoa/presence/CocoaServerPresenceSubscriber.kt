package gg.scala.cocoa.presence

import co.schemati.trevor.api.TrevorService
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
typealias Players = ServerPing.Players
typealias Version = ServerPing.Version

object CocoaServerPresenceSubscriber
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    private val whitelisted = Version(
        5000, "Whitelisted"
    )

    private val brand = CocoaSettingsProcessor
        .locate<CocoaVelocitySettings>()!!
        .serverBrand

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent)
    {
        event.ping = ServerPing(
            if (plugin.maintenance)
            {
                whitelisted
            } else
            {
                Version(
                    event.ping.version.protocol,
                    brand
                )
            },
            Players(
                TrevorService.getAPI().instanceData.playerCount,
                CocoaSettingsProcessor
                    .locate<CocoaServerPresenceSettings>()
                    ?.playerCap ?: 1000,
                mutableListOf()
            ),
            CocoaServerPresenceRunnable
                .cachedPresence(),
            event.ping.favicon.orElse(null),
            event.ping.modinfo.orElse(null)
        )
    }
}
