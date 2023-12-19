package gg.scala.cocoa

import gg.scala.aware.uri.WrappedAwareUri
import gg.scala.cocoa.settings.CocoaSettingsModel

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocitySettings(
    val externalProxy: Boolean = false,
    val masterProxy: Boolean = true,
    val showPlayerCountWhileWhitelisted: Boolean = false,
    val autoRegisterFromServerSyncDiscovery: Boolean = true,
    val ensureTokenWhitelisted: Boolean = false,
    val tokenWhitelistHighestWeightBeforeNotEffective: Int = 99,
    val externalProxyPrefix: String = "-ofl",
    val serverBrand: String = "1.8.9-1.19.2",
    val middleware: String? = null,
    val listeningGroups: List<String> = listOf("practice", "hub"),
    val listeningExemptions: List<String> = listOf("build"),
    val networking: NetworkingSettings = NetworkingSettings(),
    val preferHubServerRedirects: Map<String, String> = mutableMapOf(
        "mipgame" to "miplobby"
    ),
    val totpIssuer: String? = "Scala%20Studios",
    val bitlyApiKey: String? = ""
) : CocoaSettingsModel()
{
    override fun getAbstractType() = CocoaVelocitySettings::class.java

    class NetworkingSettings(
        val handleNetworkingChecks: Boolean = false,
        val apiKey: String = ""
    )
}
