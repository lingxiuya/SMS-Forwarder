package com.example.smsforwarder

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.smsforwarder.email.EmailConfig
import com.example.smsforwarder.email.EmailConfigStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EmailConfigStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        EmailConfigStore.clear(context)
    }

    @Test
    fun testGetConfig_unconfigured_returnsDefaultFallbackValues() {
        val config = EmailConfigStore.getConfig(context)

        assertEquals(EmailConfigStore.DEFAULT_HOST, config.smtpHost)
        assertEquals(EmailConfigStore.DEFAULT_PORT, config.smtpPort)
        assertEquals(EmailConfigStore.DEFAULT_USERNAME, config.username)
        assertEquals(EmailConfigStore.DEFAULT_PASSWORD, config.password)
        assertEquals(EmailConfigStore.DEFAULT_USERNAME, config.fromAddress)
        assertEquals(EmailConfigStore.DEFAULT_RECIPIENT, config.toAddress)
        assertTrue(config.useTls)
    }

    @Test
    fun testSaveAndGetConfig_configured_returnsSavedValues() {
        val customConfig = EmailConfig(
            smtpHost = "smtp.custom.org",
            smtpPort = 465,
            username = "custom_user@custom.org",
            password = "custom_password",
            fromAddress = "custom_user@custom.org",
            toAddress = "destination@custom.org",
            useTls = false
        )

        EmailConfigStore.saveConfig(context, customConfig)
        val retrievedConfig = EmailConfigStore.getConfig(context)

        assertEquals("smtp.custom.org", retrievedConfig.smtpHost)
        assertEquals(465, retrievedConfig.smtpPort)
        assertEquals("custom_user@custom.org", retrievedConfig.username)
        assertEquals("custom_password", retrievedConfig.password)
        assertEquals("destination@custom.org", retrievedConfig.toAddress)
        assertEquals(false, retrievedConfig.useTls)
    }
}
