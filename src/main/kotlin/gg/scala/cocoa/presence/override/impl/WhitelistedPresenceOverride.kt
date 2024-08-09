package gg.scala.cocoa.presence.override.impl

import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.presence.override.PresenceOverride
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object WhitelistedPresenceOverride : PresenceOverride
{
    override fun getUniqueId() = "maintenance"
    override fun getWeight() = 25000

    override fun shouldOverride() = CocoaVelocitySettingsDataSync.cached()
        .maintenance.enabled

    override fun updatePresence(): Component
    {
        return MiniMessage.miniMessage()
            .deserialize(
                CocoaVelocitySettingsDataSync.cached()
                    .presence.whitelistedPresence
            )
    }
}
