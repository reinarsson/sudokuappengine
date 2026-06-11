# :app — Sudoku app (Android)

The Compose Android application: pick or capture a photo of a Sudoku board, read it via
`:reader`, solve it via the root solver, and display the solved board. The full contract is in
[`../docs/APP_SPEC.md`](../docs/APP_SPEC.md).

> **Status:** scaffold only. This module currently builds a standalone "Hello Sudoku" Compose
> screen to prove the AGP/Kotlin/Compose wiring. It is **not yet wired up** to `:reader` or the
> root solver — that's a follow-up PR. See `docs/APP_SPEC.md` for the planned pipeline and
> architecture.

## Build & test

This is a self-contained Gradle build, run from the repo root with `-p app`:

```bash
./gradlew -p app ktlintCheck         # lint
./gradlew -p app assembleDebug       # build the app
./gradlew -p app testDebugUnitTest   # pure-JVM unit tests
./gradlew -p app koverVerify         # coverage (>=80%)
```

Requires the Android SDK (`ANDROID_HOME`, or `local.properties` with `sdk.dir`).

## Module layout (planned)

- `MainActivity.kt` — single-activity Compose entry point.
- `ui/` — Compose screens and the `SudokuGrid` composable for rendering a solved board.
- `pipeline/` — small testable orchestration class wrapping `SudokuReader.read()` and
  `SudokuSolver.solve()`.

See [`../docs/APP_SPEC.md`](../docs/APP_SPEC.md) for the full contract, pipeline, and definition
of done.
