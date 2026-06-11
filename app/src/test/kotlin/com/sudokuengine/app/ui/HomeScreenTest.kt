package com.sudokuengine.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [imageStatusText], the pure status-formatting function used by
 * [HomeScreen] after a gallery picker round-trip.
 */
class HomeScreenTest {
    @Test
    fun `null bytes report no image selected`() {
        assertEquals("No image selected", imageStatusText(null))
    }

    @Test
    fun `non-null bytes report their size`() {
        assertEquals("Selected image: 1234 bytes", imageStatusText(ByteArray(1234)))
    }

    @Test
    fun `empty byte array reports zero bytes`() {
        assertEquals("Selected image: 0 bytes", imageStatusText(ByteArray(0)))
    }
}
