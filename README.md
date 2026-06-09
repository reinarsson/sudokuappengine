# Sudoku Engine

A standalone, **pure-Kotlin** Sudoku-solving library. Input is a 9×9 integer grid; output is a
solve result. No third-party dependencies, no platform APIs — written KMP-safe so promotion to
Kotlin Multiplatform is cheap later.

> Computer vision, the digit model, and the app live in **other** repos and are out of scope
> here. See [`docs/SPEC.md`](docs/SPEC.md) for the full contract and [`CLAUDE.md`](CLAUDE.md) for
> the standing constraints.

## Usage

```kotlin
import com.sudokuengine.SudokuSolver
import com.sudokuengine.SolveResult

val solver = SudokuSolver.create()

val puzzle: Array<IntArray> = arrayOf(
    intArrayOf(5, 3, 0, 0, 7, 0, 0, 0, 0),
    intArrayOf(6, 0, 0, 1, 9, 5, 0, 0, 0),
    intArrayOf(0, 9, 8, 0, 0, 0, 0, 6, 0),
    intArrayOf(8, 0, 0, 0, 6, 0, 0, 0, 3),
    intArrayOf(4, 0, 0, 8, 0, 3, 0, 0, 1),
    intArrayOf(7, 0, 0, 0, 2, 0, 0, 0, 6),
    intArrayOf(0, 6, 0, 0, 0, 0, 2, 8, 0),
    intArrayOf(0, 0, 0, 4, 1, 9, 0, 0, 5),
    intArrayOf(0, 0, 0, 0, 8, 0, 0, 7, 9),
)

when (val result = solver.solve(puzzle)) {
    is SolveResult.Solved -> println(result.grid.joinToString("\n") { it.joinToString("") })
    SolveResult.Unsolvable -> println("No solution exists")
    is SolveResult.Invalid -> println("Bad input: ${result.reason}")
}

// Quality signal for OCR pipelines: a correctly-read puzzle has exactly one solution.
val unique: Boolean = solver.hasUniqueSolution(puzzle)
```

`0` marks an empty cell; `1..9` are givens. The input grid is never mutated, and identical input
always produces identical output.

## API

- `SudokuSolver.create()` — returns the default solver (bitmask + Minimum-Remaining-Values
  backtracking).
- `solve(puzzle): SolveResult` — `Solved(grid)`, `Unsolvable`, or `Invalid(reason)`.
- `hasUniqueSolution(puzzle): Boolean` — `true` iff exactly one completion exists.

## Build & test

| Task     | Command                  |
| -------- | ------------------------ |
| Build    | `./gradlew build`        |
| Test     | `./gradlew test`         |
| Lint     | `./gradlew ktlintCheck`  |
| Coverage | `./gradlew koverVerify`  |

Requires a JDK 21 toolchain.

## Test oracle

The golden-puzzle fixture `src/test/resources/golden_puzzles.json` is produced **externally** by
the independent Python `pulp` solver and committed by the maintainer. It is never generated or
computed here. `GoldenPuzzlesTest` skips with a reminder when the fixture is absent and activates
automatically once it is committed. Expected format — a JSON array of objects, each with a
`puzzle` and a `solution`, where each is either an 81-character string (row-major; `0`/`.` for
blanks) or a 9×9 array of integers.
