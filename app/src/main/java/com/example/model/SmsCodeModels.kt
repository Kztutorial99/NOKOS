package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class NokosStatus {
    WAITING_SMS,
    RECEIVED_OTP,
    COMPLETED,
    CANCELED,
    EXPIRED
}

data class ActiveNokos(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val countryId: String,
    val countryName: String,
    val flagEmoji: String,
    val phoneNumber: String,
    val status: NokosStatus,
    val otpCode: String? = null,
    val fullSms: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (15 * 60 * 1000), // 15 mins default
    val price: Double = 0.0,
    val isDemo: Boolean = false,
    val lastCheckedAt: Long = System.currentTimeMillis()
) {
    val remainingSeconds: Long
        get() {
            val diff = (expiresAt - System.currentTimeMillis()) / 1000
            return if (diff > 0) diff else 0
        }

    val isWaiting: Boolean
        get() = status == NokosStatus.WAITING_SMS && remainingSeconds > 0
}

data class ServiceProduct(
    val id: String,
    val name: String,
    val price: Double,
    val currency: String = "USD",
    val stockCount: Int = 999,
    val category: String = "Popular",
    val iconEmoji: String = "💬"
)

data class CountryItem(
    val id: String,
    val name: String,
    val phonePrefix: String,
    val flagEmoji: String
)

data class EncryptedVaultRecord(
    val recordId: String,
    val serviceName: String,
    val countryName: String,
    val encryptedPhone: String,
    val ivPhone: String,
    val encryptedOtp: String?,
    val ivOtp: String?,
    val encryptedSms: String?,
    val ivSms: String?,
    val timestamp: Long,
    val status: String,
    val isDemo: Boolean
)

// Network DTOs for SMSCode.gg API
@JsonClass(generateAdapter = true)
data class BalanceDto(
    @field:Json(name = "balance") val balance: Double? = 0.0,
    @field:Json(name = "currency") val currency: String? = "USD"
)

@JsonClass(generateAdapter = true)
data class CountryDto(
    @field:Json(name = "id") val id: Any?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "code") val code: String? = null
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @field:Json(name = "id") val id: Any?,
    @field:Json(name = "platform_id") val platformId: String? = null,
    @field:Json(name = "platform_name") val platformName: String? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "price") val price: Double? = null,
    @field:Json(name = "available_count") val availableCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class CreateOrderBody(
    @field:Json(name = "country_id") val countryId: String?,
    @field:Json(name = "platform_id") val platformId: String?
)

@JsonClass(generateAdapter = true)
data class OrderResponseDto(
    @field:Json(name = "id") val id: Any?,
    @field:Json(name = "phone_number") val phoneNumber: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "otp_code") val otpCode: String? = null,
    @field:Json(name = "sms_text") val smsText: String? = null,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "expires_at") val expiresAt: String? = null,
    @field:Json(name = "message") val message: String? = null
)
