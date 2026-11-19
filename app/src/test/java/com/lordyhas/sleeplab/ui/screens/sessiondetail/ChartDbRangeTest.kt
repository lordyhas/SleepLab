package com.lordyhas.sleeplab.ui.screens.sessiondetail

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartDbRangeTest {
    @Test
    fun `a quiet night with a 35 dB threshold fits from 0 to 50`() {
        assertEquals(0f to 50f, chartDbRange(listOf(5f, 7f, 10f, 35f, 44f)))
    }

    @Test
    fun `the range is at least 20 dB wide`() {
        assertEquals(40f to 60f, chartDbRange(listOf(44f, 46f)))
    }

    @Test
    fun `no values gives a default range`() {
        assertEquals(0f to 40f, chartDbRange(emptyList()))
    }
}
