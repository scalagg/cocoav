package gg.scala.cocoa

import gg.scala.aware.uri.WrappedAwareUri
import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocitySettings(
    val externalProxy: Boolean = false,
    val externalProxyPrefix: String = "-ofl",
    val credentials: WrappedAwareUri = WrappedAwareUri(),
    val networking: NetworkingSettings = NetworkingSettings()
) : CocoaSettingsModel()
{
    override fun getAbstractType() = CocoaVelocitySettings::class.java

    class NetworkingSettings(
        val handleNetworkingChecks: Boolean = false,
        val apiKey: String = ""
    )
}
