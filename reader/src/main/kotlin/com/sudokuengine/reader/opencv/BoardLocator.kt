package com.sudokuengine.reader.opencv

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * Locates a Sudoku board in a grayscale image and warps it to a clean square.
 *
 * Direct port of `find_board()` / `_order_corners()` from `sudoku_reader.py`. Reuses the same
 * tunables: Gaussian blur `(5,5)`, adaptive threshold (inverse, block 11, C 2), 3x3 rectangular
 * close, largest external contour, `approxPolyDP` at 2% of perimeter, falling back to the
 * bounding box if the approximation isn't a quadrilateral.
 */
internal object BoardLocator {
    /**
     * Finds the board in [gray] and warps it to a `(9 * cellPx)` square.
     *
     * @param gray a single-channel (grayscale) [Mat].
     * @param cellPx pixels per cell after warping (the board side is `9 * cellPx`).
     * @return the warped, single-channel board [Mat], or `null` if no contours were found
     *   (mirrors the Python pipeline raising when the image has no usable contours).
     */
    fun findBoard(
        gray: Mat,
        cellPx: Int,
    ): Mat? {
        val side = (9 * cellPx).toDouble()

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            blurred,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0,
        )
        blurred.release()

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresh,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE,
        )
        thresh.release()
        hierarchy.release()

        if (contours.isEmpty()) {
            return null
        }

        val biggest = contours.maxBy { Imgproc.contourArea(it) }

        val biggest2f = MatOfPoint2f(*biggest.toArray())
        val peri = Imgproc.arcLength(biggest2f, true)
        val approx2f = MatOfPoint2f()
        Imgproc.approxPolyDP(biggest2f, approx2f, CellTuning.BOARD_APPROX_EPSILON_FRAC * peri, true)
        biggest2f.release()

        val dst =
            MatOfPoint2f(
                Point(0.0, 0.0),
                Point(side, 0.0),
                Point(side, side),
                Point(0.0, side),
            )

        val src =
            if (approx2f.toArray().size == 4) {
                orderCorners(approx2f.toArray())
            } else {
                val rect = Imgproc.boundingRect(biggest)
                MatOfPoint2f(
                    Point(rect.x.toDouble(), rect.y.toDouble()),
                    Point((rect.x + rect.width).toDouble(), rect.y.toDouble()),
                    Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                    Point(rect.x.toDouble(), (rect.y + rect.height).toDouble()),
                )
            }
        approx2f.release()
        for (c in contours) c.release()

        val transform = Imgproc.getPerspectiveTransform(src, dst)
        src.release()
        dst.release()

        val board = Mat()
        Imgproc.warpPerspective(gray, board, transform, Size(side, side))
        transform.release()

        return board
    }

    /**
     * Orders 4 corner points as top-left, top-right, bottom-right, bottom-left, matching
     * `_order_corners()`: TL = min(x+y), BR = max(x+y), TR = min(y-x), BL = max(y-x).
     */
    private fun orderCorners(points: Array<Point>): MatOfPoint2f {
        val tl = points.minBy { it.x + it.y }
        val br = points.maxBy { it.x + it.y }
        val tr = points.minBy { it.y - it.x }
        val bl = points.maxBy { it.y - it.x }
        return MatOfPoint2f(tl, tr, br, bl)
    }

    /**
     * Slices [board] into a row-major list of 81 equal cell [Mat]s ([cellPx] x [cellPx] each).
     * Mirrors `split_cells()`.
     */
    fun splitCells(
        board: Mat,
        cellPx: Int,
    ): List<Mat> {
        val cells = mutableListOf<Mat>()
        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val roi =
                    org.opencv.core.Rect(
                        c * cellPx,
                        r * cellPx,
                        cellPx,
                        cellPx,
                    )
                cells += Mat(board, roi).clone()
            }
        }
        return cells
    }
}
