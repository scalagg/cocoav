package gg.scala.cocoa

import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import java.util.UUID

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocitySettings(
    var showPlayerCountWhileWhitelisted: Boolean = false,
    var autoRegisterFromServerSyncDiscovery: Boolean = true,
    var ensureTokenWhitelisted: Boolean = false,
    var tokenWhitelistHighestWeightBeforeNotEffective: Int = 99,
    var serverBrand: String = "1.8.9-1.19.2",
    var middleware: String? = null,
    val listeningGroups: MutableList<String> = mutableListOf("practice", "hub"),
    val listeningExemptions: MutableList<String> = mutableListOf("build"),
    val networking: NetworkingSettings = NetworkingSettings(),
    val preferHubServerRedirects: MutableMap<String, String> = mutableMapOf(
        "mipgame" to "miplobby"
    ),
    var totpIssuer: String = "Scala%20Studios",
    val bitlyApiKey: String? = "",
    val countdown: CocoaCountdown = CocoaCountdown(),
    val maintenance: MaintenanceSettings = MaintenanceSettings(),
    val presence: CocoaServerPresenceSettings = CocoaServerPresenceSettings(),
)
{
    data class MaintenanceSettings(
        var enabled: Boolean = false,
        var rank: UUID? = null
    )

    data class NetworkingSettings(
        val handleNetworkingChecks: Boolean = false,
        val apiKey: String = ""
    )
}
