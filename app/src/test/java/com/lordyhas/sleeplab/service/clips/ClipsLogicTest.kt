package com.lordyhas.sleeplab.service.clips

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PcmRingBufferTest {

    private fun ramp(from: Int, count: Int) = ShortArray(count) { (from + it).toShort() }

    @Test
    fun `reads back a range that is still buffered`() {
        val ring = PcmRingBuffer(capacity = 10)
        ring.write(ramp(0, 6))
        assertArrayEquals(ramp(2, 3), ring.read(2, 5))
    }

    @Test
    fun `wraps around and keeps only the newest samples`() {
        val ring = PcmRingBuffer(capacity = 10)
        ring.write(ramp(0, 25))
        assertEquals(15L, ring.oldestAvailable)
        assertArrayEquals(ramp(20, 5), ring.read(20, 25))
    }

    @Test
    fun `start older than the buffer is clamped`() {
        val ring = PcmRingBuffer(capacity = 10)
        ring.write(ramp(0, 25))
        assertArrayEquals(ramp(15, 10), ring.read(0, 25))
    }

    @Test
    fun `range not yet written or fully overwritten returns null`() {
        val ring = PcmRingBuffer(capacity = 10)
        ring.write(ramp(0, 25))
        assertNull(ring.read(20, 30))
        assertNull(ring.read(0, 10))
    }
}

class ClipBudgetTest {

    @Test
    fun `accepts well spaced clips until full`() {
        val budget = ClipBudget(maxClips = 3, minSpacingMs = 1_000)
        repeat(3) { assertEquals(ClipBudget.Decision.Accept(emptyList()), budget.offer(it * 2_000L, 50f)) }
        assertEquals(3, budget.size)
    }

    @Test
    fun `when full only a louder snore gets in and evicts the quietest`() {
        val budget = ClipBudget(maxClips = 2, minSpacingMs = 1_000)
        budget.offer(0, 50f)
        budget.offer(10_000, 60f)
        assertEquals(ClipBudget.Decision.Reject, budget.offer(20_000, 45f))
        assertEquals(ClipBudget.Decision.Accept(listOf(0L)), budget.offer(30_000, 55f))
        assertEquals(2, budget.size)
    }

    @Test
    fun `clips too close together compete and the louder wins`() {
        val budget = ClipBudget(maxClips = 10, minSpacingMs = 30_000)
        budget.offer(0, 50f)
        assertEquals(ClipBudget.Decision.Reject, budget.offer(5_000, 48f))
        assertEquals(ClipBudget.Decision.Accept(listOf(0L)), budget.offer(10_000, 58f))
        assertEquals(1, budget.size)
    }

    @Test
    fun `a louder snore between two kept clips replaces both`() {
        val budget = ClipBudget(maxClips = 10, minSpacingMs = 30_000)
        budget.offer(0, 50f)
        budget.offer(40_000, 52f)
        assertEquals(ClipBudget.Decision.Accept(listOf(0L, 40_000L)), budget.offer(20_000, 60f))
        assertEquals(1, budget.size)
    }
}
