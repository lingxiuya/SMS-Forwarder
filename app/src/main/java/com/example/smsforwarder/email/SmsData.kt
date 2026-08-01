package com.example.smsforwarder.email

data class SmsData(
    val sender: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)
