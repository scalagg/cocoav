package gg.scala.cocoa

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.*
import java.util.regex.Pattern

/**
 * @author joeleoli
 * @since 12/23/2021
 */
private val timePattern = Pattern.compile(
    "(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?(?:([0-9]+)\\s*(?:s[a-z]*)?)?",
    2
)

fun String.centered(pixels: Double): String
{
    return center(this, pixels)
}

val serializer by lazy {
    LegacyComponentSerializer
        .legacyAmpersand()
        .toBuilder()
        .extractUrls()
        .build()
}

fun String.legacy() = serializer
    .deserialize(this)
    .asComponent()

private fun center(message: String, centerPixels: Double): String
{
    var messagePxSize = 0

    var previousCode = false
    var isBold = false

    for (c in message.toCharArray())
    {
        if (c == '§')
        {
            previousCode = true
        } else if (previousCode)
        {
            previousCode = false
            isBold = c == 'l' || c == 'L'
        } else
        {
            val dFI = FontInformation.getDefaultFontInfo(c)

            messagePxSize += if (isBold) dFI.getBoldLength() else dFI.length
            messagePxSize++
        }
    }

    val halvedMessageSize = messagePxSize / 2
    val toCompensate = centerPixels - halvedMessageSize
    val spaceLength = FontInformation.SPACE.length + 1
    var compensated = 0

    var whitespaces = ""

    while (compensated < toCompensate)
    {
        whitespaces += " "
        compensated += spaceLength
    }

    return "$whitespaces$message"
}

fun parseDuration(time: String): Long
{
    val matcher = timePattern.matcher(time)

    var years = 0
    var months = 0
    var weeks = 0
    var days = 0
    var hours = 0
    var minutes = 0
    var seconds = 0
    var found = false

    while (matcher.find())
    {
        if (matcher.group() != null && matcher.group().isNotEmpty())
        {
            for (c in 0 until matcher.groupCount())
            {
                if (matcher.group(c) != null && matcher.group(c).isNotEmpty())
                {
                    found = true
                    break
                }
            }

            if (found)
            {
                if (matcher.group(1) != null && matcher.group(1).isNotEmpty())
                {
                    years = Integer.parseInt(matcher.group(1))
                }

                if (matcher.group(2) != null && matcher.group(2).isNotEmpty())
                {
                    months = Integer.parseInt(matcher.group(2))
                }

                if (matcher.group(3) != null && matcher.group(3).isNotEmpty())
                {
                    weeks = Integer.parseInt(matcher.group(3))
                }

                if (matcher.group(4) != null && matcher.group(4).isNotEmpty())
                {
                    days = Integer.parseInt(matcher.group(4))
                }

                if (matcher.group(5) != null && matcher.group(5).isNotEmpty())
                {
                    hours = Integer.parseInt(matcher.group(5))
                }

                if (matcher.group(6) != null && matcher.group(6).isNotEmpty())
                {
                    minutes = Integer.parseInt(matcher.group(6))
                }

                if (matcher.group(7) != null && matcher.group(7).isNotEmpty())
                {
                    seconds = Integer.parseInt(matcher.group(7))
                }
                break
            }
        }
    }

    if (!found)
    {
        throw Exception("Illegal Date")
    } else
    {
        val calendar = GregorianCalendar()
        val start = calendar.timeInMillis

        if (years > 0)
        {
            calendar.add(1, years)
        }

        if (months > 0)
        {
            calendar.add(2, months)
        }

        if (weeks > 0)
        {
            calendar.add(3, weeks)
        }

        if (days > 0)
        {
            calendar.add(5, days)
        }

        if (hours > 0)
        {
            calendar.add(11, hours)
        }

        if (minutes > 0)
        {
            calendar.add(12, minutes)
        }

        if (seconds > 0)
        {
            calendar.add(13, seconds)
        }

        val max = GregorianCalendar()
        max.add(1, 2)

        return if (calendar.after(max))
        {
            max.timeInMillis - start
        } else
        {
            calendar.timeInMillis - start
        }
    }
}

