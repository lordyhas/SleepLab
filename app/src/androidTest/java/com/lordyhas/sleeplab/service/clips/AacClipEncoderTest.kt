package com.lordyhas.sleeplab.service.clips

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class AacClipEncoderTest {

    @Test
    fun encodesPlayableAacOfTheRightLength() {
        val sampleRate = 16_000
        val seconds = 3
        val pcm = ShortArray(sampleRate * seconds) { i -> (6_000 * sin(2 * PI * 150 * i / sampleRate)).toInt().toShort() }
        val out = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "encoder-test.m4a")
        try {
            assertTrue(AacClipEncoder.encode(pcm, sampleRate, out))
            assertTrue("file too small: ${out.length()}", out.length() > 1_000)
            // ~32 kbps → about 4 KB per second
            assertTrue("file too large: ${out.length()}", out.length() < 40_000)

            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(out.path)
                assertEquals(1, extractor.trackCount)
                val format = extractor.getTrackFormat(0)
                assertEquals(MediaFormat.MIMETYPE_AUDIO_AAC, format.getString(MediaFormat.KEY_MIME))
                val durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1000
                assertEquals(seconds * 1000.0, durationMs.toDouble(), 150.0)
            } finally {
                extractor.release()
            }
        } finally {
            out.delete()
        }
    }
}
