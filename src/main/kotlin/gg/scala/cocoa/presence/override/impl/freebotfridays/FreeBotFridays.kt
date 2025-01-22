package gg.scala.cocoa.presence.override.impl.freebotfridays

import java.time.*

/**
 * @author GrowlyX
 * @since 8/9/2024
 */
object FreeBotFridays {
    private val newYorkLocalEST = ZoneId.of("America/New_York")

    private val fbfStart = LocalTime.of(15, 30) // 3:30 PM EST on Friday
    private val fbfEnd = LocalTime.of(22, 0)   // 10:00 PM EST on Sunday

    fun isFreeBotFriday(): Boolean {
        val now = ZonedDateTime.now(newYorkLocalEST)
        val currentTime = now.toLocalTime()

        return when (now.dayOfWeek) {
            DayOfWeek.FRIDAY -> currentTime.isAfter(fbfStart)
            DayOfWeek.SATURDAY -> true // All day Saturday
            DayOfWeek.SUNDAY -> currentTime.isBefore(fbfEnd)
            else -> false
        }
    }

    fun timeUntilNextFreeBotFriday(): Duration {
        val now = ZonedDateTime.now(newYorkLocalEST)

        val nextFriday = now.withDayOfWeek(DayOfWeek.FRIDAY).with(fbfStart)
        val targetDateTime = if (now.dayOfWeek == DayOfWeek.FRIDAY && now.toLocalTime().isAfter(fbfStart)) {
            nextFriday.plusWeeks(1)
        } else {
            nextFriday
        }

        // Calculate the duration between now and the target date and time
        return Duration.between(now, targetDateTime)
    }

    fun ZonedDateTime.withDayOfWeek(dayOfWeek: DayOfWeek): ZonedDateTime {
        var result = this.with(dayOfWeek)
        if (result.isBefore(this)) {
            result = result.plusWeeks(1)
        }
        return result
    }
}