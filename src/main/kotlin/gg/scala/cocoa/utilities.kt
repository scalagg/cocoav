package gg.scala.cocoa

/**
 * @author GrowlyX
 * @since 12/23/2021
 */
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
