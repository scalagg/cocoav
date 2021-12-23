package gg.scala.cocoa.presence

import gg.scala.cocoa.presence.override.PresenceOverride
import gg.scala.cocoa.presence.override.impl.DefaultPresenceOverride
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object CocoaServerPresenceHandler
{
    val presences = mutableListOf<PresenceOverride>()
    var current by Delegates.notNull<PresenceOverride>()

    fun initialLoad()
    {
        CocoaServerPresenceThread.isDaemon = true
        CocoaServerPresenceThread.start()

        presences.add(DefaultPresenceOverride)
    }

    fun trackPresence(override: PresenceOverride)
    {
        presences.add(override)
    }
}
