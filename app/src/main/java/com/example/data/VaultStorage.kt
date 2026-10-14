package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.CryptoManager
import com.example.model.ActiveNokos
import com.example.model.CountryItem
import com.example.model.EncryptedVaultRecord
import com.example.model.NokosStatus
import com.example.model.ServiceProduct
import com.squareup.moshi.Types
import org.json.JSONArray
import org.json.JSONObject

class VaultStorage(
    private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nokos_vault_secure_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENCRYPTED_API_TOKEN = "enc_api_token"
        private const val KEY_IV_API_TOKEN = "iv_api_token"
        private const val KEY_IS_DEMO_MODE = "is_demo_mode"
        private const val KEY_VAULT_PIN = "vault_security_pin"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_ENCRYPTED_VAULT_RECORDS = "enc_vault_records_json"
        private const val KEY_LAST_BALANCE = "last_balance_cached"
        private const val KEY_LAST_CURRENCY = "last_currency_cached"
    }

    // Default Popular Services for instant ordering & fallback catalog
    val defaultServices = listOf(
        ServiceProduct("wa", "WhatsApp", 0.35, "USD", 480, "Messaging", "🟢"),
        ServiceProduct("tg", "Telegram", 0.25, "USD", 950, "Messaging", "✈️"),
        ServiceProduct("go", "Google / Gmail", 0.30, "USD", 620, "Social", "🔍"),
        ServiceProduct("tt", "TikTok", 0.20, "USD", 1240, "Social", "🎵"),
        ServiceProduct("sp", "Shopee", 0.40, "USD", 310, "E-Commerce", "🛒"),
        ServiceProduct("ig", "Instagram", 0.28, "USD", 870, "Social", "📸"),
        ServiceProduct("oa", "OpenAI / ChatGPT", 0.45, "USD", 290, "AI & Tech", "🤖"),
        ServiceProduct("gj", "Gojek", 0.38, "USD", 150, "Transport", "🛵"),
        ServiceProduct("dn", "DANA", 0.42, "USD", 180, "Fintech", "💳"),
        ServiceProduct("tw", "Twitter / X", 0.22, "USD", 740, "Social", "𝕏"),
        ServiceProduct("ds", "Discord", 0.18, "USD", 1100, "Gaming", "🎮"),
        ServiceProduct("gb", "Grab", 0.35, "USD", 210, "Transport", "🚗")
    )

    val defaultCountries = listOf(
        CountryItem("id", "Indonesia", "+62", "🇮🇩"),
        CountryItem("us", "United States", "+1", "🇺🇸"),
        CountryItem("my", "Malaysia", "+60", "🇲🇾"),
        CountryItem("vn", "Vietnam", "+84", "🇻🇳"),
        CountryItem("ph", "Philippines", "+63", "🇵🇭"),
        CountryItem("th", "Thailand", "+66", "🇹🇭"),
        CountryItem("gb", "United Kingdom", "+44", "🇬🇧"),
        CountryItem("ru", "Russia", "+7", "🇷🇺"),
        CountryItem("br", "Brazil", "+55", "🇧🇷"),
        CountryItem("in", "India", "+91", "🇮🇳")
    )

    fun saveApiToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            prefs.edit()
                .remove(KEY_ENCRYPTED_API_TOKEN)
                .remove(KEY_IV_API_TOKEN)
                .apply()
            return
        }
        val encrypted = cryptoManager.encrypt(trimmed)
        prefs.edit()
            .putString(KEY_ENCRYPTED_API_TOKEN, encrypted.cipherTextBase64)
            .putString(KEY_IV_API_TOKEN, encrypted.ivBase64)
            .apply()
    }

    fun getApiToken(): String {
        val cipher = prefs.getString(KEY_ENCRYPTED_API_TOKEN, null)
        val iv = prefs.getString(KEY_IV_API_TOKEN, null)
        if (cipher != null && iv != null) {
            val decrypted = cryptoManager.decrypt(iv, cipher)
            if (decrypted.isNotEmpty() && !decrypted.startsWith("[")) {
                return decrypted
            }
        }
        return ""
    }

    fun isDemoMode(): Boolean {
        // Defaults to true if no token is saved, so user can immediately experiment
        val token = getApiToken()
        return prefs.getBoolean(KEY_IS_DEMO_MODE, token.isEmpty())
    }

    fun setDemoMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DEMO_MODE, enabled).apply()
    }

    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun getVaultPin(): String {
        return prefs.getString(KEY_VAULT_PIN, "") ?: ""
    }

    fun setVaultPin(pin: String) {
        prefs.edit().putString(KEY_VAULT_PIN, pin.trim()).apply()
    }

    fun saveLastBalance(balance: Double, currency: String) {
        prefs.edit()
            .putFloat(KEY_LAST_BALANCE, balance.toFloat())
            .putString(KEY_LAST_CURRENCY, currency)
            .apply()
    }

    fun getLastBalance(): Pair<Double, String> {
        val balance = prefs.getFloat(KEY_LAST_BALANCE, 0f).toDouble()
        val curr = prefs.getString(KEY_LAST_CURRENCY, "USD") ?: "USD"
        return Pair(balance, curr)
    }

    // Securely archive an order into the encrypted vault
    fun archiveEncryptedRecord(nokos: ActiveNokos) {
        val encPhone = cryptoManager.encrypt(nokos.phoneNumber)
        val encOtp = nokos.otpCode?.let { cryptoManager.encrypt(it) }
        val encSms = nokos.fullSms?.let { cryptoManager.encrypt(it) }

        val newRecord = EncryptedVaultRecord(
            recordId = nokos.id,
            serviceName = nokos.serviceName,
            countryName = nokos.countryName,
            encryptedPhone = encPhone.cipherTextBase64,
            ivPhone = encPhone.ivBase64,
            encryptedOtp = encOtp?.cipherTextBase64,
            ivOtp = encOtp?.ivBase64,
            encryptedSms = encSms?.cipherTextBase64,
            ivSms = encSms?.ivBase64,
            timestamp = System.currentTimeMillis(),
            status = nokos.status.name,
            isDemo = nokos.isDemo
        )

        val currentList = getEncryptedVaultRecords().toMutableList()
        // Replace if exists, else append to front
        val existingIndex = currentList.indexOfFirst { it.recordId == nokos.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = newRecord
        } else {
            currentList.add(0, newRecord)
        }
        saveVaultRecords(currentList)
    }

    fun getEncryptedVaultRecords(): List<EncryptedVaultRecord> {
        val rawJson = prefs.getString(KEY_ENCRYPTED_VAULT_RECORDS, null) ?: return emptyList()
        val list = mutableListOf<EncryptedVaultRecord>()
        try {
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    EncryptedVaultRecord(
                        recordId = obj.optString("recordId"),
                        serviceName = obj.optString("serviceName"),
                        countryName = obj.optString("countryName"),
                        encryptedPhone = obj.optString("encryptedPhone"),
                        ivPhone = obj.optString("ivPhone"),
                        encryptedOtp = if (obj.has("encryptedOtp")) obj.optString("encryptedOtp") else null,
                        ivOtp = if (obj.has("ivOtp")) obj.optString("ivOtp") else null,
                        encryptedSms = if (obj.has("encryptedSms")) obj.optString("encryptedSms") else null,
                        ivSms = if (obj.has("ivSms")) obj.optString("ivSms") else null,
                        timestamp = obj.optLong("timestamp"),
                        status = obj.optString("status"),
                        isDemo = obj.optBoolean("isDemo", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveVaultRecords(records: List<EncryptedVaultRecord>) {
        val array = JSONArray()
        records.take(100).forEach { r ->
            val obj = JSONObject().apply {
                put("recordId", r.recordId)
                put("serviceName", r.serviceName)
                put("countryName", r.countryName)
                put("encryptedPhone", r.encryptedPhone)
                put("ivPhone", r.ivPhone)
                put("encryptedOtp", r.encryptedOtp)
                put("ivOtp", r.ivOtp)
                put("encryptedSms", r.encryptedSms)
                put("ivSms", r.ivSms)
                put("timestamp", r.timestamp)
                put("status", r.status)
                put("isDemo", r.isDemo)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ENCRYPTED_VAULT_RECORDS, array.toString()).apply()
    }

    fun deleteVaultRecord(recordId: String) {
        val current = getEncryptedVaultRecords().filter { it.recordId != recordId }
        saveVaultRecords(current)
    }

    fun shredAllData() {
        prefs.edit().clear().apply()
    }
}
