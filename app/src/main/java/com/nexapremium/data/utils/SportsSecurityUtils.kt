package com.nexapremium.data.utils

import android.util.Base64
import java.net.URI
import java.security.MessageDigest

object SportsSecurityUtils {
    var activeSalt: String = "9HY(#b1q6"

    private val alphabet = "fFgGjJkKlLaApPbBmMoOzZeEnNcCdDrRqQtTvVuUxXhHiIwWyYsS"
    private val standard = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ"

    fun decodeObfuscatedString(input: String): String {
        return try {
            val rotated = rotateCharacters(input)
            val decodedBytes = Base64.decode(rotated, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            input
        }
    }

    fun decodeBase64(input: String): String {
        return try {
            val decodedBytes = Base64.decode(input, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun rotateCharacters(input: String): String {
        val result = StringBuilder()
        for (char in input) {
            val index = alphabet.indexOf(char)
            if (index != -1 && index < standard.length) {
                result.append(standard[index])
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }

    fun getDomainSegment(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return "unknown"
            host.substringBefore(".")
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun generateToken(domainPart: String): String {
        val timestamp = System.currentTimeMillis() / 1000
        val expiry = timestamp + 3600

        val input = "$domainPart$activeSalt$timestamp$expiry"
        val hash = sha256(input)

        return "?token=$hash-$expiry-$timestamp"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
