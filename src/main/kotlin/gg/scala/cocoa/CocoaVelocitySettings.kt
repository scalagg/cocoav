package gg.scala.cocoa

import gg.scala.banana.credentials.BananaCredentials
import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocitySettings(
    val credentials: BananaCredentials = BananaCredentials(),
    val mongoUri: String = "mongodb://127.0.0.1:27017",
    val networking: NetworkingSettings = NetworkingSettings()
) : CocoaSettingsModel()
{
    override fun getAbstractType() = CocoaVelocitySettings::class.java

    class NetworkingSettings(
        val handleNetworkingChecks: Boolean = false,
        val apiKey: String = ""
    )
}
