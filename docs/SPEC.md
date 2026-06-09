# Sudoku Solver — Implementation Spec (pure Kotlin)

> **How to use this file:** this is the task spec for a standalone repository whose sole
> purpose is the Sudoku-solving engine. Repo-wide conventions (Kotlin version, formatting,
> coverage gate, build/test commands) belong in `CLAUDE.md` at the repo root; this file
> describes *what to build* and *the contract it must meet*.

## Context & scope
This repository **is** the Sudoku engine — a self-contained library. The following live in
**other** projects and are **out of scope here**; do not build, import, or assume access to
them:
- the OCR / image pipeline (OpenCV), the digit model (LiteRT), and the Android/iOS app;
- the original Python `pulp`-based solver (it serves only as the source of the test oracle —
  see *Tests*).

The engine takes a grid of integers and returns a result. Nothing else.

## Confirm before starting — one decision
Choose the target shape and **state it at the top of `CLAUDE.md`** so every agent shares the
assumption (the rest of this spec is identical either way):
- **(a) Plain Kotlin/JVM library now, written KMP-safe** so promotion is cheap later
  — *recommended default*; or
- **(b) Kotlin Multiplatform project from day one** with a `commonMain` source set.

## Hard constraints — do not violate
- **Pure Kotlin, standard library only.** Do **NOT** add any dependency — no OR-Tools, no
  Choco-solver, no ojAlgo, no other optimization/solver library. They are JVM/JNI-only and
  would permanently block iOS sharing. The ILP formulation from the Python version is not
  needed; a plain algorithm is faster here and has zero deps.
- **No platform APIs** in production code. No `java.*`, no `android.*`. Must be
  `commonMain`-compatible.
- **Tests use `kotlin.test`**, not JVM-only JUnit APIs, so they too stay multiplatform-ready.
- **No knowledge of the outside world.** The engine knows nothing about images, OCR, the
  model, or any app. Input is a grid of integers; output is a result.
- **Deterministic.** Identical input always produces identical output (required for the golden
  tests). Use a fixed cell-ordering, not randomness.
- **Pure / non-mutating.** Never mutate the caller's input grid. Return a new array.
- **Stateless across calls.** A single solver instance must be safe to reuse repeatedly.

## Public contract
```kotlin
/**
 * A 9x9 Sudoku grid. Outer index = row (0..8), inner index = column (0..8).
 * 0 = empty cell; 1..9 = a filled digit.
 */
typealias Grid = Array<IntArray>

sealed interface SolveResult {
    /** A complete, valid solution. [grid] is a fresh array; the input is left untouched. */
    data class Solved(val grid: Grid) : SolveResult

    /** Input is well-formed and rule-consistent, but no completion exists. */
    data object Unsolvable : SolveResult

    /** Input is malformed, or the givens already break Sudoku rules (e.g. an OCR misread). */
    data class Invalid(val reason: String) : SolveResult
}

interface SudokuSolver {
    fun solve(puzzle: Grid): SolveResult
}
```

**Why a sealed result, not a nullable `Grid?`:** the OCR pipeline (elsewhere) can hand the
solver a grid that is internally contradictory — a misread digit duplicated in a row. The app
needs to tell three cases apart: "the photo was read wrong" (`Invalid`), "this puzzle genuinely
has no solution" (`Unsolvable`), and success (`Solved`). A bare `Grid?` collapses the first two.

### Optional but recommended — ambiguity detection
```kotlin
/** True iff the puzzle has exactly one solution. Used to flag likely misreads. */
fun hasUniqueSolution(puzzle: Grid): Boolean
```
A correctly-read Sudoku has exactly one solution. If a puzzle has *multiple* solutions, a clue
was almost certainly missed or misread — a cheap quality signal for the pipeline. Implement by
searching for a second solution and stopping as soon as two are found.

## Recommended algorithm
**Backtracking with constraint propagation** (bitmask + Minimum Remaining Values):
- Track used digits per row, per column, and per 3×3 box as 9-bit `Int` bitmasks, for O(1)
  legality checks and updates.
- Candidates for an empty cell: `(0x1FF) and (rowMask or colMask or boxMask).inv()`.
- Choose the next cell to fill by **Minimum Remaining Values** (fewest candidates first). This
  heuristic alone makes even hard puzzles solve in microseconds.
- Propagate forced cells (exactly one candidate) before branching; recurse; undo on dead ends.

No library is needed and this solves any 9×9 far faster than perceptible. An exact-cover /
Dancing Links (Algorithm X) implementation is acceptable under the same contract, but default
to bitmask + MRV backtracking — it is simpler and plenty fast.

## Validation pre-check
Before searching, validate the input and return `Invalid(reason)` when:
- the grid is not 9×9, or any value is outside `0..9`; or
- any non-zero given duplicates another in its row, column, or 3×3 box.

This one cheap pass catches the most common OCR failure before any search runs.

## Performance target
Solve any valid 9×9 — including known hard instances — in well under 10 ms on a mid-range
phone (in practice sub-millisecond). No perceptible delay in the app.

## Tests — required (use `kotlin.test`)
1. **Oracle / golden tests.** Load a fixture of `(puzzle, solution)` pairs from a committed
   test resource (e.g. `src/test/resources/golden_puzzles.json`) and assert each `Solved.grid`
   matches the expected solution exactly.
   **This fixture is supplied externally: it is produced by the independent Python `pulp`
   solver and committed into the repo by the maintainer. Do NOT generate, fabricate, or compute
   these solutions — deriving them from the solver under test (or inventing them) makes the test
   circular and worthless. If the fixture file is missing, still write the test to load that
   path, and leave it pending/failing until the file is added; never substitute invented data.**
2. **Already solved.** A complete valid grid returns `Solved` equal to itself.
3. **Invalid input.** Duplicate given → `Invalid`; wrong dimensions → `Invalid`; out-of-range
   value → `Invalid`.
4. **Unsolvable.** A rule-consistent but uncompletable grid → `Unsolvable`.
5. **Hard cases.** A handful of known worst-case-style puzzles, confirming the search stays
   fast and correct. (These may also come from the external fixture.)
6. **Input immutability.** After `solve`, the caller's input array is unchanged.
7. **Determinism.** Solving the same puzzle twice yields identical output.
8. **Uniqueness** *(if implemented)*. A proper puzzle reports `true`; the same puzzle with one
   clue removed (now ambiguous) reports `false`.

> **Empty grid note:** an all-zeros grid has many valid solutions, so do **not** assert a
> specific solution for it — assert only that the result is a valid completed grid.

## Repository setup
- This repo *is* the engine; set it up as a standalone Kotlin library built with Gradle (Kotlin
  DSL), per the decision recorded in `CLAUDE.md`.
- **One agent scaffolds first.** The Gradle wrapper, build files, source-set layout, and CI
  must be created and committed by a single setup task **before** parallel work begins. Do not
  let multiple agents each scaffold the build — they will conflict.
- Keep the public surface to exactly the types in *Public contract*; mark everything else
  `internal`.

## Definition of done
- [ ] `SudokuSolver.solve` implemented per contract, pure Kotlin, zero third-party dependencies.
- [ ] Input validation returns `Invalid` with a clear, human-readable reason.
- [ ] All required tests pass (using `kotlin.test`), including the externally-supplied PuLP
      oracle fixture.
- [ ] Caller's input grid is never mutated; output is deterministic.
- [ ] No `java.*` / `android.*` / platform imports anywhere in production code.
- [ ] Build and test commands documented in `README.md` / `CLAUDE.md`.
