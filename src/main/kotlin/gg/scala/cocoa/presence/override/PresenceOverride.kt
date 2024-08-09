package gg.scala.cocoa.presence.override

import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import gg.scala.cocoa.centered
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
interface PresenceOverride
{
    fun getUniqueId(): String
    fun getWeight(): Int

    fun shouldOverride(): Boolean
    fun updatePresence(): Component

    fun updatedPresence(): Component
    {
        val settings = CocoaVelocitySettingsDataSync.cached().presence
        if (settings.shouldCenterPresence)
        {
            val deserialized = LegacyComponentSerializer
                .legacyAmpersand()
                .serialize(updatePresence())

            val parts = mutableListOf<String>()

            for (part in deserialized.split("\n"))
            {
                parts += part.centered(132.2)
            }

            return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(
                    parts.joinToString(separator = "\n")
                )
        } else
        {
            return updatePresence()
        }
    }
}
