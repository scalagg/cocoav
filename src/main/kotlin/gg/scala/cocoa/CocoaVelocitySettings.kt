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
    val listeningGroups: List<String> = listOf("practice", "hub"),
    val listeningExemptions: List<String> = listOf("build"),
    val consul: ConsulSettings = ConsulSettings(),
    val credentials: WrappedAwareUri = WrappedAwareUri(),
    val networking: NetworkingSettings = NetworkingSettings()
) : CocoaSettingsModel()
{
    override fun getAbstractType() = CocoaVelocitySettings::class.java

    class NetworkingSettings(
        val handleNetworkingChecks: Boolean = false,
        val apiKey: String = ""
    )

    class ConsulSettings(
        val address: String = "127.0.0.1",
        val port: Int = 8500
    )
}
