package com.sudokuengine.reader.opencv

import org.opencv.android.OpenCVLoader

/**
 * Ensures the OpenCV native library is loaded exactly once per process.
 *
 * [org.opencv.android.OpenCVLoader.initLocal] is idempotent but touches native state, so we guard
 * it with a one-shot flag to avoid repeated JNI calls across multiple [ensureLoaded] callers.
 */
internal object OpenCvInit {
    @Volatile
    private var loaded = false

    /** Loads the OpenCV native library if it hasn't been loaded yet. */
    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            OpenCVLoader.initLocal()
            loaded = true
        }
    }
}
