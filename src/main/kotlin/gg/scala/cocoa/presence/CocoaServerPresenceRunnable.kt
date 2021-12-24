package gg.scala.cocoa.presence

import net.kyori.adventure.text.TextComponent

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object CocoaServerPresenceRunnable : Runnable
{
    private val cachedPresences = mutableMapOf<String, TextComponent>()

    fun cachedPresence(): TextComponent
    {
        return cachedPresences[
                CocoaServerPresenceHandler.current.getUniqueId()
        ]!!
    }

    override fun run()
    {
        val computed = CocoaServerPresenceHandler
            .presences.sortedByDescending {
                it.getWeight()
            }
            .firstOrNull {
                it.shouldOverride()
            }

        if (computed != null)
        {
            CocoaServerPresenceHandler
                .current = computed
        }

        CocoaServerPresenceHandler
            .presences.forEach {
                cachedPresences[it.getUniqueId()] = it.updatePresence()
            }
    }
}
