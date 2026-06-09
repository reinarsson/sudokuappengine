package com.sudokuengine

/** Solves a 9×9 Sudoku puzzle. */
interface SudokuSolver {
    /**
     * Returns a [SolveResult] for the given [puzzle].
     *
     * - [SolveResult.Solved] — a complete, valid solution (input is not mutated).
     * - [SolveResult.Unsolvable] — puzzle is rule-consistent but has no completion.
     * - [SolveResult.Invalid] — puzzle is malformed or the givens already break Sudoku rules.
     */
    fun solve(puzzle: Grid): SolveResult = throw NotImplementedError("Solver not yet implemented")
}
