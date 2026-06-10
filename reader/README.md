# :reader — Sudoku board reader (Android)

Turns an encoded photo/screenshot of a Sudoku board into a 9×9 integer grid. This is the
Kotlin/Android port of the Python `sudoku_reader.py` pipeline; it depends on native **OpenCV** and
**LiteRT**, so — unlike the solver — it is an Android module, not pure-Kotlin/KMP. The full contract
is in [`../docs/SPEC.md`](../docs/SPEC.md).

> **Status:** implemented. The pipeline (findBoard → splitCells → preprocessCell → classify →
> assemble) is in place, backed by OpenCV and LiteRT adapters, with unit tests for the
> non-vision logic and an instrumented end-to-end oracle test.

## Build & test

This is a self-contained Gradle build, run from the repo root with `-p reader`:

```bash
./gradlew -p reader ktlintCheck        # lint
./gradlew -p reader assembleDebug      # build the library
./gradlew -p reader testDebugUnitTest  # pure-JVM unit tests (fake DigitClassifier)
./gradlew -p reader koverVerify        # coverage (≥80% on the unit-tested scope)
```

The **end-to-end oracle** is an Android *instrumented* test — it needs the LiteRT runtime, so it
runs on a device/emulator, not the JVM:

```bash
./gradlew -p reader connectedDebugAndroidTest   # requires a connected device/emulator
```

Requires the Android SDK (`ANDROID_HOME`, or `local.properties` with `sdk.dir`).

## Public contract

```kotlin
interface SudokuReader {
    fun read(image: ByteArray): ReadOutcome   // encoded JPEG/PNG bytes
}

sealed interface ReadOutcome {
    data class Success(val result: ReadResult) : ReadOutcome
    data object BoardNotFound : ReadOutcome
}

data class ReadResult(val grid: Grid, val confidence: ConfidenceGrid)
```

The pipeline talks to the model through one seam — `DigitClassifier` (a 784-float cell → a
`Prediction`) — so the assembly/confidence logic is unit-testable with a fake, no model or emulator
needed. OpenCV and LiteRT live behind adapters at the edge.

## The model asset (maintainer-supplied)

The digit model is a committed asset at **`src/main/assets/digits.tflite`**, exported from the
`mnist-mlp-pytorch` repo by `scripts/export_tflite.py` (a 784→hidden→10 MLP emitting raw logits for
classes 0–9). It is maintainer-supplied — **not generated here**. The cell handed to the model
matches what the exporter produced: **28×28, single channel, values `[0,1]`, digit white-on-black,
centred, flattened row-major to 784 floats.**

## Test fixtures

`src/androidTest/assets/samples/` holds labelled boards copied from `sudokureader/tests/samples`:
each `*.png` is paired with a sibling `*.json` whose `grid` array is the **ground-truth puzzle**. The
oracle requires **≥95% accuracy over filled cells per board** (not exact match): the finetuned model
misreads a small fraction of printed digits — the same ones the Python reference misses — so exact
100% isn't achievable. The `Success` outcome and the board-not-found / empty-cell tests stay exact.
Test-only — not bundled into the app.
