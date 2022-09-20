package gg.scala.cocoa.presence

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import java.util.UUID

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object CocoaServerPresenceRunnable : Runnable
{
    private val cachedPresences = mutableMapOf<String, Component>()

    private val fallback: UUID = UUID.randomUUID()
    private val fallbackPresence = Component
        .text("Fallback proxy state - is something wrong?")
        .color(NamedTextColor.GRAY)

    fun cachedPresence(): Component
    {
        return cachedPresences[
                kotlin.runCatching {
                    CocoaServerPresenceHandler.current.getUniqueId()
                }.getOrDefault(fallback)
        ] ?: fallbackPresence
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
                cachedPresences[it.getUniqueId()] =
                    it.updatedPresence()
            }
    }
}
