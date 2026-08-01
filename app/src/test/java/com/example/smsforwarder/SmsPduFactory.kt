package com.example.smsforwarder

import java.io.ByteArrayOutputStream

object SmsPduFactory {
    fun create3GppSmsPdu(senderPhoneNumber: String, messageText: String): ByteArray {
        val pduStream = ByteArrayOutputStream()

        // 1. Service Center Address Length (0x00 = default/none)
        pduStream.write(0x00)

        // 2. SMS-DELIVER First Octet (0x04 = MTI: SMS-DELIVER, no header)
        pduStream.write(0x04)

        // 3. Sender Address Length & Format
        val cleanNumber = senderPhoneNumber.replace("+", "")
        pduStream.write(cleanNumber.length)
        pduStream.write(if (senderPhoneNumber.startsWith("+")) 0x91 else 0x81)

        // Semi-octet encoding for phone number digits
        for (i in cleanNumber.indices step 2) {
            val d1 = cleanNumber[i] - '0'
            val d2 = if (i + 1 < cleanNumber.length) cleanNumber[i + 1] - '0' else 0x0F
            pduStream.write((d2 shl 4) or d1)
        }

        // 4. Protocol Identifier (0x00 = implicit text)
        pduStream.write(0x00)

        // 5. Data Coding Scheme (0x04 = 8-bit data)
        pduStream.write(0x04)

        // 6. Timestamp (7 bytes: Year, Month, Day, Hour, Min, Sec, TimeZone)
        pduStream.write(byteArrayOf(0x26, 0x08, 0x01, 0x12, 0x00, 0x00, 0x00))

        // 7. User Data Length & Body bytes
        val textBytes = messageText.toByteArray(Charsets.UTF_8)
        pduStream.write(textBytes.size)
        pduStream.write(textBytes)

        return pduStream.toByteArray()
    }
}
