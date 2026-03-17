package com.callapp.android.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class UsernameFormatterTest {

    @Test
    fun displayUsername_addsPrefixWhenMissing() {
        assertEquals("@alex", displayUsername("alex"))
    }

    @Test
    fun displayUsername_keepsSinglePrefixWhenAlreadyPresent() {
        assertEquals("@alex", displayUsername("@alex"))
    }

    @Test
    fun editableUsername_removesPrefixWhenPresent() {
        assertEquals("alex", editableUsername("@alex"))
    }

    @Test
    fun editableUsername_keepsValueWhenPrefixMissing() {
        assertEquals("alex", editableUsername("alex"))
    }
}
