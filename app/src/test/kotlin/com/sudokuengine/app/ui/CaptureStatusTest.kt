package com.sudokuengine.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the pure capture-status helpers used by [CameraCaptureButton].
 */
class CaptureStatusTest {
    @Test
    fun `no bytes captured yet`() {
        assertEquals(NO_IMAGE_CAPTURED_YET, captureStatusFor(null))
    }

    @Test
    fun `captured image reports byte count`() {
        val bytes = ByteArray(1234)
        assertEquals("Captured image: 1234 bytes", captureStatusFor(bytes))
    }

    @Test
    fun `empty capture reports zero bytes`() {
        assertEquals("Captured image: 0 bytes", captureStatusFor(ByteArray(0)))
    }

    @Test
    fun `capturedImageStatus formats byte count directly`() {
        assertEquals("Captured image: 42 bytes", capturedImageStatus(42))
    }
}
