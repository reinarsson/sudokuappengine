package com.sudokuengine.reader

/**
 * The one clean seam between the pipeline and the digit model: a single preprocessed cell in, a
 * [Prediction] out.
 *
 * Keeping this an interface lets the assembly and confidence logic be unit-tested with a fake
 * classifier — no model or emulator needed. The production implementation is a LiteRT adapter.
 */
interface DigitClassifier {
    /**
     * Classifies one preprocessed cell.
     *
     * @param cell a 784-element ([MODEL_SIZE]×[MODEL_SIZE]) float array, values in `[0,1]`, digit
     *   white-on-black and centred — matching what the model was trained and exported on.
     * @return the predicted digit (`1..9`) and its confidence.
     */
    fun classify(cell: FloatArray): Prediction

    companion object {
        /** Side length, in pixels, of the square cell the model expects (MNIST-style 28×28). */
        const val MODEL_SIZE: Int = 28
    }
}
