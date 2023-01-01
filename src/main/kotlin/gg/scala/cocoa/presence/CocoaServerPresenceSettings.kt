package gg.scala.cocoa.presence

import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaServerPresenceSettings(
    var playerCap: Int = 1000,
    var normalPresence: String = "Hey",
    var whitelistedPresence: String = "cocoaV Lol",
    var whitelistedNotification: String = """
        <red>We're currently whitelisted.
        
        <red>Additional information may be found in:
        <red><bold>discord.gg/asdf
    """.trimIndent(),
    var shouldCenterPresence: Boolean = false
) : CocoaSettingsModel()
{
    override fun getAbstractType() =
        CocoaServerPresenceSettings::class.java
}
