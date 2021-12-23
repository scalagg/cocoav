package gg.scala.cocoa.countdown

import gg.scala.cocoa.expirable.Expirable

/**
 * @author GrowlyX
 * @since 12/17/2021
 */
class CocoaCountdown(
    val id: String, duration: Long
) : Expirable(
    System.currentTimeMillis(), duration
)
{
    var activated = false


}
