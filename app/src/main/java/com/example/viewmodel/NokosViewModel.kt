package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoManager
import com.example.data.VaultStorage
import com.example.model.ActiveNokos
import com.example.model.CountryItem
import com.example.model.CreateOrderBody
import com.example.model.NokosStatus
import com.example.model.ServiceProduct
import com.example.network.ApiClient
import com.example.util.OtpExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class DecryptedVaultItem(
    val recordId: String,
    val serviceName: String,
    val countryName: String,
    val phoneNumber: String,
    val otpCode: String?,
    val fullSms: String?,
    val timestamp: Long,
    val status: String,
    val isDemo: Boolean
)

data class NokosUiState(
    val balance: Double = 0.0,
    val currency: String = "USD",
    val isLoadingBalance: Boolean = false,
    val maskedToken: String = "",
    val isDemoMode: Boolean = true,
    val activeOrders: List<ActiveNokos> = emptyList(),
    val countries: List<CountryItem> = emptyList(),
    val selectedCountry: CountryItem? = null,
    val catalogServices: List<ServiceProduct> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Semua",
    val isOrdering: Boolean = false,
    val isPolling: Boolean = false,
    val lastPollTime: Long = 0L,
    val toastMessage: String? = null,
    val isToastError: Boolean = false,
    val isVaultUnlocked: Boolean = true, // Open by default unless PIN is set
    val hasVaultPin: Boolean = false,
    val decryptedVaultRecords: List<DecryptedVaultItem> = emptyList(),
    val keyFingerprint: String = "AES-256-GCM Hardware-Backed",
    val hapticEnabled: Boolean = true
)

class NokosViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = CryptoManager()
    private val vaultStorage = VaultStorage(application, cryptoManager)

    private val _uiState = MutableStateFlow(NokosUiState())
    val uiState: StateFlow<NokosUiState> = _uiState.asStateFlow()

    private var pollerJob: Job? = null

    init {
        loadInitialData()
        startRealtimePoller()
    }

    private fun loadInitialData() {
        val token = vaultStorage.getApiToken()
        val isDemo = vaultStorage.isDemoMode()
        val (cachedBalance, cachedCurr) = vaultStorage.getLastBalance()
        val pin = vaultStorage.getVaultPin()
        val haptic = vaultStorage.isHapticEnabled()

        _uiState.update {
            it.copy(
                maskedToken = OtpExtractor.maskToken(token),
                isDemoMode = isDemo,
                balance = if (isDemo && cachedBalance == 0.0) 25.0 else cachedBalance,
                currency = cachedCurr,
                countries = vaultStorage.defaultCountries,
                selectedCountry = vaultStorage.defaultCountries.first(),
                catalogServices = vaultStorage.defaultServices,
                hasVaultPin = pin.isNotEmpty(),
                isVaultUnlocked = pin.isEmpty(),
                keyFingerprint = cryptoManager.getKeyFingerprint(),
                hapticEnabled = haptic
            )
        }

        refreshVaultRecords()

        if (!isDemo && token.isNotEmpty()) {
            refreshBalance()
            fetchCatalog()
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        vaultStorage.setHapticEnabled(enabled)
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun selectCountry(country: CountryItem) {
        _uiState.update { it.copy(selectedCountry = country) }
        if (!_uiState.value.isDemoMode && vaultStorage.getApiToken().isNotEmpty()) {
            fetchCatalog()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedCategory(cat: String) {
        _uiState.update { it.copy(selectedCategory = cat) }
    }

    fun setDemoMode(isDemo: Boolean) {
        vaultStorage.setDemoMode(isDemo)
        _uiState.update {
            it.copy(
                isDemoMode = isDemo,
                balance = if (isDemo && it.balance <= 0.0) 25.0 else it.balance,
                toastMessage = if (isDemo) "Mode Sandbox / Simulasi Aktif" else "Mode Live SMSCode.gg Aktif",
                isToastError = false
            )
        }
        if (!isDemo) {
            refreshBalance()
            fetchCatalog()
        }
    }

    fun saveApiToken(newToken: String) {
        val trimmed = newToken.trim()
        vaultStorage.saveApiToken(trimmed)
        val masked = OtpExtractor.maskToken(trimmed)

        _uiState.update {
            it.copy(
                maskedToken = masked,
                isDemoMode = trimmed.isEmpty(),
                toastMessage = if (trimmed.isNotEmpty()) "API Token tersimpan di Encrypted Vault" else "API Token dihapus",
                isToastError = false
            )
        }
        if (trimmed.isNotEmpty()) {
            vaultStorage.setDemoMode(false)
            refreshBalance()
            fetchCatalog()
        }
    }

    fun testTokenConnection(token: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            onResult(false, "Token tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.smsCodeApi.getBalance("Bearer $trimmed")
                if (response.isSuccessful) {
                    val body = response.body()
                    val bal = body?.balance ?: 0.0
                    val curr = body?.currency ?: "USD"
                    onResult(true, "Koneksi Berhasil! Saldo: $bal $curr")
                } else {
                    val code = response.code()
                    onResult(false, "Gagal terhubung (HTTP $code): Token tidak valid atau server bermasalah")
                }
            } catch (e: Exception) {
                onResult(false, "Kesalahan jaringan: ${e.localizedMessage ?: "Tidak dapat menjangkau server"}")
            }
        }
    }

    fun refreshBalance() {
        val token = vaultStorage.getApiToken()
        if (token.isEmpty() || _uiState.value.isDemoMode) {
            if (_uiState.value.isDemoMode) {
                _uiState.update { it.copy(balance = if (it.balance <= 0) 25.0 else it.balance) }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBalance = true) }
            try {
                val response = ApiClient.smsCodeApi.getBalance("Bearer $token")
                if (response.isSuccessful) {
                    val body = response.body()
                    val bal = body?.balance ?: 0.0
                    val curr = body?.currency ?: "USD"
                    vaultStorage.saveLastBalance(bal, curr)
                    _uiState.update {
                        it.copy(
                            balance = bal,
                            currency = curr,
                            isLoadingBalance = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingBalance = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingBalance = false) }
            }
        }
    }

    fun fetchCatalog() {
        val token = vaultStorage.getApiToken()
        if (token.isEmpty() || _uiState.value.isDemoMode) return

        val countryId = _uiState.value.selectedCountry?.id

        viewModelScope.launch {
            try {
                val response = ApiClient.smsCodeApi.getProducts("Bearer $token", countryId)
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.map { p ->
                        ServiceProduct(
                            id = (p.platformId ?: p.id).toString(),
                            name = p.platformName ?: p.name ?: "Unknown Service",
                            price = p.price ?: 0.35,
                            currency = "USD",
                            stockCount = p.availableCount ?: 100,
                            category = "Live",
                            iconEmoji = getEmojiForService(p.platformName ?: p.name ?: "")
                        )
                    }
                    if (list.isNotEmpty()) {
                        _uiState.update { it.copy(catalogServices = list) }
                    }
                }
            } catch (e: Exception) {
                // Fall back to default catalog
            }
        }
    }

    private fun getEmojiForService(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("whatsapp") -> "🟢"
            lower.contains("telegram") -> "✈️"
            lower.contains("google") || lower.contains("gmail") -> "🔍"
            lower.contains("tiktok") -> "🎵"
            lower.contains("shopee") -> "🛒"
            lower.contains("insta") -> "📸"
            lower.contains("openai") || lower.contains("gpt") -> "🤖"
            lower.contains("gojek") -> "🛵"
            lower.contains("dana") -> "💳"
            lower.contains("twitter") || lower.contains("x") -> "𝕏"
            lower.contains("discord") -> "🎮"
            lower.contains("grab") -> "🚗"
            else -> "📱"
        }
    }

    fun orderNumber(service: ServiceProduct) {
        val country = _uiState.value.selectedCountry ?: vaultStorage.defaultCountries.first()
        val isDemo = _uiState.value.isDemoMode
        val token = vaultStorage.getApiToken()

        if (!isDemo && token.isEmpty()) {
            _uiState.update {
                it.copy(
                    toastMessage = "Masukkan API Token di Pengaturan atau aktifkan Mode Sandbox!",
                    isToastError = true
                )
            }
            return
        }

        _uiState.update { it.copy(isOrdering = true) }

        viewModelScope.launch {
            if (isDemo) {
                // Generate simulated order
                delay(600) // Brief realistic animation
                val fakeId = "demo_${System.currentTimeMillis().toString().takeLast(6)}"
                val randomDigits = (10000000..99999999).random().toString()
                val fakeNumber = "${country.phonePrefix}$randomDigits"

                val newOrder = ActiveNokos(
                    id = fakeId,
                    serviceId = service.id,
                    serviceName = service.name,
                    countryId = country.id,
                    countryName = country.name,
                    flagEmoji = country.flagEmoji,
                    phoneNumber = fakeNumber,
                    status = NokosStatus.WAITING_SMS,
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (15 * 60 * 1000),
                    price = service.price,
                    isDemo = true
                )

                val newBalance = (_uiState.value.balance - service.price).coerceAtLeast(0.0)
                _uiState.update {
                    it.copy(
                        isOrdering = false,
                        balance = newBalance,
                        activeOrders = listOf(newOrder) + it.activeOrders,
                        toastMessage = "Nomor ${country.name} untuk ${service.name} aktif! Menunggu SMS...",
                        isToastError = false
                    )
                }

                // Auto archive in vault
                vaultStorage.archiveEncryptedRecord(newOrder)
                refreshVaultRecords()

            } else {
                // Live SMSCode.gg API request
                try {
                    val req = CreateOrderBody(countryId = country.id, platformId = service.id)
                    val response = ApiClient.smsCodeApi.createOrder("Bearer $token", req)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val orderId = body.id?.toString() ?: UUID.randomUUID().toString()
                        val phone = body.phoneNumber ?: "${country.phonePrefix}0000000"

                        val liveOrder = ActiveNokos(
                            id = orderId,
                            serviceId = service.id,
                            serviceName = service.name,
                            countryId = country.id,
                            countryName = country.name,
                            flagEmoji = country.flagEmoji,
                            phoneNumber = phone,
                            status = NokosStatus.WAITING_SMS,
                            createdAt = System.currentTimeMillis(),
                            expiresAt = System.currentTimeMillis() + (15 * 60 * 1000),
                            price = service.price,
                            isDemo = false
                        )

                        _uiState.update {
                            it.copy(
                                isOrdering = false,
                                activeOrders = listOf(liveOrder) + it.activeOrders,
                                toastMessage = "Pesanan Berhasil: $phone",
                                isToastError = false
                            )
                        }

                        vaultStorage.archiveEncryptedRecord(liveOrder)
                        refreshVaultRecords()
                        refreshBalance()

                    } else {
                        val errorText = response.errorBody()?.string() ?: "Gagal membuat pesanan (HTTP ${response.code()})"
                        _uiState.update {
                            it.copy(
                                isOrdering = false,
                                toastMessage = errorText,
                                isToastError = true
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isOrdering = false,
                            toastMessage = "Gagal menghubungi server: ${e.localizedMessage}",
                            isToastError = true
                        )
                    }
                }
            }
        }
    }

    // Cancel order & trigger refund
    fun cancelOrder(orderId: String) {
        val target = _uiState.value.activeOrders.find { it.id == orderId } ?: return
        val isDemo = target.isDemo
        val token = vaultStorage.getApiToken()

        viewModelScope.launch {
            if (isDemo) {
                val updatedOrders = _uiState.value.activeOrders.map { order ->
                    if (order.id == orderId) order.copy(status = NokosStatus.CANCELED) else order
                }
                val refundedBalance = _uiState.value.balance + target.price
                _uiState.update {
                    it.copy(
                        activeOrders = updatedOrders,
                        balance = refundedBalance,
                        toastMessage = "Nomor dibatalkan. Saldo dikembalikan: +${target.price} ${it.currency}",
                        isToastError = false
                    )
                }
                vaultStorage.archiveEncryptedRecord(target.copy(status = NokosStatus.CANCELED))
                refreshVaultRecords()
            } else {
                try {
                    ApiClient.smsCodeApi.cancelOrder("Bearer $token", orderId)
                    val updated = _uiState.value.activeOrders.map { order ->
                        if (order.id == orderId) order.copy(status = NokosStatus.CANCELED) else order
                    }
                    _uiState.update {
                        it.copy(
                            activeOrders = updated,
                            toastMessage = "Nomor berhasil dibatalkan dan direfund ke akun smscode.gg",
                            isToastError = false
                        )
                    }
                    vaultStorage.archiveEncryptedRecord(target.copy(status = NokosStatus.CANCELED))
                    refreshVaultRecords()
                    refreshBalance()
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            toastMessage = "Gagal membatalkan: ${e.localizedMessage}",
                            isToastError = true
                        )
                    }
                }
            }
        }
    }

    // Finish / complete order
    fun finishOrder(orderId: String) {
        val target = _uiState.value.activeOrders.find { it.id == orderId } ?: return
        val isDemo = target.isDemo
        val token = vaultStorage.getApiToken()

        viewModelScope.launch {
            if (!isDemo && token.isNotEmpty()) {
                try {
                    ApiClient.smsCodeApi.finishOrder("Bearer $token", orderId)
                } catch (_: Exception) {}
            }
            val updated = _uiState.value.activeOrders.map { order ->
                if (order.id == orderId) order.copy(status = NokosStatus.COMPLETED) else order
            }
            _uiState.update {
                it.copy(
                    activeOrders = updated,
                    toastMessage = "Nomor ${target.phoneNumber} ditandai selesai.",
                    isToastError = false
                )
            }
            vaultStorage.archiveEncryptedRecord(target.copy(status = NokosStatus.COMPLETED))
            refreshVaultRecords()
        }
    }

    // Force simulate incoming SMS for demonstration / testing
    fun simulateIncomingSms(orderId: String) {
        val target = _uiState.value.activeOrders.find { it.id == orderId } ?: return
        val otpNum = (100000..999999).random().toString()
        val formattedOtp = "${otpNum.take(3)}-${otpNum.takeLast(3)}"
        val sms = when (target.serviceId) {
            "wa" -> "<#> Kode verifikasi WhatsApp Anda: $formattedOtp. Jangan berikan kode ini kepada siapa pun."
            "tg" -> "Telegram code: $otpNum. You can also tap on this link to log in."
            "go" -> "G-$otpNum adalah kode verifikasi Google Anda."
            "tt" -> "[$otpNum] adalah kode verifikasi TikTok Anda. Berlaku selama 5 menit."
            "sp" -> "Kode OTP Shopee Anda: $otpNum. Rahasiakan kode ini dari siapapun termasuk pihak Shopee."
            else -> "Kode verifikasi untuk ${target.serviceName}: $otpNum. Rahasiakan kode ini."
        }

        deliverOtpToOrder(orderId, otpNum, sms)
    }

    private fun deliverOtpToOrder(orderId: String, otp: String, fullSms: String) {
        val updated = _uiState.value.activeOrders.map { order ->
            if (order.id == orderId) {
                order.copy(
                    status = NokosStatus.RECEIVED_OTP,
                    otpCode = otp,
                    fullSms = fullSms,
                    lastCheckedAt = System.currentTimeMillis()
                )
            } else order
        }

        val target = updated.find { it.id == orderId }

        _uiState.update {
            it.copy(
                activeOrders = updated,
                toastMessage = "🔔 SMS Diterima! OTP untuk ${target?.serviceName ?: "Layanan"}: $otp",
                isToastError = false
            )
        }

        if (target != null) {
            vaultStorage.archiveEncryptedRecord(target)
            refreshVaultRecords()
        }

        triggerHapticAlert()
    }

    private fun triggerHapticAlert() {
        if (!_uiState.value.hapticEnabled) return
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 150, 100, 250)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 150, 100, 250), -1)
                }
            }
        } catch (_: Exception) {}
    }

    // Continuous Real-Time Poller (polls every 4 seconds)
    private fun startRealtimePoller() {
        pollerJob?.cancel()
        pollerJob = viewModelScope.launch {
            while (isActive) {
                delay(4000)
                _uiState.update { it.copy(isPolling = true, lastPollTime = System.currentTimeMillis()) }

                val waitingOrders = _uiState.value.activeOrders.filter { it.status == NokosStatus.WAITING_SMS }

                if (waitingOrders.isNotEmpty()) {
                    for (order in waitingOrders) {
                        // Check expiration
                        if (order.remainingSeconds <= 0) {
                            val expiredList = _uiState.value.activeOrders.map {
                                if (it.id == order.id) it.copy(status = NokosStatus.EXPIRED) else it
                            }
                            _uiState.update { it.copy(activeOrders = expiredList) }
                            vaultStorage.archiveEncryptedRecord(order.copy(status = NokosStatus.EXPIRED))
                            continue
                        }

                        if (order.isDemo) {
                            // Demo auto-arrival: after 10-20 seconds with 30% chance per tick
                            val elapsedSeconds = (System.currentTimeMillis() - order.createdAt) / 1000
                            if (elapsedSeconds >= 12 && (1..3).random() == 1) {
                                simulateIncomingSms(order.id)
                            }
                        } else {
                            // Live polling smscode.gg
                            val token = vaultStorage.getApiToken()
                            if (token.isNotEmpty()) {
                                try {
                                    val res = ApiClient.smsCodeApi.getOrder("Bearer $token", order.id)
                                    if (res.isSuccessful && res.body() != null) {
                                        val body = res.body()!!
                                        val otp = body.otpCode ?: OtpExtractor.extractOtp(body.smsText)
                                        val sms = body.smsText
                                        if (!otp.isNullOrEmpty() || !sms.isNullOrEmpty()) {
                                            deliverOtpToOrder(order.id, otp ?: "", sms ?: "OTP: $otp")
                                        } else if (body.status?.uppercase() == "CANCELED" || body.status?.uppercase() == "CANCELLED") {
                                            val canceledList = _uiState.value.activeOrders.map {
                                                if (it.id == order.id) it.copy(status = NokosStatus.CANCELED) else it
                                            }
                                            _uiState.update { it.copy(activeOrders = canceledList) }
                                            vaultStorage.archiveEncryptedRecord(order.copy(status = NokosStatus.CANCELED))
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }

                _uiState.update { it.copy(isPolling = false) }
            }
        }
    }

    // Vault and End-to-End Encryption features
    fun refreshVaultRecords() {
        val encryptedList = vaultStorage.getEncryptedVaultRecords()
        val decryptedList = encryptedList.map { enc ->
            val plainPhone = cryptoManager.decrypt(enc.ivPhone, enc.encryptedPhone)
            val plainOtp = if (enc.encryptedOtp != null && enc.ivOtp != null) {
                cryptoManager.decrypt(enc.ivOtp, enc.encryptedOtp)
            } else null
            val plainSms = if (enc.encryptedSms != null && enc.ivSms != null) {
                cryptoManager.decrypt(enc.ivSms, enc.encryptedSms)
            } else null

            DecryptedVaultItem(
                recordId = enc.recordId,
                serviceName = enc.serviceName,
                countryName = enc.countryName,
                phoneNumber = plainPhone,
                otpCode = plainOtp,
                fullSms = plainSms,
                timestamp = enc.timestamp,
                status = enc.status,
                isDemo = enc.isDemo
            )
        }
        _uiState.update { it.copy(decryptedVaultRecords = decryptedList) }
    }

    fun unlockVault(pinAttempt: String): Boolean {
        val storedPin = vaultStorage.getVaultPin()
        if (storedPin.isEmpty() || storedPin == pinAttempt.trim()) {
            _uiState.update { it.copy(isVaultUnlocked = true) }
            refreshVaultRecords()
            return true
        }
        return false
    }

    fun lockVault() {
        _uiState.update { it.copy(isVaultUnlocked = false) }
    }

    fun setVaultPin(newPin: String) {
        vaultStorage.setVaultPin(newPin)
        _uiState.update {
            it.copy(
                hasVaultPin = newPin.isNotEmpty(),
                isVaultUnlocked = true,
                toastMessage = if (newPin.isNotEmpty()) "PIN Vault berhasil diaktifkan" else "PIN Vault dinonaktifkan",
                isToastError = false
            )
        }
    }

    fun deleteVaultRecord(id: String) {
        vaultStorage.deleteVaultRecord(id)
        refreshVaultRecords()
    }

    fun shredAllData() {
        vaultStorage.shredAllData()
        _uiState.update {
            it.copy(
                activeOrders = emptyList(),
                decryptedVaultRecords = emptyList(),
                balance = 25.0,
                isDemoMode = true,
                maskedToken = "Belum diatur",
                hasVaultPin = false,
                isVaultUnlocked = true,
                toastMessage = "🔥 Zero-Trace Wipe Selesai: Semua token, nomor, dan rekaman SMS dihapus permanen!",
                isToastError = false
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun removeActiveOrder(orderId: String) {
        _uiState.update { state ->
            state.copy(activeOrders = state.activeOrders.filter { it.id != orderId })
        }
    }
}
