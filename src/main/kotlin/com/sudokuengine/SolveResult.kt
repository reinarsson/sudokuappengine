package com.sudokuengine

/** Result of a solve attempt. */
sealed interface SolveResult {
    /** A complete, valid solution. [grid] is a fresh array; the input is left untouched. */
    data class Solved(val grid: Grid) : SolveResult

    /** Input is well-formed and rule-consistent, but no completion exists. */
    data object Unsolvable : SolveResult

    /** Input is malformed, or the givens already break Sudoku rules (e.g. an OCR misread). */
    data class Invalid(val reason: String) : SolveResult
}
