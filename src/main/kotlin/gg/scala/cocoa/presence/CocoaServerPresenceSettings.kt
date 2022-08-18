package gg.scala.cocoa.presence

import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaServerPresenceSettings(
    var normalPresence: String = "Hey",
    var whitelistedPresence: String = "cocoaV Lol",
    var whitelistedNotification: String = "",
    var shouldCenterPresence: Boolean = false
) : CocoaSettingsModel()
{
    override fun getAbstractType() =
        CocoaServerPresenceSettings::class.java
}
