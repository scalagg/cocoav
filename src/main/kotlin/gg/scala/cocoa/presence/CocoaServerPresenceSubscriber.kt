package gg.scala.cocoa.presence

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.motdForProtocol
import gg.scala.cocoa.symphony.SymphonyCompat

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
typealias Players = ServerPing.Players
typealias Version = ServerPing.Version

object CocoaServerPresenceSubscriber
{
    private val whitelisted = Version(
        5000, "Whitelisted"
    )

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent)
    {
        val settings = CocoaVelocitySettingsDataSync.cached()
        val onIncompatibleVersion = settings.allowedVersions != null && event.ping.version.protocol !in settings.allowedVersions!!

        event.ping = ServerPing(
            if (settings.maintenance.enabled && !settings.showPlayerCountWhileWhitelisted)
            {
                whitelisted
            } else if (onIncompatibleVersion)
            {
                Version(
                    5000,
                    CocoaVelocitySettingsDataSync.cached().serverBrand
                )
            } else
            {
                Version(
                    event.ping.version.protocol,
                    CocoaVelocitySettingsDataSync.cached()
                        .serverBrand
                )
            },
            Players(
                SymphonyCompat.globalPlayerCount(),
                settings.presence.playerCap,
                mutableListOf()
            ),
            motdForProtocol(CocoaServerPresenceRunnable.cachedPresence(), event.ping.version.protocol),
            event.ping.favicon.orElse(null),
            event.ping.modinfo.orElse(null)
        )
    }
}
