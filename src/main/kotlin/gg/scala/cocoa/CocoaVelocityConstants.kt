package gg.scala.cocoa

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * @author GrowlyX
 * @since 12/15/2021
 */
object CocoaVelocityConstants
{
    @JvmStatic
    val MAINTENANCE by lazy {
        LegacyComponentSerializer.legacyAmpersand()
            .deserialize(
                """
                    &cWe're currently under maintenance.
                    &cAdditional info may be found at &ldiscord.gg/minequest&c.
                """.trimIndent()
            )
    }

    @JvmStatic
    val NEW_LINE = LegacyComponentSerializer
        .legacyAmpersand().deserialize("&r\n")
}
