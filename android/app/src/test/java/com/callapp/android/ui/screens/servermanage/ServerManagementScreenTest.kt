package com.callapp.android.ui.screens.servermanage

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class ServerManagementScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersFilledStateWithoutCrashing() {
        composeRule.setAleAppContent {
            ServerManagementScreen(
                initial = ServerManageData(
                    id = "srv-1",
                    name = "Tech Community",
                    username = "tech_community",
                    description = "Description",
                    imageUrl = "",
                ),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun rendersEmptyStateWithoutCrashing() {
        composeRule.setAleAppContent {
            ServerManagementScreen(
                initial = ServerManageData(
                    id = "srv-1",
                    name = "",
                    username = "",
                    description = "",
                    imageUrl = "",
                ),
            )
        }

        composeRule.waitForIdle()
    }

    @Test
    fun changePhotoButton_opensPhotoDialog() {
        var pickerRequested = false

        composeRule.setAleAppContent {
            ServerManagementScreen(
                initial = ServerManageData(
                    id = "srv-1",
                    name = "Tech Community",
                    username = "tech_community",
                    description = "Description",
                    imageUrl = "",
                ),
                onPhotoPickerRequest = { pickerRequested = true },
            )
        }

        composeRule.onNodeWithContentDescription("Изменить фото").performClick()
        assertTrue(pickerRequested)
    }
}
