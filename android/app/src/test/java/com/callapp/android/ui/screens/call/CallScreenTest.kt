package com.callapp.android.ui.screens.call

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.callapp.android.ui.testutil.setAleAppContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun connectedCallShowsDurationAndInfoPanel() {
        composeRule.setAleAppContent {
            CallScreen(
                contactName = "Анна Смирнова",
                callStatus = CallStatus.CONNECTED,
                elapsedSeconds = 83,
            )
        }

        composeRule.onNodeWithText("01:23").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Информация о звонке").performClick()
        composeRule.onNodeWithText("Качество:").assertIsDisplayed()
    }

    @Test
    fun controlButtonsTriggerCallbacks() {
        var micToggled = false
        var cameraToggled = false
        var switched = false
        var ended = false

        composeRule.setAleAppContent {
            CallScreen(
                contactName = "Анна Смирнова",
                isMicOn = true,
                isCameraOn = false,
                onMicToggle = { micToggled = true },
                onCameraToggle = { cameraToggled = true },
                onSwitchCamera = { switched = true },
                onEndCall = { ended = true },
            )
        }

        composeRule.onNodeWithContentDescription("Выключить микрофон").performClick()
        composeRule.onNodeWithContentDescription("Включить камеру").performClick()
        composeRule.onNodeWithContentDescription("Переключить камеру").performClick()
        composeRule.onNodeWithContentDescription("Завершить звонок").performClick()

        assertTrue(micToggled)
        assertTrue(cameraToggled)
        assertTrue(switched)
        assertTrue(ended)
    }
}
