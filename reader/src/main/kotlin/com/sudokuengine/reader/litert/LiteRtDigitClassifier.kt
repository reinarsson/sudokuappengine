package com.sudokuengine.reader.litert

import com.sudokuengine.reader.DigitClassifier
import com.sudokuengine.reader.Prediction
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import kotlin.math.exp

/**
 * [DigitClassifier] adapter backed by the committed `digits.tflite` model.
 *
 * Model contract (must match `../mnist-mlp-pytorch/scripts/export_tflite.py`, which reconstructs
 * the MLP from `mnist_printed.npz` — 784 -> hidden -> 10, raw logits, no softmax):
 * - input: a single `[1, 784]` float32 tensor — the row-major flatten of a
 *   [DigitClassifier.MODEL_SIZE] x [DigitClassifier.MODEL_SIZE] cell, values in `[0,1]`,
 *   white-on-black.
 * - output: a single `[1, 10]` float32 tensor of **raw logits** for classes `0..9`. The model
 *   applies no softmax; the label is simply the output index (`decode` is the identity map).
 *
 * Softmax (for [Prediction.confidence]) and argmax (for [Prediction.digit]) are applied here, in
 * this one place, so the `decode` mapping can't drift between adapter and model. The argmax is
 * over all 10 classes (0..9); empty cells are filtered upstream by `CellPreprocessor`, and a
 * Sudoku cell never contains a printed "0", so in practice [Prediction.digit] is `1..9`.
 *
 * Holds no cross-call mutable state beyond the loaded interpreter: identical input always
 * produces identical output.
 */
class LiteRtDigitClassifier private constructor(
    private val interpreter: Interpreter,
) : DigitClassifier {
    companion object {
        private const val INPUT_SIZE = DigitClassifier.MODEL_SIZE * DigitClassifier.MODEL_SIZE
        private const val OUTPUT_CLASSES = 10
        private const val BYTES_PER_FLOAT = 4

        /**
         * Creates a classifier from the raw bytes of `digits.tflite` (e.g. read from the Android
         * `assets/` folder).
         */
        fun fromModelBytes(modelBytes: ByteArray): LiteRtDigitClassifier {
            val buffer =
                ByteBuffer.allocateDirect(modelBytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(modelBytes)
                    rewind()
                }
            return LiteRtDigitClassifier(Interpreter(buffer))
        }

        /** Creates a classifier from an already-mapped model buffer. */
        fun fromMappedBuffer(modelBuffer: MappedByteBuffer): LiteRtDigitClassifier = LiteRtDigitClassifier(Interpreter(modelBuffer))
    }

    override fun classify(cell: FloatArray): Prediction {
        require(cell.size == INPUT_SIZE) {
            "Expected a $INPUT_SIZE-element cell (${DigitClassifier.MODEL_SIZE}x" +
                "${DigitClassifier.MODEL_SIZE}), got ${cell.size}"
        }

        val input =
            ByteBuffer.allocateDirect(INPUT_SIZE * BYTES_PER_FLOAT).apply {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().put(cell)
                rewind()
            }

        val output = Array(1) { FloatArray(OUTPUT_CLASSES) }
        interpreter.run(input, output)

        val logits = output[0]
        val probabilities = softmax(logits)

        var bestIndex = 0
        for (i in probabilities.indices) {
            if (probabilities[i] > probabilities[bestIndex]) {
                bestIndex = i
            }
        }

        // decode: model output index == digit (0..9), in one place.
        return Prediction(digit = bestIndex, confidence = probabilities[bestIndex])
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val exps = FloatArray(logits.size) { exp((logits[it] - max).toDouble()).toFloat() }
        val sum = exps.sum()
        return FloatArray(exps.size) { exps[it] / sum }
    }
}
