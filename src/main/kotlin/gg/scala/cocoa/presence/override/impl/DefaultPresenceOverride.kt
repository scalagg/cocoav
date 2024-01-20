package gg.scala.cocoa.presence.override.impl

import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.presence.override.PresenceOverride
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object DefaultPresenceOverride : PresenceOverride
{
    override fun getUniqueId() = "default"
    override fun getWeight() = 0

    override fun ableToBeManuallyToggle() = false
    override fun shouldOverride() = true

    override fun updatePresence(): Component
    {
        return MiniMessage.miniMessage()
            .deserialize(
                CocoaVelocitySettingsDataSync.cached()
                    .presence.normalPresence
            )
    }
}
