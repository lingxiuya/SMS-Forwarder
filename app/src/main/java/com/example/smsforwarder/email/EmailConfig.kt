package com.example.smsforwarder.email

data class EmailConfig(
    val smtpHost: String,
    val smtpPort: Int = 587,
    val username: String,
    val password: String,
    val fromAddress: String,
    val toAddress: String,
    val useTls: Boolean = true,
    val useSsl: Boolean = false
)
