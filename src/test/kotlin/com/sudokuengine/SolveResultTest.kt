package com.sudokuengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SolveResultTest {
    @Test
    fun `Invalid stores reason`() {
        val result = SolveResult.Invalid("duplicate in row 0")
        assertEquals("duplicate in row 0", result.reason)
    }

    @Test
    fun `Invalid equality`() {
        assertEquals(SolveResult.Invalid("a"), SolveResult.Invalid("a"))
        assertNotEquals(SolveResult.Invalid("a"), SolveResult.Invalid("b"))
    }

    @Test
    fun `Invalid hashCode is consistent with equals`() {
        val r1 = SolveResult.Invalid("a")
        val r2 = SolveResult.Invalid("a")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun `Invalid toString contains reason`() {
        assertTrue(SolveResult.Invalid("oops").toString().contains("oops"))
    }

    @Test
    fun `Invalid copy produces equal instance`() {
        val original = SolveResult.Invalid("original")
        assertEquals(original, original.copy())
        assertEquals(SolveResult.Invalid("modified"), original.copy(reason = "modified"))
    }

    @Test
    fun `Invalid destructuring yields reason`() {
        val (reason) = SolveResult.Invalid("grid is 8x9")
        assertEquals("grid is 8x9", reason)
    }

    @Test
    fun `Unsolvable is a singleton`() {
        assertSame(SolveResult.Unsolvable, SolveResult.Unsolvable)
    }

    @Test
    fun `Unsolvable toString is non-null`() {
        assertNotNull(SolveResult.Unsolvable.toString())
    }

    @Test
    fun `Solved stores grid`() {
        val grid: Grid = Array(9) { IntArray(9) }
        val result = SolveResult.Solved(grid)
        assertSame(grid, result.grid)
    }

    @Test
    fun `Solved equality`() {
        val grid: Grid = Array(9) { IntArray(9) }
        assertEquals(SolveResult.Solved(grid), SolveResult.Solved(grid))
        assertNotEquals(SolveResult.Solved(Array(9) { IntArray(9) }), SolveResult.Solved(Array(9) { IntArray(9) }))
    }

    @Test
    fun `Solved hashCode is consistent with equals`() {
        val grid: Grid = Array(9) { IntArray(9) }
        assertEquals(SolveResult.Solved(grid).hashCode(), SolveResult.Solved(grid).hashCode())
    }

    @Test
    fun `Solved toString is non-null`() {
        val grid: Grid = Array(9) { IntArray(9) }
        assertNotNull(SolveResult.Solved(grid).toString())
    }

    @Test
    fun `Solved copy produces equal instance`() {
        val grid: Grid = Array(9) { IntArray(9) }
        val original = SolveResult.Solved(grid)
        assertEquals(original, original.copy())
    }

    @Test
    fun `Solved destructuring yields grid`() {
        val grid: Grid = Array(9) { IntArray(9) }
        val (g) = SolveResult.Solved(grid)
        assertSame(grid, g)
    }
}
