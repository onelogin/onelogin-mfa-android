package com.onelogin.mfa.data.otp

import com.onelogin.mfa.data.util.TimeProvider
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Hex
import java.lang.reflect.UndeclaredThrowableException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

class TotpToken @JvmOverloads constructor(
    private val seed: String?,
    private val period: Int = 30,
    private val digits: Int = 6,
    private val crypto: String = "HmacSHA1",
    private val t0: Int = 0,
    val getTime: () -> Long = TimeProvider()
) {

    val periodInMillis = period * 1000L

    fun generateOtp(): String {
        val time = getTime()
        return generateOtp(time)
    }

    private fun generateOtp(timestamp: Long): String {
        val time = (timestamp / 1000) / period
        val base32 = Base32()
        val decodedSeed = base32.decode(seed)
        val key = byteArrayToHexString(decodedSeed)

        if (decodedSeed.isEmpty() || key.isBlank()) {
            return ""
        }
        val msg = ByteBuffer.allocate(8).putLong(time).array()
        val hash = hmacSha(crypto, Hex.decodeHex(key.toCharArray()), msg)

        val offset: Int = hash[hash.size - 1].toInt() and 0xf

        val binary = ((hash[offset].toInt() and 0x7f).toLong() shl 24) or
                ((hash[offset + 1].toInt() and 0xff).toLong() shl 16) or
                ((hash[offset + 2].toInt() and 0xff).toLong() shl 8) or
                ((hash[offset + 3].toInt() and 0xff).toLong())

        return (binary % 10.0.pow(digits.toDouble())).toInt().toString().padStart(digits, '0')
    }

    fun getTimer(): Int {
        val time = getTime() / 1000
        return (period - ((time - t0) % period)).toInt()
    }

    fun getTimerInMillis(): Long {
        val time = getTime()
        val periodInMillis = period * 1000
        return periodInMillis - ((time - t0) % periodInMillis)
    }

    private fun byteArrayToHexString(digest: ByteArray): String {

        val buffer = StringBuffer()

        for (i in digest.indices) {
            val hex = Integer.toHexString(0xff and digest[i].toInt())

            if (hex.length == 1)
                buffer.append("0")

            buffer.append(hex)

        }

        return buffer.toString()
    }

    private fun hmacSha(crypto: String, keyBytes: ByteArray, text: ByteArray): ByteArray {
        try {
            val supportedCrypto = getSupportedCrypto(crypto)
            val hmac: Mac = Mac.getInstance(supportedCrypto)
            val macKey = SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            return hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }
    }

    private fun getSupportedCrypto(crypto: String): String {
        if (listOf("hmacsha1", "hmacsha256", "hmacsha512").contains(crypto.toLowerCase(Locale.ROOT)))
            return crypto

        return when (crypto.toLowerCase(Locale.ROOT)) {
            "sha1" -> "HmacSHA1"
            "sha256" -> "HmacSHA256"
            "sha512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }
    }
}
