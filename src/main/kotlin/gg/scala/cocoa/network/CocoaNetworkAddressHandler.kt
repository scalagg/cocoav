package gg.scala.cocoa.network

import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.evilblock.cubed.serializers.Serializers
import java.io.InputStreamReader
import java.net.URL
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
    @JvmStatic
    val API_KEY by lazy {
        CocoaSettingsProcessor
            .locate<CocoaVelocitySettings>()!!
            .networking.apiKey
    }

    @JvmStatic
    val REQUEST_URL by lazy {
        "https://api.vpnblocker.net/v2/json/$API_KEY"
    }

    private val vpnBlockerResponses =
        ConcurrentHashMap<String, CocoaNetworkAddressModel>()

    /**
     * Requests host data for the target ip address.
     *
     * @param ipAddress the ip address in its [String] form
     * @return nullable [CocoaNetworkAddressModel]
     */
    fun requestHostData(ipAddress: String): CocoaNetworkAddressModel?
    {
        val cached =
            vpnBlockerResponses[ipAddress]

        if (cached != null)
        {
            return cached
        }

        val url = URL(REQUEST_URL)

        return Serializers.gson.fromJson(
            url.openStream().reader(),
            CocoaNetworkAddressModel::class.java
        ).apply {
            if (this != null)
            {
                vpnBlockerResponses[ipAddress] = this
            }
        }
    }
}
