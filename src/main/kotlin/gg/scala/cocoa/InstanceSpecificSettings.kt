package gg.scala.cocoa

/**
 * @author GrowlyX
 * @since 1/19/2024
 */
data class InstanceSpecificSettings(
    val proxyId: String = "proxy-1",
    val masterProxy: Boolean = true,
    val externalProxy: Boolean = false,
    val externalProxyPrefix: String = "-ofl",
    val docker: Boolean = false,
    val apiID: String = "dewgs",
    val apiSupplier: String = "api.scala.gg",
    val apiUseHTTPS: Boolean = true
)
