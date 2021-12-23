package gg.scala.cocoa.presence.override

import net.kyori.adventure.text.TextComponent

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
interface PresenceOverride
{
    fun getUniqueId(): String
    fun getWeight(): Int

    fun ableToBeManuallyToggle(): Boolean
    fun shouldOverride(): Boolean

    fun updatePresence(): TextComponent
}
