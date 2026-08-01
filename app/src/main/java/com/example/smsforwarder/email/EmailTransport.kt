package com.example.smsforwarder.email

interface EmailTransport {
    suspend fun sendEmail(
        config: EmailConfig,
        subject: String,
        bodyText: String
    ): Result<Unit>
}
