package gg.scala.cocoa.network

import gg.scala.cocoa.CocoaVelocitySettingsDataSync
import kong.unirest.Unirest
import net.evilblock.cubed.serializers.Serializers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles everything in relation
 * to networking filters.
 *
 * @author GrowlyX
 * @since 12/24/2021
 */
object CocoaNetworkAddressHandler
{
    private val vpnBlockerResponses =
        ConcurrentHashMap<String, CocoaNetworkAddressModel>()

    /**
     * Requests host data for the target ip address.
     *
     * @param ipAddress the ip address in its [String] form
     * @return nullable [CocoaNetworkAddressModel]
     */
    fun requestHostData(ipAddress: String): CompletableFuture<CocoaNetworkAddressModel?>
    {
        val cached = vpnBlockerResponses[ipAddress]

        if (cached != null)
        {
            return CompletableFuture.completedFuture(cached)
        }

        val apiKey = CocoaVelocitySettingsDataSync.cached()
            .networking.apiKey

        return Unirest
            .get("https://api.vpnblocker.net/v2/json/$ipAddress")
            .header("X-API-KEY", apiKey)
            .asStringAsync()
            .thenApply {
                if (it.status != 200)
                {
                    println("Failed VPNBlocker request: returned ${it.status}")
                    return@thenApply null
                }

                Serializers.gson
                    .fromJson(
                        it.body, CocoaNetworkAddressModel::class.java
                    )
                    .apply {
                        if (this != null)
                        {
                            vpnBlockerResponses[ipAddress] = this
                        }
                    }
            }
    }
}
