package com.example.smsforwarder

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import com.example.smsforwarder.email.DefaultEmailForwarder
import com.example.smsforwarder.email.EmailConfig
import com.example.smsforwarder.email.EmailForwarder
import com.example.smsforwarder.email.JavaMailEmailTransport
import com.example.smsforwarder.email.SmsData
import com.example.smsforwarder.receiver.SmsReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.plus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Properties

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EmpiricalEdgeCasesTest {

    private lateinit var context: Context
    private lateinit var fakeForwarder: SmsReceiverTest.FakeEmailForwarder
    private lateinit var receiver: SmsReceiver
    private val testConfig = EmailConfig(
        smtpHost = "smtp.test.com",
        smtpPort = 587,
        username = "user@test.com",
        password = "secretpassword",
        fromAddress = "sender@test.com",
        toAddress = "receiver@test.com"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeForwarder = SmsReceiverTest.FakeEmailForwarder()
        receiver = SmsReceiver(
            emailForwarder = fakeForwarder,
            configProvider = { testConfig },
            scope = TestScope() + Dispatchers.Unconfined
        )
    }

    // -------------------------------------------------------------------------
    // EDGE CASE 1: Empty SMS Body
    // -------------------------------------------------------------------------
    @Test
    fun testEmptySmsBody_extractsAndForwardsSuccessfully() {
        val pdu = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+15550000", messageText = "")
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(pdu))
            putExtra("format", "3gpp")
        }

        val smsData = receiver.extractSmsData(intent)
        assertNotNull(smsData)
        assertEquals("", smsData?.body)

        receiver.onReceive(context, intent)
        assertEquals(1, fakeForwarder.forwardedList.size)
        assertEquals("", fakeForwarder.forwardedList.first().body)
    }

    // -------------------------------------------------------------------------
    // EDGE CASE 2: Multi-part SMS PDU Concatenation
    // -------------------------------------------------------------------------
    @Test
    fun testMultipartSmsPduConcatenation_joinsInOrder() {
        val pdu1 = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+15551111", messageText = "Part 1 - ")
        val pdu2 = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+15551111", messageText = "Part 2 - ")
        val pdu3 = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+15551111", messageText = "Part 3")

        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(pdu1, pdu2, pdu3))
            putExtra("format", "3gpp")
        }

        val smsData = receiver.extractSmsData(intent)
        assertNotNull(smsData)
        assertEquals("Part 1 - Part 2 - Part 3", smsData?.body)

        receiver.onReceive(context, intent)
        assertEquals(1, fakeForwarder.forwardedList.size)
        assertEquals("Part 1 - Part 2 - Part 3", fakeForwarder.forwardedList.first().body)
    }

    // -------------------------------------------------------------------------
    // EDGE CASE 3: Special Characters & Unicode Text
    // -------------------------------------------------------------------------
    @Test
    fun testUnicodeAndSpecialCharacters_handlingInForwarder() = runTest {
        val unicodeText = "Hello 世界 🌍! Special chars: <>&\"' $%\nNew Line"
        val mockTransport = EmailForwarderTest.FakeEmailTransport()
        val forwarder = DefaultEmailForwarder(mockTransport, UnconfinedTestDispatcher())

        val smsData = SmsData(sender = "+1999888777", body = unicodeText)
        val result = forwarder.forwardSms(smsData, testConfig)

        assertTrue(result.isSuccess)
        assertTrue(mockTransport.lastBodyText?.contains("Hello 世界 🌍!") == true)
        assertEquals("[SMS Forward] From: +1999888777", mockTransport.lastSubject)
    }

    // -------------------------------------------------------------------------
    // EDGE CASE 4: Null Intent Extras & Production Instantiation Vulnerability
    // -------------------------------------------------------------------------
    @Test
    fun testNullIntentExtras_returnsNullWithoutCrashing() {
        val nullExtrasIntent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        val smsData = receiver.extractSmsData(nullExtrasIntent)
        assertNull(smsData)
    }

    @Test
    fun testWrongIntentAction_returnsNull() {
        val wrongActionIntent = Intent("android.intent.action.BOOT_COMPLETED").apply {
            putExtra("pdus", arrayOf(SmsPduFactory.create3GppSmsPdu("+123", "test")))
        }
        val smsData = receiver.extractSmsData(wrongActionIntent)
        assertNull(smsData)
    }

    @Test
    fun testDefaultConstructorSmsReceiver_instantiatesProductionDependencies() {
        // Simulates OS instantiating SmsReceiver via default no-arg constructor
        val defaultReceiver = SmsReceiver()
        val pdu = SmsPduFactory.create3GppSmsPdu(senderPhoneNumber = "+15551234", messageText = "Production SMS")
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(pdu))
            putExtra("format", "3gpp")
        }

        // Must not throw an exception and automatically loads EmailConfigStore fallback config and real forwarder
        defaultReceiver.onReceive(context, intent)
    }

    // -------------------------------------------------------------------------
    // EDGE CASE 5: SMTP Timeout & Failure Result Handling
    // -------------------------------------------------------------------------
    @Test
    fun testSmsReceiver_ignoresResultFailureFromForwarder() {
        val failingForwarder = object : EmailForwarder {
            var callCount = 0
            override suspend fun forwardSms(smsData: SmsData, config: EmailConfig): Result<Unit> {
                callCount++
                return Result.failure(RuntimeException("SMTP Connection Timeout"))
            }
        }

        val testReceiver = SmsReceiver(
            emailForwarder = failingForwarder,
            configProvider = { testConfig },
            scope = TestScope() + Dispatchers.Unconfined
        )

        val pdu = SmsPduFactory.create3GppSmsPdu("+15550000", "Fail Test")
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
            putExtra("pdus", arrayOf(pdu))
            putExtra("format", "3gpp")
        }

        // Receiver invokes forwarder, forwarder returns Result.failure, receiver has no retry or error handling logic
        testReceiver.onReceive(context, intent)
        assertEquals(1, failingForwarder.callCount)
    }
}
