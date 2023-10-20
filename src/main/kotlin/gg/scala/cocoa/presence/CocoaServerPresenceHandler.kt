package gg.scala.cocoa.presence

import gg.scala.cocoa.countdown.CocoaCountdownPresence
import gg.scala.cocoa.presence.override.PresenceOverride
import gg.scala.cocoa.presence.override.impl.DefaultPresenceOverride
import gg.scala.cocoa.presence.override.impl.WhitelistedPresenceOverride
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
object CocoaServerPresenceHandler
{
    val presences = mutableListOf<PresenceOverride>()
    var current by Delegates.notNull<PresenceOverride>()

    private val scheduler: ScheduledExecutorService =
        Executors.newScheduledThreadPool(1)

    fun initialLoad()
    {
        scheduler.scheduleAtFixedRate(
            CocoaServerPresenceRunnable,
            0L, 1L, TimeUnit.SECONDS
        )

        presences.add(DefaultPresenceOverride)
        presences.add(WhitelistedPresenceOverride)
        presences.add(CocoaCountdownPresence)
    }
}
