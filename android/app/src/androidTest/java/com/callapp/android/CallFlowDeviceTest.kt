package com.callapp.android

import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallFlowDeviceTest {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun acceptIncomingCallWhenPromptAppears() {
        if (Build.MODEL != "22101320G") return

        launchApp()
        Thread.sleep(10_000L)

        val acceptButton = waitForAny(
            timeoutMs = 90_000L,
            By.text("Принять"),
            By.descContains("Принять звонок"),
        )

        assertNotNull("Incoming call accept button did not appear", acceptButton)
        tapCenter(acceptButton!!)

        device.waitForIdle()
        device.wait(Until.hasObject(By.descContains("Завершить звонок")), 15_000L)
    }

    @Test
    fun placeCallToRemoteUserAndEnableCamera() {
        if (Build.MODEL != "Pixel 9 Pro") return

        launchApp()
        Thread.sleep(10_000L)

        val serverCard = waitForAny(
            timeoutMs = 40_000L,
            By.textContains("CallApp Test Server"),
        )
        assertNotNull("Server card did not appear", serverCard)
        tapCenter(serverCard!!)
        Thread.sleep(5_000L)

        val callButton = waitForAny(
            timeoutMs = 20_000L,
            By.descContains("Позвонить Mi QA"),
            By.descContains("Позвонить qwertz"),
        )
        assertNotNull("Call button for remote user did not appear", callButton)
        tapCenter(callButton!!)

        val cameraButton = waitForAny(
            timeoutMs = 90_000L,
            By.descContains("Включить камеру"),
            By.descContains("Выключить камеру"),
        )

        assertNotNull("Camera toggle did not appear", cameraButton)
        tapCenter(cameraButton!!)

        device.waitForIdle()
        device.wait(Until.hasObject(By.descContains("Выключить камеру")), 15_000L)
    }

    private fun launchApp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        requireNotNull(launchIntent) { "Launch intent for ${context.packageName} not found" }
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        context.startActivity(launchIntent)
        device.waitForIdle()
        device.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10_000L)
    }

    private fun waitForAny(
        timeoutMs: Long,
        vararg selectors: BySelector,
    ): UiObject2? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            selectors.forEach { selector ->
                val found = device.findObject(selector)
                if (found != null) {
                    return found
                }
            }

            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0L) break
            device.waitForIdle()
            device.wait(Until.findObject(selectors.first()), minOf(1_000L, remaining))
        }
        return null
    }

    private fun tapCenter(node: UiObject2) {
        val clickableNode = generateSequence(node) { current -> current.parent }
            .firstOrNull { current -> current.isClickable }
            ?: node
        val bounds = clickableNode.visibleBounds
        assertTrue(
            "Failed to tap node at $bounds",
            device.click(bounds.centerX(), bounds.centerY()),
        )
        device.waitForIdle()
    }
}
