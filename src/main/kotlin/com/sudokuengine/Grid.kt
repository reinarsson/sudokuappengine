package com.sudokuengine

/**
 * A 9×9 Sudoku grid. Outer index = row (0..8), inner index = column (0..8).
 * 0 = empty cell; 1..9 = a filled digit.
 */
typealias Grid = Array<IntArray>
