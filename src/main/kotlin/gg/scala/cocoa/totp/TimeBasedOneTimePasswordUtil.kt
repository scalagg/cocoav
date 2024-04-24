package gg.scala.cocoa.totp

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TimeBasedOneTimePasswordUtil
{
    const val DEFAULT_TIME_STEP_SECONDS = 30
    private const val NUM_DIGITS_OUTPUT = 6
    private var blockOfZeros: String? = null

    @JvmOverloads
    fun generateBase32Secret(length: Int = 16): String
    {
        val sb = StringBuilder(length)
        val random: Random = SecureRandom()
        for (i in 0 until length)
        {
            val `val` = random.nextInt(32)
            if (`val` < 26)
            {
                sb.append((65 + `val`).toChar())
            } else
            {
                sb.append((50 + (`val` - 26)).toChar())
            }
        }
        return sb.toString()
    }

    @JvmOverloads
    @Throws(GeneralSecurityException::class)
    fun validateCurrentNumber(
        base32Secret: String,
        authNumber: Int,
        windowMillis: Int,
        timeMillis: Long = System.currentTimeMillis(),
        timeStepSeconds: Int = 30
    ): Boolean
    {
        var from = timeMillis
        var to = timeMillis
        if (windowMillis > 0)
        {
            from = timeMillis - windowMillis.toLong()
            to = timeMillis + windowMillis.toLong()
        }
        val timeStepMillis = (timeStepSeconds * 1000).toLong()
        var millis = from
        while (millis <= to)
        {
            val compare = generateNumber(base32Secret, millis, timeStepSeconds)
            if (compare == authNumber.toLong())
            {
                return true
            }
            millis += timeStepMillis
        }
        return false
    }

    @Throws(GeneralSecurityException::class)
    fun generateCurrentNumberString(base32Secret: String): String
    {
        return generateNumberString(base32Secret, System.currentTimeMillis(), 30)
    }

    @Throws(GeneralSecurityException::class)
    fun generateNumberString(base32Secret: String, timeMillis: Long, timeStepSeconds: Int): String
    {
        val number = generateNumber(base32Secret, timeMillis, timeStepSeconds)
        return zeroPrepend(number, NUM_DIGITS_OUTPUT)
    }

    @Throws(GeneralSecurityException::class)
    fun generateCurrentNumber(base32Secret: String): Long
    {
        return generateNumber(base32Secret, System.currentTimeMillis(), 30)
    }

    @Throws(GeneralSecurityException::class)
    fun generateNumber(base32Secret: String, timeMillis: Long, timeStepSeconds: Int): Long
    {
        val key = decodeBase32(base32Secret)
        val data = ByteArray(8)
        var value = timeMillis / 1000L / timeStepSeconds.toLong()
        run {
            var i = 7
            while (value > 0L)
            {
                data[i] = (value and 255L).toInt().toByte()
                value = value shr 8
                --i
            }
        }
        val signKey = SecretKeySpec(key, "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(signKey)
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 15
        var truncatedHash = 0L
        for (i in offset until offset + 4)
        {
            truncatedHash = truncatedHash shl 8
            truncatedHash = truncatedHash or (hash[i].toInt() and 255).toLong()
        }
        truncatedHash = truncatedHash and 2147483647L
        truncatedHash %= 1000000L
        return truncatedHash
    }

    fun qrImageUrl(keyId: String, secret: String, username: String): String
    {
        val sb = StringBuilder(128)
        sb.append("https://quickchart.io/qr?text=${
            addOtpAuthPart(keyId, username, secret, sb)
        }&dark=ff7300&size=500")
        return sb.toString()
    }

    fun generateOtpAuthUrl(keyId: String, secret: String, username: String): String
    {
        val sb = StringBuilder(64)
        addOtpAuthPart(keyId, username, secret, sb)
        return sb.toString()
    }

    private fun addOtpAuthPart(keyId: String, username: String?, secret: String, sb: StringBuilder) = "otpauth://totp/$keyId:$username?secret=$secret&issuer=$keyId"

    fun zeroPrepend(num: Long, digits: Int): String
    {
        val numStr = java.lang.Long.toString(num)
        return if (numStr.length >= digits)
        {
            numStr
        } else
        {
            val sb = StringBuilder(digits)
            val zeroCount = digits - numStr.length
            sb.append(blockOfZeros, 0, zeroCount)
            sb.append(numStr)
            sb.toString()
        }
    }

    fun decodeBase32(str: String): ByteArray
    {
        val numBytes = (str.length * 5 + 7) / 8
        var result = ByteArray(numBytes)
        var resultIndex = 0
        var which = 0
        var working = 0
        for (i in 0 until str.length)
        {
            val ch = str[i]
            var `val`: Int
            if (ch >= 'a' && ch <= 'z')
            {
                `val` = ch.code - 97
            } else if (ch >= 'A' && ch <= 'Z')
            {
                `val` = ch.code - 65
            } else
            {
                if (ch < '2' || ch > '7')
                {
                    require(ch == '=') { "Invalid base-32 character: $ch" }
                    which = 0
                    break
                }
                `val` = 26 + (ch.code - 50)
            }
            when (which)
            {
                0 ->
                {
                    working = `val` and 31 shl 3
                    which = 1
                }

                1 ->
                {
                    working = working or (`val` and 28 shr 2)
                    result[resultIndex++] = working.toByte()
                    working = `val` and 3 shl 6
                    which = 2
                }

                2 ->
                {
                    working = working or (`val` and 31 shl 1)
                    which = 3
                }

                3 ->
                {
                    working = working or (`val` and 16 shr 4)
                    result[resultIndex++] = working.toByte()
                    working = `val` and 15 shl 4
                    which = 4
                }

                4 ->
                {
                    working = working or (`val` and 30 shr 1)
                    result[resultIndex++] = working.toByte()
                    working = `val` and 1 shl 7
                    which = 5
                }

                5 ->
                {
                    working = working or (`val` and 31 shl 2)
                    which = 6
                }

                6 ->
                {
                    working = working or (`val` and 24 shr 3)
                    result[resultIndex++] = working.toByte()
                    working = `val` and 7 shl 5
                    which = 7
                }

                7 ->
                {
                    working = working or (`val` and 31)
                    result[resultIndex++] = working.toByte()
                    which = 0
                }
            }
        }
        if (which != 0)
        {
            result[resultIndex++] = working.toByte()
        }
        if (resultIndex != result.size)
        {
            result = Arrays.copyOf(result, resultIndex)
        }
        return result
    }

    init
    {
        val chars = CharArray(NUM_DIGITS_OUTPUT)

        for (i in chars.indices)
        {
            chars[i] = '0'
        }

        blockOfZeros = String(chars)
    }
}
