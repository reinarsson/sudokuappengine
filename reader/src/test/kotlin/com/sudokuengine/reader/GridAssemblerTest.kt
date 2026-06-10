package com.sudokuengine.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridAssemblerTest {
    @Test
    fun `assembles grid and confidence from canned predictions`() {
        // 81 entries, row-major. Cell (0,0) = digit 5 conf 0.9, cell (0,1) empty, rest empty.
        val predictions = MutableList<Prediction?>(81) { null }
        predictions[0] = Prediction(digit = 5, confidence = 0.9f)
        predictions[8] = Prediction(digit = 3, confidence = 0.75f)
        predictions[80] = Prediction(digit = 9, confidence = 0.5f)

        val result = GridAssembler.assemble(predictions)

        assertEquals(5, result.grid[0][0])
        assertEquals(3, result.grid[0][8])
        assertEquals(9, result.grid[8][8])
        assertEquals(0, result.grid[0][1])

        assertEquals(0.9f, result.confidence[0][0])
        assertEquals(0.75f, result.confidence[0][8])
        assertEquals(0.5f, result.confidence[8][8])
        assertEquals(0f, result.confidence[0][1])
    }

    @Test
    fun `empty predictions assemble to all-zero grid and confidence`() {
        val predictions = List<Prediction?>(81) { null }

        val result = GridAssembler.assemble(predictions)

        for (row in result.grid) {
            for (value in row) {
                assertEquals(0, value)
            }
        }
        for (row in result.confidence) {
            for (value in row) {
                assertEquals(0f, value)
            }
        }
    }

    @Test
    fun `wrong-sized prediction list throws`() {
        assertFailsWith<IllegalArgumentException> {
            GridAssembler.assemble(List(80) { null })
        }
    }

    @Test
    fun `grid dimensions are 9 by 9`() {
        val result = GridAssembler.assemble(List(81) { null })

        assertEquals(9, result.grid.size)
        for (row in result.grid) {
            assertEquals(9, row.size)
        }
        assertEquals(9, result.confidence.size)
        for (row in result.confidence) {
            assertEquals(9, row.size)
        }
    }
}
