package com.example.smsforwarder

import android.Manifest
import android.os.Build
import android.widget.Button
import com.example.smsforwarder.service.KeepAliveService
import com.example.smsforwarder.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class MainActivityTest {

    @Before
    fun setUp() {
        KeepAliveService.isRunning = false
    }

    @Test
    fun testToggleServiceButton_startsKeepAliveService_andUpdatesButtonText() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        val btnToggleService = activity.findViewById<Button>(R.id.btnToggleService)
        assertEquals("Start Keep-Alive Service", btnToggleService.text.toString())

        btnToggleService.performClick()

        val nextStartedService = shadowOf(activity).nextStartedService
        assertNotNull(nextStartedService)
        assertEquals(KeepAliveService::class.java.name, nextStartedService.component?.className)
        assertEquals("Stop Keep-Alive Service", btnToggleService.text.toString())

        btnToggleService.performClick()
        assertEquals("Start Keep-Alive Service", btnToggleService.text.toString())
    }

    @Test
    fun testRuntimePermissionRequest() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        val requestedPermissions = shadowOf(activity).lastRequestedPermission.requestedPermissions
        assertNotNull(requestedPermissions)
        assertTrue(requestedPermissions.contains(Manifest.permission.RECEIVE_SMS))
    }

    @Test
    fun testBtnTestEmail_dispatchesEmailAndShowsToast() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()

        val fakeForwarder = SmsReceiverTest.FakeEmailForwarder()
        activity.emailForwarder = fakeForwarder

        controller.start().resume()

        val btnTestEmail = activity.findViewById<Button>(R.id.btnTestEmail)
        btnTestEmail.performClick()

        assertEquals(1, fakeForwarder.forwardedList.size)
        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("Test email sent successfully", latestToast)
    }
}
