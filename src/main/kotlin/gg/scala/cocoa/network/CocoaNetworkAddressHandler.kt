package gg.scala.cocoa.network

import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import net.evilblock.cubed.serializers.Serializers
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit

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
        CocoaSettingsProcessor.locate<CocoaVelocitySettings>()!!
            .networking.apiKey
    }

    @JvmStatic
    val REQUEST_URL = "https://api.vpnblocker.net/v2/json/%s/$API_KEY"

    /**
     * Requests host data for the target ip address.
     *
     * @param ipAddress the ip address in its [String] form
     * @return nullable [CocoaNetworkAddressModel]
     */
    fun requestHostData(ipAddress: String): CocoaNetworkAddressModel?
    {
        val url = URL(
            String.format(REQUEST_URL, ipAddress)
        )

        return Serializers.gson.fromJson(
            InputStreamReader(
                url.openStream()
            ),
            CocoaNetworkAddressModel::class.java
        )
    }
}
