package com.lordyhas.sleeplab.service.clips

/**
 * Fixed-size circular buffer of PCM-16 samples addressed by absolute sample index
 * (0 = first sample ever written). Lets the analyzer cut a clip that starts *before* the moment
 * a snore was confirmed.
 */
class PcmRingBuffer(private val capacity: Int) {
    private val buffer = ShortArray(capacity)

    /** Absolute index of the next sample to be written (= total samples written). */
    var writtenSamples: Long = 0L
        private set

    /** Oldest absolute index still available. */
    val oldestAvailable: Long
        get() = maxOf(0L, writtenSamples - capacity)

    fun write(samples: ShortArray, offset: Int = 0, count: Int = samples.size) {
        for (i in offset until offset + count) {
            buffer[(writtenSamples % capacity).toInt()] = samples[i]
            writtenSamples++
        }
    }

    /**
     * Copies samples in [from, to). The start is clamped to what is still buffered;
     * returns null if nothing of the range is available or the end hasn't been written yet.
     */
    fun read(from: Long, to: Long): ShortArray? {
        if (to > writtenSamples || to <= from) return null
        val start = maxOf(from, oldestAvailable)
        if (start >= to) return null
        return ShortArray((to - start).toInt()) { i -> buffer[((start + i) % capacity).toInt()] }
    }
}
