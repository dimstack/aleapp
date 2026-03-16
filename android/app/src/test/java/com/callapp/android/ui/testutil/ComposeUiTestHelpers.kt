package com.callapp.android.ui.testutil

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.callapp.android.ui.theme.AleAppTheme

fun ComposeContentTestRule.setAleAppContent(content: @Composable () -> Unit) {
    setContent {
        AleAppTheme(darkTheme = false) {
            content()
        }
    }
}
