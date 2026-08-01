package com.example.smsforwarder.email

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface EmailForwarder {
    suspend fun forwardSms(
        smsData: SmsData,
        config: EmailConfig
    ): Result<Unit>
}

class DefaultEmailForwarder(
    private val transport: EmailTransport,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EmailForwarder {

    override suspend fun forwardSms(
        smsData: SmsData,
        config: EmailConfig
    ): Result<Unit> = withContext(ioDispatcher) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(smsData.timestamp))

        val subject = "[SMS Forward] From: ${smsData.sender}"
        val bodyText = """
            Incoming SMS Notification
            ----------------------------------------
            From: ${smsData.sender}
            Date: $formattedDate
            
            Message Content:
            ${smsData.body}
            ----------------------------------------
            Sent automatically by Android SMS Forwarder.
        """.trimIndent()

        transport.sendEmail(config, subject, bodyText)
    }
}
