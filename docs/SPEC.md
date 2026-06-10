# Sudoku Reader — Implementation Spec (image → 9×9 matrix)

> **How to use this file:** task spec for the module that turns a photo of a Sudoku board
> into a 9×9 integer matrix. It is the Kotlin/Android port of the Python `sudoku_reader.py`
> pipeline, and it replaces the `opencv-python` and `numpy` dependencies. Repo/module-wide
> conventions live in `CLAUDE.md`; this file is the contract.

## Context & scope
This module lives in the **platform (Android) layer** — unlike the solver, it is *not*
dependency-free and *not* KMP-shared, because it depends on native OpenCV and the LiteRT
runtime. Out of scope here:
- camera capture and any UI — input is an already-encoded image (see contract);
- the solver — it lives in its own repo and is consumed downstream by the app, not by this module;
- the **confidence gate and "ask the user to retake"** decision — this module *reports*
  per-cell confidence; the app decides what to do with it.

**Operating assumption (carried over, keep it):** the photo is roughly upright and the board is
the largest object in frame. No 90°/180° orientation handling and no heavy-skew correction
beyond the perspective warp. Document this limit; don't try to exceed it in this task.

## Settled decisions
- **Placement:** an Android library module in the app project, `:reader`.
- **Input type:** encoded image **bytes** (JPEG/PNG, OpenCV-decodable) — *not* an
  `android.graphics.Bitmap` — so the orchestration is testable without an emulator.

## Dependencies — allowed here (the opposite of the solver rule)
- **OpenCV Android SDK** — replaces `opencv-python`. Same C++ core, so the port is near 1:1.
- **LiteRT** (Google AI Edge) — runs the digit model.
- Nothing else without human approval. Keep these two **at the edges** (adapters), behind the
  ports below, so the orchestration logic stays testable.

## What replaces what
- `opencv-python` → OpenCV Android SDK (`Imgproc.*` / `Core.*` on `Mat`).
- `numpy` → plain Kotlin arrays (`IntArray`, `FloatArray`, `Array<IntArray>`) plus the
  `FloatBuffer`/`ByteBuffer` that feeds LiteRT. No ndarray library.

## Public contract
```kotlin
/** 9x9 grid, 0 = empty, 1..9 = filled. Same type the solver consumes. */
typealias Grid = Array<IntArray>

/** Per-cell read confidence, 0f..1f; 0f for empty cells. */
typealias ConfidenceGrid = Array<FloatArray>

data class ReadResult(val grid: Grid, val confidence: ConfidenceGrid)

sealed interface ReadOutcome {
    data class Success(val result: ReadResult) : ReadOutcome
    /** No Sudoku board could be located in the image. */
    data object BoardNotFound : ReadOutcome
}

interface SudokuReader {
    /** @param image encoded JPEG/PNG bytes, decodable by OpenCV. */
    fun read(image: ByteArray): ReadOutcome
}
```

### Recommended internal seams (ports + adapters)
```kotlin
data class Prediction(val digit: Int, val confidence: Float)   // digit in 1..9

/** The one clean seam: a 784-float cell ([0,1], white-on-black) -> a digit. */
interface DigitClassifier {
    fun classify(cell: FloatArray): Prediction
}
```
Implement `DigitClassifier` with a LiteRT adapter. Keeping it an interface lets the assembly
and confidence logic be unit-tested with a **fake** classifier, no model or emulator needed.

## Pipeline stages (port of `sudoku_reader.py`)
Mirror the Python stages and **reuse its parameter values** — don't reinvent them:
1. **findBoard** — grayscale → Gaussian blur → `adaptiveThreshold` (inverse, block 11, C 2) →
   `morphologyEx` close → `findContours` (external) → largest by area → `approxPolyDP` →
   order 4 corners → `getPerspectiveTransform` + `warpPerspective` to a square. Fallback to the
   contour's bounding box. No board found → `BoardNotFound`.
2. **splitCells** — slice the square into 81 equal cells.
3. **preprocessCell** — trim a ~12% margin → Otsu threshold → empty check by ink fraction in
   the centre (empty → leave cell 0, never call the model) → largest contour bbox → centre on a
   28×28 canvas → normalise to `[0,1]`, white-on-black.
4. **classify** — feed each non-empty 784-float cell to `DigitClassifier`; record digit + confidence.
5. **assemble** — build the 9×9 `Grid` and the matching `ConfidenceGrid`.