fun formatIntoDetailedString(secs: Int): String
{
    if (secs == 0)
    {
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

fun formatIntoAbbreviatedString(secs: Int): String
{
    if (secs == 0)
    {
        return "0s"
    }

    val remainder = secs % 86400
    val days = secs / 86400
    val hours = remainder / 3600
    val minutes = remainder / 60 - hours * 60
    val seconds = remainder % 3600 - minutes * 60

    val fDays = if (days > 0)
    {
        " " + days + "d"
    } else
    {
        ""
    }

    val fHours = if (hours > 0)
    {
        " " + hours + "h"
    } else
    {
        ""
    }

    val fMinutes = if (minutes > 0)
    {
        " " + minutes + "m"
    } else
    {
        ""
    }

    val fSeconds = if (seconds > 0)
    {
        " " + seconds + "s"
    } else
    {
        ""
    }

    return (fDays + fHours + fMinutes + fSeconds).trim { it <= ' ' }
}

enum class FontInformation(val character: Char, val length: Int)
{
    A('A', 5), a('a', 5), B('B', 5), b('b', 5), C('C', 5), c('c', 5), D('D', 5), d('d', 5), E('E', 5), e('e', 5), F(
    'F',
    5
),
    f('f', 4), G('G', 5), g('g', 5), H('H', 5), h('h', 5), I('I', 3), i('i', 1), J('J', 5), j('j', 5), K('K', 5), k(
    'k',
    4
),
    L('L', 5), l('l', 1), M('M', 5), m('m', 5), N('N', 5), n('n', 5), O('O', 5), o('o', 5), P('P', 5), p('p', 5), Q(
    'Q',
    5
),
    q('q', 5), R('R', 5), r('r', 5), S('S', 5), s('s', 5), T('T', 5), t('t', 4), U('U', 5), u('u', 5), V('V', 5), v(
    'v',
    5
),
    W('W', 5), w('w', 5), X('X', 5), x('x', 5), Y('Y', 5), y('y', 5), Z('Z', 5), z('z', 5), NUM_1('1', 5), NUM_2(
    '2',
    5
),
    NUM_3('3', 5), NUM_4('4', 5), NUM_5('5', 5), NUM_6('6', 5), NUM_7('7', 5), NUM_8('8', 5), NUM_9('9', 5), NUM_0(
    '0',
    5
),
    EXCLAMATION_POINT('!', 1), AT_SYMBOL('@', 6), NUM_SIGN('#', 5), DOLLAR_SIGN('$', 5), PERCENT('%', 5), UP_ARROW(
    '^',
    5
),
    AMPERSAND('&', 5), ASTERISK('*', 5), LEFT_PARENTHESIS('(', 4), RIGHT_PERENTHESIS(')', 4), MINUS('-', 5), UNDERSCORE(
    '_',
    5
),
    PLUS_SIGN('+', 5), EQUALS_SIGN('=', 5), LEFT_CURL_BRACE('{', 4), RIGHT_CURL_BRACE('}', 4), LEFT_BRACKET(
    '[',
    3
),
    RIGHT_BRACKET(']', 3), COLON(':', 1), SEMI_COLON(';', 1), DOUBLE_QUOTE('"', 3), SINGLE_QUOTE('\'', 1), LEFT_ARROW(
    '<',
    4
),
    RIGHT_ARROW('>', 4), QUESTION_MARK('?', 5), SLASH('/', 5), BACK_SLASH('\\', 5), LINE('|', 1), TILDE('~', 5), TICK(
    '`',
    2
),
    PERIOD('.', 1), COMMA(',', 1), SPACE(' ', 3), DEFAULT('a', 4);

    fun getBoldLength(): Int
    {
        return if (this == SPACE) length else length + 1
    }

    companion object
    {
        fun getDefaultFontInfo(c: Char): FontInformation
        {
            for (dFI in values())
            {
                if (dFI.character == c) return dFI
            }

            return DEFAULT
        }
    }
}
