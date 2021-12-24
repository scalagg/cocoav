package gg.scala.cocoa.lemon.expirable

/**
 * @author puugz, GrowlyX
 * @since 23/08/2021 18:32
 */
open class LemonExpirable(
    var addedAt: Long,
    var duration: Long
) {
    val isPermanent: Boolean
        get() = duration == Long.MAX_VALUE

    val hasExpired: Boolean
        get() = !isPermanent && System.currentTimeMillis() >= addedAt + duration
}
