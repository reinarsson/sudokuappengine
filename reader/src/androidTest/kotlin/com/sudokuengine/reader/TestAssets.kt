package com.sudokuengine.reader

import android.content.Context
import org.json.JSONObject

/**
 * Test-only helpers for loading the bundled model and the labelled sample boards from
 * `androidTest` assets.
 */
internal object TestAssets {
    /** Reads the bytes of an asset under `src/main/assets` (e.g. `digits.tflite`). */
    fun readAppAsset(
        context: Context,
        name: String,
    ): ByteArray = context.assets.open(name).use { it.readBytes() }

    /** Reads the bytes of an asset under `src/androidTest/assets` (e.g. a sample PNG). */
    fun readTestAsset(
        context: Context,
        path: String,
    ): ByteArray = context.assets.open(path).use { it.readBytes() }

    /** Lists the basenames (without extension) of the sample images under `samples/`. */
    fun sampleBasenames(context: Context): List<String> =
        context.assets.list("samples")
            .orEmpty()
            .filter { it.endsWith(".png") }
            .map { it.removeSuffix(".png") }
            .sorted()

    /**
     * Parses the sibling `*.json` for a sample and returns its `grid` field as a [Grid].
     */
    fun expectedGrid(
        context: Context,
        basename: String,
    ): Grid {
        val json = String(readTestAsset(context, "samples/$basename.json"), Charsets.UTF_8)
        val root = JSONObject(json)
        val rows = root.getJSONArray("grid")
        return Array(rows.length()) { r ->
            val row = rows.getJSONArray(r)
            IntArray(row.length()) { c -> row.getInt(c) }
        }
    }
}
