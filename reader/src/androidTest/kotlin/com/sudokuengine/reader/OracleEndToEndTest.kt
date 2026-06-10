package com.sudokuengine.reader

import androidx.test.platform.app.InstrumentationRegistry
import com.sudokuengine.reader.litert.LiteRtDigitClassifier
import com.sudokuengine.reader.opencv.OpenCvSudokuReader
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

/**
 * End-to-end oracle: for each sample image under `androidTest/assets/samples`,
 * [SudokuReader.read] must return [ReadOutcome.Success] whose `grid` exactly equals the `grid`
 * array in the sibling JSON file.
 *
 * Runs against the real, committed `digits.tflite` model and the real OpenCV pipeline.
 */
class OracleEndToEndTest {
    @Test
    fun readsAllSampleBoardsExactly() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val testContext = instrumentation.context
        val appContext = instrumentation.targetContext

        val modelBytes = TestAssets.readAppAsset(appContext, "digits.tflite")
        val classifier = LiteRtDigitClassifier.fromModelBytes(modelBytes)
        val reader = OpenCvSudokuReader(classifier)

        val basenames = TestAssets.sampleBasenames(testContext)
        check(basenames.isNotEmpty()) { "No sample boards found under samples/" }

        for (basename in basenames) {
            val imageBytes = TestAssets.readTestAsset(testContext, "samples/$basename.png")
            val expected = TestAssets.expectedGrid(testContext, basename)

            val outcome = reader.read(imageBytes)
            val success =
                assertIs<ReadOutcome.Success>(outcome, "Expected Success for $basename, got $outcome")

            for (r in 0 until 9) {
                assertContentEquals(
                    expected[r],
                    success.result.grid[r],
                    "Row $r mismatch for $basename",
                )
            }
        }
    }
}
