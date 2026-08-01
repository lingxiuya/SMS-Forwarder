package com.example.smsforwarder

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import com.example.smsforwarder.email.EmailConfig
import com.example.smsforwarder.email.EmailConfigStore
import com.example.smsforwarder.email.EmailForwarder
import com.example.smsforwarder.email.SmsData
import com.example.smsforwarder.receiver.SmsReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.plus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class SmsReceiverTest {

    private lateinit var context: Context
    private lateinit var fakeForwarder: FakeEmailForwarder
    private lateinit var receiver: SmsReceiver
    private val testConfig = EmailConfig(
        smtpHost = "smtp.test.com",
        smtpPort = 587,
        username = "user@test.com",
        password = "secretpassword",
        fromAddress = "sender@test.com",
        toAddress = "receiver@test.com"
    )

    class FakeEmailForwarder : EmailForwarder {
        val forwardedList = mutableListOf<SmsData>()
        var shouldFail = false

        override suspend fun forwardSms(smsData: SmsData, config: EmailConfig): Result<Unit> {
            return if (shouldFail) {
                Result.failure(RuntimeException("Network error"))
            } else {
                forwardedList.add(smsData)
                Result.success(Unit)
            }
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        EmailConfigStore.clear(context)
        fakeForwarder = FakeEmailForwarder()
        receiver = SmsReceiver(
            emailForwarder = fakeForwarder,
            configProvider = { testConfig },
            scope = TestScope() + Dispatchers.Unconfined
        )
    }

    @Test
    fun testOnReceive_triggersEmailForwarding_withCorrectData() {
        val pdu = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+1555123456", messageText = "Test SMS Payload")
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(pdu))
            putExtra("format", "3gpp")
        }

        receiver.onReceive(context, intent)

        assertEquals(1, fakeForwarder.forwardedList.size)
        val forwarded = fakeForwarder.forwardedList.first()
        assertEquals("+1555123456", forwarded.sender)
        assertEquals("Test SMS Payload", forwarded.body)
    }

    @Test
    fun testOnReceive_defaultConstructor_usesConfigStoreFallback() {
        val defaultReceiver = SmsReceiver()
        val pdu = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+15559999", messageText = "Production Message")
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(pdu))
            putExtra("format", "3gpp")
        }

        defaultReceiver.onReceive(context, intent)

        val config = EmailConfigStore.getConfig(context)
        assertEquals(EmailConfigStore.DEFAULT_HOST, config.smtpHost)
    }
}
