package gg.scala.cocoa

import java.util.*
import java.util.regex.Pattern

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
private val timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?(?:([0-9]+)\\s*(?:s[a-z]*)?)?", 2)

fun parseDuration(time: String): Long {
    val m = timePattern.matcher(time)
    var years = 0
    var months = 0
    var weeks = 0
    var days = 0
    var hours = 0
    var minutes = 0
    var seconds = 0
    var found = false

    while (m.find()) {
        if (m.group() != null && m.group().isNotEmpty()) {
            for (c in 0 until m.groupCount()) {
                if (m.group(c) != null && m.group(c).isNotEmpty()) {
                    found = true
                    break
                }
            }

            if (found) {
                if (m.group(1) != null && m.group(1).isNotEmpty()) {
                    years = Integer.parseInt(m.group(1))
                }

                if (m.group(2) != null && m.group(2).isNotEmpty()) {
                    months = Integer.parseInt(m.group(2))
                }

                if (m.group(3) != null && m.group(3).isNotEmpty()) {
                    weeks = Integer.parseInt(m.group(3))
                }

                if (m.group(4) != null && m.group(4).isNotEmpty()) {
                    days = Integer.parseInt(m.group(4))
                }

                if (m.group(5) != null && m.group(5).isNotEmpty()) {
                    hours = Integer.parseInt(m.group(5))
                }

                if (m.group(6) != null && m.group(6).isNotEmpty()) {
                    minutes = Integer.parseInt(m.group(6))
                }

                if (m.group(7) != null && m.group(7).isNotEmpty()) {
                    seconds = Integer.parseInt(m.group(7))
                }
                break
            }
        }
    }

    if (!found) {
        throw Exception("Illegal Date")
    } else {
        val calendar = GregorianCalendar()
        val start = calendar.timeInMillis

        if (years > 0) {
            calendar.add(1, years)
        }

        if (months > 0) {
            calendar.add(2, months)
        }

        if (weeks > 0) {
            calendar.add(3, weeks)
        }

        if (days > 0) {
            calendar.add(5, days)
        }

        if (hours > 0) {
            calendar.add(11, hours)
        }

        if (minutes > 0) {
            calendar.add(12, minutes)
        }

        if (seconds > 0) {
            calendar.add(13, seconds)
        }

        val max = GregorianCalendar()
        max.add(1, 2)

        return if (calendar.after(max)) {
            max.timeInMillis - start
        } else {
            calendar.timeInMillis - start
        }
    }
}

fun formatIntoDetailedString(secs: Int): String {
    if (secs == 0) {
        return "0 seconds"
    }
    val remainder = secs % 86400
    val days = secs / 86400
    val hours = remainder / 3600
    val minutes = remainder / 60 - hours * 60
    val seconds = remainder % 3600 - minutes * 60
    val fDays = if (days > 0) " " + days + " day" + if (days > 1) "s" else "" else ""
    val fHours = if (hours > 0) " " + hours + " hour" + if (hours > 1) "s" else "" else ""
    val fMinutes = if (minutes > 0) " " + minutes + " minute" + if (minutes > 1) "s" else "" else ""
    val fSeconds = if (seconds > 0) " " + seconds + " second" + if (seconds > 1) "s" else "" else ""
    return (fDays + fHours + fMinutes + fSeconds).trim { it <= ' ' }
}

fun formatIntoAbbreviatedString(secs: Int): String {
    if (secs == 0) {
        return "0s"
    }

    val remainder = secs % 86400
    val days = secs / 86400
    val hours = remainder / 3600
    val minutes = remainder / 60 - hours * 60
    val seconds = remainder % 3600 - minutes * 60

    val fDays = if (days > 0) {
        " " + days + "d"
    } else {
        ""
    }

    val fHours = if (hours > 0) {
        " " + hours + "h"
    } else {
        ""
    }

    val fMinutes = if (minutes > 0) {
        " " + minutes + "m"
    } else {
        ""
    }

    val fSeconds = if (seconds > 0) {
        " " + seconds + "s"
    } else {
        ""
    }

    return (fDays + fHours + fMinutes + fSeconds).trim { it <= ' ' }
}
