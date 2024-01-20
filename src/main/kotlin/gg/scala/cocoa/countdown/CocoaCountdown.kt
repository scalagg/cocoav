package gg.scala.cocoa.countdown

import gg.scala.cocoa.lemon.expirable.LemonExpirable

/**
 * @author GrowlyX
 * @since 12/17/2021
 */
class CocoaCountdown : LemonExpirable(
    System.currentTimeMillis(), 0L
)
{
    var active = false

    var presenceStaging = "Something happens in [detailed]"
    var presenceCommenced = "Something just happened"
}
