package gg.scala.cocoa.presence

import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaServerPresenceSettings(
    val normalPresence: String = "Hey",
    val whitelistedPresence: String = "cocoaV Lol",
    var shouldCenterPresence: Boolean = false
) : CocoaSettingsModel()
{
    override fun getAbstractType() =
        CocoaServerPresenceSettings::class.java
}
