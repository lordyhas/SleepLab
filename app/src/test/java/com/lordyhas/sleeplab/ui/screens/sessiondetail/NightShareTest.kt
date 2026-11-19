package com.lordyhas.sleeplab.ui.screens.sessiondetail

import org.junit.Assert.assertEquals
import org.junit.Test

class NightShareTest {
    @Test
    fun `a few minutes of a long night never reads as zero`() {
        assertEquals("<1%", nightShare(snoreSeconds = 300, nightSeconds = 36_180))
    }

    @Test
    fun `whole percents otherwise`() {
        assertEquals("25%", nightShare(snoreSeconds = 900, nightSeconds = 3_600))
        assertEquals("0%", nightShare(snoreSeconds = 0, nightSeconds = 3_600))
        assertEquals("0%", nightShare(snoreSeconds = 10, nightSeconds = 0))
    }
}
