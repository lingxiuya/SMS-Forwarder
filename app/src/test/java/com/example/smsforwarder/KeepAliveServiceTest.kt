package com.example.smsforwarder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.smsforwarder.service.KeepAliveService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class KeepAliveServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        KeepAliveService.isRunning = false
    }

    @Test
    fun testServiceStartIntent() {
        KeepAliveService.startService(context)

        val nextStartedService = shadowOf(context).nextStartedService
        assertNotNull(nextStartedService)
        assertEquals(KeepAliveService::class.java.name, nextStartedService.component?.className)
        assertEquals(KeepAliveService.ACTION_START, nextStartedService.action)
    }

    @Test
    fun testNotificationChannelCreationOnServiceStart() {
        val service = KeepAliveService()
        service.onCreate()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = shadowOf(notificationManager)

        val channel = shadowManager.getNotificationChannel(KeepAliveService.CHANNEL_ID)
        assertNotNull("Notification channel must be created", channel)
        assertEquals(KeepAliveService.CHANNEL_ID, channel.id)
    }

    @Test
    fun testIsRunningStateOnStartAndStopCommand() {
        val service = KeepAliveService()
        service.onCreate()

        assertFalse(KeepAliveService.isRunning)

        val startIntent = Intent(context, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_START
        }
        service.onStartCommand(startIntent, 0, 1)
        assertTrue(KeepAliveService.isRunning)

        val stopIntent = Intent(context, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_STOP
        }
        service.onStartCommand(stopIntent, 0, 2)
        assertFalse(KeepAliveService.isRunning)
    }

    @Test
    @Config(sdk = [34])
    fun testServiceStartOnSdk34() {
        val service = KeepAliveService()
        service.onCreate()

        val startIntent = Intent(context, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_START
        }
        service.onStartCommand(startIntent, 0, 1)
        assertTrue(KeepAliveService.isRunning)
    }
}
