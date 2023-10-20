package gg.scala.cocoa.presence.override.impl

import gg.scala.cocoa.CocoaVelocityPlugin
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.presence.override.PresenceOverride
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object WhitelistedPresenceOverride : PresenceOverride
{
    private val plugin = CocoaVelocityPlugin.INSTANCE

    override fun getUniqueId() = "maintenance"
    override fun getWeight() = 25000

    override fun ableToBeManuallyToggle() = false
    override fun shouldOverride() = plugin.maintenance

    override fun updatePresence(): Component
    {
        val settings = CocoaSettingsProcessor
            .locate<CocoaServerPresenceSettings>()!!

        return MiniMessage.miniMessage()
            .deserialize(
                settings.whitelistedPresence
            )
    }
}
