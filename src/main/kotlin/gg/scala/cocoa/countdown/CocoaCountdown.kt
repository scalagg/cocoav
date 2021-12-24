package gg.scala.cocoa.countdown

import gg.scala.cocoa.lemon.expirable.LemonExpirable
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 12/17/2021
 */
class CocoaCountdown : LemonExpirable(
    System.currentTimeMillis(), 0L
)
{
    companion object
    {
        @JvmStatic
        var INSTANCE by Delegates.notNull<CocoaCountdown>()

        fun initialLoad()
        {
            INSTANCE = CocoaSettingsProcessor
                .locate<CocoaCountdownSettings>()!!.countdown
        }
    }

    var activated = false

    var presenceFormat = "Something happens in [detailed]"
    var commencedFormat = "Something just happened"
}
