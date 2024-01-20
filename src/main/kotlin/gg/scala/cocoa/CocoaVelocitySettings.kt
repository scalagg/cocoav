package gg.scala.cocoa

import gg.scala.cocoa.countdown.CocoaCountdown
import gg.scala.cocoa.presence.CocoaServerPresenceSettings
import java.util.UUID

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
class CocoaVelocitySettings(
    val showPlayerCountWhileWhitelisted: Boolean = false,
    val autoRegisterFromServerSyncDiscovery: Boolean = true,
    val ensureTokenWhitelisted: Boolean = false,
    val tokenWhitelistHighestWeightBeforeNotEffective: Int = 99,
    val serverBrand: String = "1.8.9-1.19.2",
    val middleware: String? = null,
    val listeningGroups: List<String> = listOf("practice", "hub"),
    val listeningExemptions: List<String> = listOf("build"),
    val networking: NetworkingSettings = NetworkingSettings(),
    val preferHubServerRedirects: Map<String, String> = mutableMapOf(
        "mipgame" to "miplobby"
    ),
    val totpIssuer: String = "Scala%20Studios",
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
