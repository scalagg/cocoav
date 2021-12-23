package gg.scala.cocoa.presence

import co.schemati.trevor.api.TrevorService
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaServerPresenceSubscriber
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    private val whitelisted by lazy {
        ServerPing.Version(-1, "● Whitelisted")
    }

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent)
    {
        event.ping = ServerPing(
            if (plugin.maintenance)
            {
                whitelisted
            } else
            {
                event.ping.version
            },
            ServerPing.Players(
                TrevorService.getAPI().instanceData.playerCount,
                10000, mutableListOf()
            ),
            CocoaServerPresenceThread.cachedPresence(),
            event.ping.favicon.orElse(null),
            event.ping.modinfo.orElse(null)
        )
    }
}
