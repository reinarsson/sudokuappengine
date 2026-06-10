package com.sudokuengine.reader.opencv

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Tunables ported verbatim from `sudoku_reader.py` — do not retune here.
 */
internal object CellTuning {
    /** Pixels per cell after the board is warped to a `9 * CELL_PX` square. */
    const val CELL_PX: Int = 50

    /** Fraction of each cell's border trimmed off before thresholding. */
    const val CELL_MARGIN: Double = 0.12

    /** Minimum fraction of "ink" in the central region for a cell to be considered non-empty. */
    const val EMPTY_INK_FRAC: Double = 0.015

    /** Below this inner-patch standard deviation, a cell is treated as empty (no Otsu). */
    const val EMPTY_STD_MAX: Double = 8.0

    /** Side length, in pixels, of the square the digit model expects. */
    const val MODEL_SIZE: Int = 28

    /** Blank border kept around the centred digit, in [MODEL_SIZE] units. */
    const val DIGIT_PAD: Int = 4

    /**
     * Minimum fraction of the thresholded cell's area a contour's bounding box must cover to be
     * treated as a digit (rather than noise). Ported from `sudoku_reader.py`.
     */
    const val DIGIT_MIN_AREA_FRAC: Double = 0.02

    /**
     * Denominator for the central-region crop used by the empty-cell ink check: the region spans
     * `[cw/CENTER_REGION_DIVISOR, 4*cw/CENTER_REGION_DIVISOR)` in each dimension. Ported from
     * `sudoku_reader.py`'s `[w//5 : 4*w//5]` slicing.
     */
    const val CENTER_REGION_DIVISOR: Int = 5

    /**
     * Epsilon fraction of the contour perimeter used by `approxPolyDP` when locating the board
     * outline, mirroring the Python pipeline's `0.02 * peri`.
     */
    const val BOARD_APPROX_EPSILON_FRAC: Double = 0.02
}

/**
 * Preprocesses one cell of the warped board into the 28x28 `[0,1]` white-on-black float array the
 * digit model expects, or `null` if the cell is empty.
 *
 * Direct port of `preprocess_cell()` from `sudoku_reader.py`.
 */
internal object CellPreprocessor {
    /**
     * Preprocesses [cell] (a single-channel [Mat] slice of the warped board).
     *
     * @return a row-major [FloatArray] of length `MODEL_SIZE * MODEL_SIZE` with values in
     *   `[0,1]`, or `null` if the cell is empty (in which case the digit model must not be
     *   invoked for it).
     */
    fun preprocess(cell: Mat): FloatArray? {
        val h = cell.rows()
        val w = cell.cols()
        val m = (minOf(h, w) * CellTuning.CELL_MARGIN).toInt()

        if (h - 2 * m <= 0 || w - 2 * m <= 0) {
            return null
        }

        val inner = Mat(cell, Rect(m, m, w - 2 * m, h - 2 * m))

        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(inner, mean, stddev)
        val std = stddev.toArray()[0]
        mean.release()
        stddev.release()

        if (std < CellTuning.EMPTY_STD_MAX) {
            inner.release()
            return null
        }

        // Otsu threshold -> digit is white (255) on black.
        val th = Mat()
        Imgproc.threshold(
            inner,
            th,
            0.0,
            255.0,
            Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU,
        )
        inner.release()

        // Empty check: ink in the central region only.
        val ch = th.rows()
        val cw = th.cols()
        val d = CellTuning.CENTER_REGION_DIVISOR
        val center = Mat(th, Rect(cw / d, ch / d, cw * 4 / d - cw / d, ch * 4 / d - ch / d))
        val centerMean = Core.mean(center).`val`[0]
        center.release()
        if (centerMean / 255.0 < CellTuning.EMPTY_INK_FRAC) {
            th.release()
            return null
        }

        // Keep only the largest blob -> the digit.
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(th, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release()
        if (contours.isEmpty()) {
            th.release()
            return null
        }

        val digitContour = contours.maxBy { Imgproc.contourArea(it) }
        val bbox = Imgproc.boundingRect(digitContour)
        for (c in contours) c.release()

        if (bbox.width.toLong() * bbox.height.toLong() < (CellTuning.DIGIT_MIN_AREA_FRAC * th.rows() * th.cols())) {
            th.release()
            return null
        }

        val digit = Mat(th, bbox)

        val target = CellTuning.MODEL_SIZE - 2 * CellTuning.DIGIT_PAD
        val scale = target.toDouble() / maxOf(bbox.width, bbox.height)
        val nw = maxOf(1, (bbox.width * scale).toInt())
        val nh = maxOf(1, (bbox.height * scale).toInt())

        val resized = Mat()
        Imgproc.resize(digit, resized, Size(nw.toDouble(), nh.toDouble()), 0.0, 0.0, Imgproc.INTER_AREA)
        th.release()

        val canvas = Mat.zeros(CellTuning.MODEL_SIZE, CellTuning.MODEL_SIZE, cell.type())
        val ox = (CellTuning.MODEL_SIZE - nw) / 2
        val oy = (CellTuning.MODEL_SIZE - nh) / 2
        resized.copyTo(Mat(canvas, Rect(ox, oy, nw, nh)))
        resized.release()

        val result = matToFloatArray(canvas)
        canvas.release()
        return result
    }

    /** Converts an 8-bit single-channel [Mat] to a row-major `[0,1]` [FloatArray]. */
    private fun matToFloatArray(mat: Mat): FloatArray {
        val size = mat.rows() * mat.cols()
        val bytes = ByteArray(size)
        mat.get(0, 0, bytes)
        return FloatArray(size) { i -> (bytes[i].toInt() and 0xFF) / 255.0f }
    }
}
