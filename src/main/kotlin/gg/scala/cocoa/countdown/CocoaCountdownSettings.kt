package gg.scala.cocoa.countdown

import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
class CocoaCountdownSettings(
    val countdown: CocoaCountdown = CocoaCountdown
) : CocoaSettingsModel()
{
    override fun getAbstractType() = CocoaCountdownSettings::class.java
}
