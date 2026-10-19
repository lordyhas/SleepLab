package com.lordyhas.sleeplab.service.clips

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Encodes mono PCM-16 to an AAC-LC `.m4a` file (≈ 4 KB per second at 32 kbps).
 * Blocking; call from a background thread.
 */
object AacClipEncoder {
    private const val TAG = "AacClipEncoder"
    private const val TIMEOUT_US = 10_000L
    /** Hard cap on codec round-trips so a misbehaving codec can never hang the service. */
    private const val MAX_LOOPS = 20_000

    fun encode(samples: ShortArray, sampleRate: Int, out: File, bitRate: Int = 32_000): Boolean {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val pcm = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            .also { it.asShortBuffer().put(samples) }
            .array()

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        return try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            out.parentFile?.mkdirs()
            muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val info = MediaCodec.BufferInfo()
            var track = -1
            var inputOffset = 0
            var inputDone = false
            var outputDone = false
            var loops = 0

            while (!outputDone && loops++ < MAX_LOOPS) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)!!
                        buffer.clear()
                        val chunk = min(buffer.remaining(), pcm.size - inputOffset)
                        val presentationUs = (inputOffset / 2) * 1_000_000L / sampleRate
                        if (chunk <= 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, presentationUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            buffer.put(pcm, inputOffset, chunk)
                            codec.queueInputBuffer(inIndex, 0, chunk, presentationUs, 0)
                            inputOffset += chunk
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outIndex >= 0 -> {
                        val buffer = codec.getOutputBuffer(outIndex)!!
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (info.size > 0 && muxerStarted) {
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buffer, info)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            outputDone && muxerStarted
        } catch (e: Exception) {
            Log.e(TAG, "Clip encoding failed", e)
            false
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            codec?.release()
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
            muxer?.release()
        }.also { ok -> if (!ok) out.delete() }
    }
}
