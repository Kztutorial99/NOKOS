package com.example.util

import java.util.regex.Pattern

object OtpExtractor {

    // Regex patterns for various OTP message structures
    private val PATTERNS = listOf(
        // "kode verifikasi anda adalah 123456" or "OTP: 123456"
        Pattern.compile("""(?i)(?:kode|code|otp|pin|verifikasi|verification)[\s:]*([0-9]{4,8})\b"""),
        // Hyphenated codes like "123-456" or "123 456"
        Pattern.compile("""\b([0-9]{3})[- ]([0-9]{3})\b"""),
        // Prefixed codes like "G-123456" (Google)
        Pattern.compile("""(?i)\b([A-Z]-[0-9]{4,8})\b"""),
        // Generic 4 to 8 digit numbers surrounded by non-digits
        Pattern.compile("""\b([0-9]{4,8})\b""")
    )

    fun extractOtp(smsText: String?): String? {
        if (smsText.isNullOrBlank()) return null
        val cleanText = smsText.trim()

        for (pattern in PATTERNS) {
            val matcher = pattern.matcher(cleanText)
            if (matcher.find()) {
                // If hyphenated like 123-456, groupCount == 2
                if (matcher.groupCount() == 2 && matcher.group(1) != null && matcher.group(2) != null) {
                    return "${matcher.group(1)}${matcher.group(2)}"
                }
                val code = matcher.group(1) ?: matcher.group(0)
                if (!code.isNullOrBlank()) {
                    return code.replace(" ", "").trim()
                }
            }
        }
        return null
    }

    fun maskPhoneNumber(phone: String): String {
        if (phone.length <= 6) return phone
        val prefix = phone.take(4)
        val suffix = phone.takeLast(3)
        val masked = "*".repeat((phone.length - 7).coerceAtLeast(2))
        return "$prefix$masked$suffix"
    }

    fun maskToken(token: String): String {
        if (token.isBlank()) return "Belum diatur"
        if (token.length <= 8) return "••••••••"
        return "${token.take(4)}••••••••${token.takeLast(4)}"
    }
}
