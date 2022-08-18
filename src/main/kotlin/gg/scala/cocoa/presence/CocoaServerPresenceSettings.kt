package gg.scala.cocoa.presence

import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaServerPresenceSettings(
    var normalPresence: String = "Hey",
    var whitelistedPresence: String = "cocoaV Lol",
    var whitelistedNotification: String = "&cThe network is currently whitelisted!\n&cJoin our discord for more information!",
    var shouldCenterPresence: Boolean = false
) : CocoaSettingsModel()
{
    override fun getAbstractType() =
        CocoaServerPresenceSettings::class.java
}
