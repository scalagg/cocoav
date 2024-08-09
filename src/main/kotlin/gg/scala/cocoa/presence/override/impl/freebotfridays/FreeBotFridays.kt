package gg.scala.cocoa.presence.override.impl.freebotfridays

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * @author GrowlyX
 * @since 8/9/2024
 */
object FreeBotFridays
{
    private val newYorkLocalEST = ZoneId.of("America/New_York")

    private val fbfStart = LocalTime.of(15, 30) // 3:30PM EST
    private val fbfEnd = LocalTime.of(19, 30) // 7:30PM EST

    fun isFreeBotFriday(): Boolean
    {
        val now = ZonedDateTime.now(newYorkLocalEST)
        val currentTime = now.toLocalTime()

        return now.dayOfWeek == DayOfWeek.FRIDAY &&
            currentTime.isAfter(fbfStart) &&
            currentTime.isBefore(fbfEnd)
    }

}
