package gg.scala.cocoa

import gg.scala.banana.credentials.BananaCredentials
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocitySettings(
    val proxyId: String = "proxy-1",
    val credentials: BananaCredentials = BananaCredentials(),
    val presence: CocoaServerPresenceSettings
) : CocoaSettingsModel
