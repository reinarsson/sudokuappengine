package com.sudokuengine

/** Solves a 9×9 Sudoku puzzle. */
interface SudokuSolver {
    /**
     * Returns a [SolveResult] for the given [puzzle]. The input is never mutated.
     *
     * - [SolveResult.Solved] — a complete, valid solution (in a fresh [Grid]).
     * - [SolveResult.Unsolvable] — puzzle is rule-consistent but has no completion.
     * - [SolveResult.Invalid] — puzzle is malformed or the givens already break Sudoku rules.
     */
    fun solve(puzzle: Grid): SolveResult

    /**
     * Returns `true` iff [puzzle] has exactly one solution.
     *
     * A correctly-read Sudoku has a unique solution; multiple solutions almost always mean a
     * clue was missed or misread, so this is a cheap quality signal for an OCR pipeline.
     * A malformed or rule-violating input returns `false`.
     */
    fun hasUniqueSolution(puzzle: Grid): Boolean

    companion object {
        /** Returns the default [SudokuSolver]: bitmask + Minimum-Remaining-Values backtracking. */
        fun create(): SudokuSolver = DefaultSudokuSolver()
    }
}
