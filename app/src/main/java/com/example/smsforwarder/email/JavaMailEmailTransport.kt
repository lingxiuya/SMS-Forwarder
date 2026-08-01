package com.example.smsforwarder.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class JavaMailEmailTransport : EmailTransport {

    init {
        // 修复安卓 IPv6 路由黑洞导致的读取超时
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")
    }

    override suspend fun sendEmail(
        config: EmailConfig,
        subject: String,
        bodyText: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 根据是否使用 SSL 决定底层协议
            val protocol = if (config.useSsl) "smtps" else "smtp"

            val props = Properties().apply {
                put("mail.transport.protocol", protocol)
                put("mail.$protocol.host", config.smtpHost.trim())
                put("mail.$protocol.port", config.smtpPort.toString())
                put("mail.$protocol.auth", "true")
                put("mail.$protocol.connectiontimeout", "15000")
                put("mail.$protocol.timeout", "15000")
                put("mail.$protocol.writetimeout", "15000")
                put("mail.$protocol.localhost", "localhost")

                if (config.useSsl) {
                    // 465 端口：使用隐式 SSL（smtps 协议自带 SSL）
                    put("mail.smtps.ssl.enable", "true")
                    put("mail.smtps.starttls.enable", "false")
                    put("mail.smtps.ssl.protocols", "TLSv1.2")
                    put("mail.smtps.socketFactory.port", config.smtpPort.toString())
                    put("mail.smtps.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtps.socketFactory.fallback", "false")
                } else if (config.useTls) {
                    // 587 端口：使用 STARTTLS
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                }
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username.trim(), config.password.trim())
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.fromAddress.trim()))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.toAddress.trim()))
                setSubject(subject, "UTF-8")
                setText(bodyText, "UTF-8")
                sentDate = Date()
            }

            // 使用显式 Transport 连接，而不是 Transport.send() 静态方法
            // Transport.send() 可能忽略 mail.transport.protocol 设置而默认使用 smtp
            val transport = session.getTransport(protocol)
            try {
                transport.connect(
                    config.smtpHost.trim(),
                    config.smtpPort,
                    config.username.trim(),
                    config.password.trim()
                )
                transport.sendMessage(message, message.allRecipients)
            } finally {
                transport.close()
            }
        }.recoverCatching { e ->
            val fullError = buildString {
                append(e.javaClass.simpleName)
                append(": ")
                append(e.message)
                var cause = e.cause
                var depth = 0
                while (cause != null && depth < 3) {
                    append(" | 因: ${cause.javaClass.simpleName}: ${cause.message}")
                    cause = cause.cause
                    depth++
                }
            }
            throw Exception(fullError, e)
        }
    }
}
