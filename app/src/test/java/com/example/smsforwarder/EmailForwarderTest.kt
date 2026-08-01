package com.example.smsforwarder

import com.example.smsforwarder.email.DefaultEmailForwarder
import com.example.smsforwarder.email.EmailConfig
import com.example.smsforwarder.email.EmailTransport
import com.example.smsforwarder.email.SmsData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailForwarderTest {

    private lateinit var mockTransport: FakeEmailTransport
    private lateinit var forwarder: DefaultEmailForwarder

    private val testConfig = EmailConfig(
        smtpHost = "smtp.example.com",
        smtpPort = 587,
        username = "user@example.com",
        password = "password",
        fromAddress = "from@example.com",
        toAddress = "to@example.com"
    )

    class FakeEmailTransport : EmailTransport {
        var lastSubject: String? = null
        var lastBodyText: String? = null
        var lastConfig: EmailConfig? = null
        var shouldFail = false

        override suspend fun sendEmail(
            config: EmailConfig,
            subject: String,
            bodyText: String
        ): Result<Unit> {
            return if (shouldFail) {
                Result.failure(RuntimeException("SMTP connection failed"))
            } else {
                lastConfig = config
                lastSubject = subject
                lastBodyText = bodyText
                Result.success(Unit)
            }
        }
    }

    @Before
    fun setUp() {
        mockTransport = FakeEmailTransport()
        forwarder = DefaultEmailForwarder(
            transport = mockTransport,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun testForwardSms_formatsEmailSubjectAndBodyCorrectly() = runTest {
        val smsData = SmsData(
            sender = "+1234567890",
            body = "Hello World!",
            timestamp = 1600000000000L
        )

        val result = forwarder.forwardSms(smsData, testConfig)

        assertTrue(result.isSuccess)
        assertEquals(testConfig, mockTransport.lastConfig)
        assertEquals("[SMS Forward] From: +1234567890", mockTransport.lastSubject)
        assertTrue(mockTransport.lastBodyText?.contains("+1234567890") == true)
        assertTrue(mockTransport.lastBodyText?.contains("Hello World!") == true)
    }

    @Test
    fun testForwardSms_whenTransportFails_returnsFailure() = runTest {
        mockTransport.shouldFail = true
        val smsData = SmsData(sender = "+1234567890", body = "Test Error")

        val result = forwarder.forwardSms(smsData, testConfig)

        assertTrue(result.isFailure)
        assertEquals("SMTP connection failed", result.exceptionOrNull()?.message)
    }
}
