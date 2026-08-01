package com.example.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.smsforwarder.email.DefaultEmailForwarder
import com.example.smsforwarder.email.EmailConfig
import com.example.smsforwarder.email.EmailConfigStore
import com.example.smsforwarder.email.EmailForwarder
import com.example.smsforwarder.email.JavaMailEmailTransport
import com.example.smsforwarder.email.SmsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver(
    private val emailForwarder: EmailForwarder? = null,
    private val configProvider: (() -> EmailConfig)? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val smsData = extractSmsData(intent) ?: return

        val forwarder = emailForwarder ?: getGlobalEmailForwarder(context)
        val config = configProvider?.invoke() ?: getGlobalEmailConfig(context)

        val pendingResult: PendingResult = goAsync()
        scope.launch {
            try {
                if (config != null && forwarder != null) {
                    val result = forwarder.forwardSms(smsData, config)
                    result.onSuccess {
                        Log.i(TAG, "SMS successfully forwarded to email for sender: ${smsData.sender}")
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to forward SMS to email", e)
                    }
                } else {
                    Log.w(TAG, "EmailForwarder or EmailConfig not available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to forward SMS to email", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    fun extractSmsData(intent: Intent): SmsData? {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return null

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!messages.isNullOrEmpty()) {
                val sortedMessages = messages.mapIndexed { index, sms ->
                    val partIndex = extractPartIndex(sms) ?: index
                    partIndex to sms
                }.sortedBy { it.first }.map { it.second }

                val sender = sortedMessages[0].originatingAddress ?: "Unknown"
                val timestamp = sortedMessages[0].timestampMillis
                val body = sortedMessages.joinToString(separator = "") { it.messageBody ?: "" }
                return SmsData(sender = sender, body = body, timestamp = timestamp)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Telephony.Sms.Intents.getMessagesFromIntent failed, trying fallback", e)
        }

        val bundle = intent.extras ?: return null
        val pdus = bundle.get("pdus") as? Array<*> ?: return null
        val format = bundle.getString("format") ?: "3gpp"

        val smsList = pdus.mapNotNull { pdu ->
            (pdu as? ByteArray)?.let { SmsMessage.createFromPdu(it, format) }
        }
        if (smsList.isEmpty()) return null

        val sortedSmsList = smsList.mapIndexed { index, sms ->
            val partIndex = extractPartIndex(sms) ?: index
            partIndex to sms
        }.sortedBy { it.first }.map { it.second }

        val sender = sortedSmsList[0].originatingAddress ?: "Unknown"
        val timestamp = sortedSmsList[0].timestampMillis
        val body = sortedSmsList.joinToString(separator = "") { it.messageBody ?: "" }
        return SmsData(sender = sender, body = body, timestamp = timestamp)
    }

    private fun extractPartIndex(sms: SmsMessage): Int? {
        return try {
            val pdu = sms.pdu ?: return null
            if (pdu.size < 3) return null
            val scaLen = (pdu[0].toInt() and 0xFF) + 1
            if (pdu.size <= scaLen) return null
            val firstOctet = pdu[scaLen].toInt() and 0xFF
            val hasUdh = (firstOctet and 0x40) != 0
            if (!hasUdh) return null

            val addrLen = pdu[scaLen + 1].toInt() and 0xFF
            val addrOctets = (addrLen + 1) / 2 + 2
            val udlIndex = scaLen + 1 + addrOctets + 1 + 1 + 7
            if (pdu.size <= udlIndex) return null

            val udhl = pdu[udlIndex + 1].toInt() and 0xFF
            var offset = udlIndex + 2
            val endOffset = (offset + udhl).coerceAtMost(pdu.size)
            while (offset + 1 < endOffset) {
                val iei = pdu[offset].toInt() and 0xFF
                val ieLen = pdu[offset + 1].toInt() and 0xFF
                if (iei == 0x00 && ieLen >= 3 && offset + 4 < pdu.size) {
                    return pdu[offset + 4].toInt() and 0xFF
                } else if (iei == 0x08 && ieLen >= 4 && offset + 5 < pdu.size) {
                    return pdu[offset + 5].toInt() and 0xFF
                }
                offset += 2 + ieLen
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getGlobalEmailForwarder(context: Context): EmailForwarder {
        return DefaultEmailForwarder(JavaMailEmailTransport())
    }

    private fun getGlobalEmailConfig(context: Context): EmailConfig {
        return EmailConfigStore.getConfig(context)
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
