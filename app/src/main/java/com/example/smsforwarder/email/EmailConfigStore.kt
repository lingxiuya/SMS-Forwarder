package com.example.smsforwarder.email

import android.content.Context
import android.content.SharedPreferences

object EmailConfigStore {
    private const val PREFS_NAME = "email_config_prefs"

    const val KEY_HOST = "host"
    const val KEY_PORT = "port"
    const val KEY_USERNAME = "username"
    const val KEY_PASSWORD = "password"
    const val KEY_RECIPIENT = "recipient"
    const val KEY_USE_TLS = "useTls"
    const val KEY_USE_SSL = "useSsl"

    const val DEFAULT_HOST = "smtp.qq.com"
    const val DEFAULT_PORT = 465
    const val DEFAULT_USERNAME = ""
    const val DEFAULT_PASSWORD = ""
    const val DEFAULT_RECIPIENT = ""
    const val DEFAULT_USE_TLS = false
    const val DEFAULT_USE_SSL = true

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context): EmailConfig {
        val prefs = getPrefs(context)
        val host = prefs.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
        val port = if (prefs.contains(KEY_PORT)) prefs.getInt(KEY_PORT, DEFAULT_PORT) else DEFAULT_PORT
        val username = prefs.getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_USERNAME
        val password = prefs.getString(KEY_PASSWORD, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_PASSWORD
        val recipient = prefs.getString(KEY_RECIPIENT, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_RECIPIENT
        val useTls = if (prefs.contains(KEY_USE_TLS)) prefs.getBoolean(KEY_USE_TLS, DEFAULT_USE_TLS) else DEFAULT_USE_TLS
        val useSsl = if (prefs.contains(KEY_USE_SSL)) prefs.getBoolean(KEY_USE_SSL, DEFAULT_USE_SSL) else (port == 465)

        return EmailConfig(
            smtpHost = host,
            smtpPort = port,
            username = username,
            password = password,
            fromAddress = username,
            toAddress = recipient,
            useTls = useTls,
            useSsl = useSsl
        )
    }

    fun saveConfig(
        context: Context,
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT,
        username: String = DEFAULT_USERNAME,
        password: String = DEFAULT_PASSWORD,
        recipient: String = DEFAULT_RECIPIENT,
        useTls: Boolean = DEFAULT_USE_TLS,
        useSsl: Boolean = DEFAULT_USE_SSL
    ) {
        getPrefs(context).edit()
            .putString(KEY_HOST, host)
            .putInt(KEY_PORT, port)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_RECIPIENT, recipient)
            .putBoolean(KEY_USE_TLS, useTls)
            .putBoolean(KEY_USE_SSL, useSsl)
            .apply()
    }

    fun saveConfig(context: Context, config: EmailConfig) {
        saveConfig(
            context = context,
            host = config.smtpHost,
            port = config.smtpPort,
            username = config.username,
            password = config.password,
            recipient = config.toAddress,
            useTls = config.useTls,
            useSsl = config.useSsl
        )
    }

    fun clear(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
