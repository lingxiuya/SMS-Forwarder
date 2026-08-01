package com.example.smsforwarder

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.smsforwarder.util.BatteryOptimizationUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class BatteryOptimizationUtilTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testIsIgnoringBatteryOptimizations_whenIgnoring_returnsTrue() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val shadowPowerManager = shadowOf(powerManager)

        shadowPowerManager.setIgnoringBatteryOptimizations(true)

        val isIgnored = BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)
        assertTrue(isIgnored)
    }

    @Test
    fun testIsIgnoringBatteryOptimizations_whenNotIgnoring_returnsFalse() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val shadowPowerManager = shadowOf(powerManager)

        shadowPowerManager.setIgnoringBatteryOptimizations(false)

        val isIgnored = BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)
        assertFalse(isIgnored)
    }

    @Test
    fun testCreateRequestIgnoreBatteryOptimizationIntent() {
        val intent = BatteryOptimizationUtil.createRequestIgnoreBatteryOptimizationIntent(context)

        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }
}
