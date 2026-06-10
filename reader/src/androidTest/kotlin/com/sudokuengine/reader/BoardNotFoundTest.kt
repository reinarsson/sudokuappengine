package com.sudokuengine.reader

import androidx.test.platform.app.InstrumentationRegistry
import com.sudokuengine.reader.litert.LiteRtDigitClassifier
import com.sudokuengine.reader.opencv.OpenCvSudokuReader
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import kotlin.test.Test
import kotlin.test.assertEquals

/** A blank (uniform) image contains no board contours and must yield [ReadOutcome.BoardNotFound]. */
class BoardNotFoundTest {
    @Test
    fun blankImageReturnsBoardNotFound() {
        OpenCVLoader.initLocal()

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val modelBytes = TestAssets.readAppAsset(appContext, "digits.tflite")
        val classifier = LiteRtDigitClassifier.fromModelBytes(modelBytes)
        val reader = OpenCvSudokuReader(classifier)

        val blank = Mat(200, 200, CvType.CV_8UC1, Scalar(255.0))
        val encoded = MatOfByte()
        Imgcodecs.imencode(".png", blank, encoded, MatOfInt())
        blank.release()
        val bytes = encoded.toArray()
        encoded.release()

        val outcome = reader.read(bytes)

        assertEquals(ReadOutcome.BoardNotFound, outcome)
    }
}
