package com.sudokuengine.app.ui

// Pure, platform-independent helpers for describing the result of a camera capture.
//
// Kept free of `android.*` imports so it can be unit-tested on the JVM without an emulator.

/** Status text shown before any capture has been attempted. */
const val NO_IMAGE_CAPTURED_YET: String = "No image captured yet"

/** Status text shown when the user denies the `CAMERA` runtime permission. */
const val CAMERA_PERMISSION_DENIED: String = "Permission denied"

/** Status text shown when the user cancels the capture (e.g. backs out of the camera app). */
const val CAPTURE_CANCELLED: String = "Capture cancelled"

/**
 * Builds the status text for a successfully captured image.
 *
 * @param byteCount number of bytes read back from the captured image file. Must be >= 0.
 * @return a human-readable status string, e.g. "Captured image: 1234 bytes".
 */
fun capturedImageStatus(byteCount: Int): String = "Captured image: $byteCount bytes"

/**
 * Builds the status text for a captured image's byte array.
 *
 * @param bytes the bytes read back from the captured image file, or `null` if nothing has
 * been captured yet.
 * @return [NO_IMAGE_CAPTURED_YET] if [bytes] is `null`, otherwise [capturedImageStatus].
 */
fun captureStatusFor(bytes: ByteArray?): String =
    if (bytes == null) {
        NO_IMAGE_CAPTURED_YET
    } else {
        capturedImageStatus(bytes.size)
    }
