package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoManager
import com.example.util.OtpExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nokos SMS", appName)
    }

    @Test
    fun `test end-to-end crypto encryption and decryption`() {
        val crypto = CryptoManager()
        val plainText = "SMSCODE_SECRET_TOKEN_12345"

        val encrypted = crypto.encrypt(plainText)
        assertNotNull(encrypted.cipherTextBase64)
        assertNotNull(encrypted.ivBase64)
        assertTrue(encrypted.cipherTextBase64.isNotEmpty())

        val decrypted = crypto.decrypt(encrypted.ivBase64, encrypted.cipherTextBase64)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `test otp extraction logic`() {
        val sms1 = "<#> Kode verifikasi WhatsApp Anda: 583-720. Jangan berikan kode ini kepada siapapun."
        val otp1 = OtpExtractor.extractOtp(sms1)
        assertEquals("583720", otp1)

        val sms2 = "G-839210 adalah kode verifikasi Google Anda."
        val otp2 = OtpExtractor.extractOtp(sms2)
        assertEquals("G-839210", otp2)

        val sms3 = "Your OTP code is 4920."
        val otp3 = OtpExtractor.extractOtp(sms3)
        assertEquals("4920", otp3)
    }
}
