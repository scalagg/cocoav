package gg.scala.cocoa.presence

import net.kyori.adventure.text.TextComponent
import java.util.concurrent.TimeUnit

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object CocoaServerPresenceThread : Thread()
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
        try
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
        } catch (exception: Exception)
        {
            exception.printStackTrace()
        }

        try
        {
            TimeUnit.SECONDS.sleep(1L)
        } catch (ignored: Exception)
        {
            println("Failed to sleep presence thread!")
        }
    }
}