## Model & input contract (the #1 footgun)
- The model is a committed asset at **`src/main/assets/digits.tflite`**. It is exported by the
  `mnist-mlp-pytorch` repo (`convert_to_litert.py`, run by the maintainer) and committed into
  this module. It is bundled into the app via the assets folder and loaded once at startup.
  Agents must **not** train, convert, fetch, or fabricate it.
- The cell handed to the model **must** match what `convert_to_litert.py` exported and the Python
  `digit_io.py`: **28×28, single channel, values [0,1], digit white on black, centred.** Encode
  the label mapping (`decode`: model index → 1..9) in one place and use it for inference; it must
  equal what training used. A mismatch here produces "splits fine, every digit wrong."

## Assets & fixtures — committed, maintainer-supplied (agents must not generate these)
- **`src/main/assets/digits.tflite`** — the model. Ships in the app *and* is loaded by the oracle
  test, so the test exercises the exact artifact that ships. Committed (a tiny MLP; plain git is
  fine — no LFS needed).
- **`src/androidTest/assets/samples/`** — the labelled board set, copied from
  `sudokureader/tests/samples`. Each image is paired with a sibling JSON of the same basename:
  ```
  samples/board01.png   samples/board01.json
  samples/board02.png   samples/board02.json
  ```
  Each `*.json` holds the expected result for that image in this format:
  ```json
  {
    "grid": [[5,3,0,0,7,0,0,0,0], "... 9 rows of 9 ints ...", [0,0,0,0,8,0,0,7,9]],
    "note": "0 = empty cell, 1-9 = digit. Row-major, top-left to bottom-right."
  }
  ```
  `grid` is a 9×9 array of ints (`0` = empty), produced by the trusted Python pipeline; it
  deserialises directly into the expected `Grid` with no string parsing. Ignore `note` — it's a
  human comment. Test-only data: it is **not** bundled into the app. Keep the set small and
  downscaled — ~10–20 representative boards.

## Performance target
Whole pipeline (detect → preprocess → 81 cells inferred → assemble) comfortably under ~1 s on a
mid-range phone; no perceptible lag in the app.

## Tests — required
1. **End-to-end oracle (primary).** For each `samples/*.png`, `read(bytes)` returns `Success`
   whose `grid` **exactly** matches the `grid` array in the sibling `*.json`. This is the real
   correctness gate. Runs as an **Android instrumented test** (OpenCV + LiteRT need the runtime);
   it loads the bundled `digits.tflite` and the `androidTest` sample assets, pairing each image
   with its JSON by basename.
2. **Board-not-found.** An image with no board → `BoardNotFound`.
3. **Empty-cell handling.** Cells the Python pipeline marks empty come back as `0`, and the model
   is never invoked on them.
4. **Assembly logic (unit, with a fake `DigitClassifier`).** Given canned predictions, the `Grid`
   and `ConfidenceGrid` are assembled correctly — pure JVM, no model/emulator needed.
5. **Confidence surfaced.** `confidence` is populated for filled cells, `0f` for empty.
6. *(Optional, diagnostic)* **Per-cell parity.** Preprocessed 28×28 cells match the Python
   `preprocess_cell` output within a tolerance (mean abs error), to localise drift. **Not**
   pixel-exact — OpenCV results differ slightly across versions/platforms, so the *matrix* is the
   exact oracle, the *images* are compared within tolerance.

## Architecture & module setup
- Hexagonal: orchestrator depends on the `DigitClassifier` port; OpenCV and LiteRT are adapters
  at the edge. No `android.graphics.*` in the orchestrator — input is bytes, output is the result.
- No camera, no UI, no solver code in this module.
- OpenCV native libs and the `assets/` wiring are set up by a single setup step; subsequent agents
  don't re-do build setup.

## Definition of done
- [ ] `SudokuReader.read` implemented to the contract; `BoardNotFound` handled.
- [ ] OpenCV stages mirror `sudoku_reader.py` and reuse its parameters.
- [ ] Model loaded from `src/main/assets/digits.tflite`; input contract (28×28, 1-channel, [0,1],
      white-on-black, `decode` mapping) matches `convert_to_litert.py` / `digit_io.py`.
- [ ] End-to-end oracle passes on the committed `src/androidTest/assets/samples/` set; assembly
      unit tests pass with a fake classifier.
- [ ] Empty cells resolve to `0` without invoking the model; confidence surfaced per cell.
- [ ] OpenCV/LiteRT confined to adapters; no UI/camera/solver code; only the two approved
      dependencies.
