package gg.scala.cocoa.network

import com.google.gson.annotations.SerializedName

/**
 * A de-serialized model of the data returned
 * from the `vpnblocker.net` API request.
 *
 * Fields should be marked with [JvmField].
 *
 * @author GrowlyX
 * @since 12/24/2021
 */
data class CocoaNetworkAddressModel(
    @JvmField
    val status: String
)
{
    @JvmField
    @SerializedName("host-ip")
    val shouldBlock: Boolean = false

    @JvmField
    @SerializedName("org")
    val networkId: String = "N/A"
}
