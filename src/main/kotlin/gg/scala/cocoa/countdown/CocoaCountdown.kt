package gg.scala.cocoa.countdown

import gg.scala.cocoa.expirable.Expirable

/**
 * @author GrowlyX
 * @since 12/17/2021
 */
object CocoaCountdown : Expirable(
    System.currentTimeMillis(), 0L
)
{
    var activated = false

    var presenceFormat = "Something happens in [detailed]"
    var commencedFormat = "Something just happened"
}
