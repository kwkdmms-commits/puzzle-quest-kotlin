package com.pingsama.puzzlequest.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Re-creates the five UI sounds from `client/src/lib/audioUtils.ts`.
 *
 * The web version uses the Web Audio API to generate tones with exponential frequency
 * sweeps and exponential gain decay. We do the same here by synthesizing PCM samples
 * directly and pushing them through [AudioTrack] in MODE_STATIC.
 *
 * Each [play*] call is non-blocking — synthesis + write happen on a small background
 * executor and the AudioTrack releases itself once playback finishes.
 */
class AudioManager {

    @Volatile var enabled: Boolean = true

    private val sampleRate = 44100
    private val executor = Executors.newSingleThreadExecutor()
    private val masterVolume = 0.30f

    // ---------- public sounds ----------

    fun playSelect()  = play(SoundSpec.Sweep(600.0, 400.0, 100, masterVolume * 0.5f))
    fun playSwap()    = play(SoundSpec.Sweep(800.0, 200.0, 150, masterVolume * 0.4f))
    fun playSnap()    = play(SoundSpec.DualSweep(800.0, 600.0, 1200.0, 900.0, 300, masterVolume * 0.6f))
    fun playWin()     = play(SoundSpec.Arpeggio(listOf(523.25, 659.25, 783.99, 1046.50), 200, 100, masterVolume * 0.5f))
    fun playHint()    = play(SoundSpec.Sweep(400.0, 800.0, 300, masterVolume * 0.4f))

    fun release() {
        try { executor.shutdownNow() } catch (_: Throwable) {}
    }

    // ---------- internals ----------

    private fun play(spec: SoundSpec) {
        if (!enabled) return
        executor.execute {
            try {
                val samples = spec.synth(sampleRate)
                writeAndPlay(samples)
            } catch (t: Throwable) {
                Log.w("AudioManager", "Sound playback failed", t)
            }
        }
    }

    private fun writeAndPlay(samples: ShortArray) {
        val bytes = samples.size * 2
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.setNotificationMarkerPosition(samples.size)
        track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(t: AudioTrack) {
                try { t.stop() } catch (_: Throwable) {}
                try { t.release() } catch (_: Throwable) {}
            }
            override fun onPeriodicNotification(t: AudioTrack) {}
        })
        track.play()
    }
}

/**
 * The three flavours of synth we need (each maps to one or two of the original sounds).
 *
 * Frequency profile is an exponential sweep: f(t) = f0·(f1/f0)^(t/T)
 * Gain envelope is also exponential decay from `v` down to ~0.01 over the duration —
 * this matches Web Audio's `exponentialRampToValueAtTime(0.01, ...)`.
 */
private sealed class SoundSpec {

    abstract fun synth(sampleRate: Int): ShortArray

    /** Single oscillator with exponential frequency sweep + exponential gain decay. */
    data class Sweep(
        val f0: Double, val f1: Double, val durationMs: Int, val v: Float,
    ) : SoundSpec() {
        override fun synth(sampleRate: Int): ShortArray {
            val n = sampleRate * durationMs / 1000
            val out = ShortArray(n)
            var phase = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / n
                val f = f0 * (f1 / f0).pow(t)
                val env = v.toDouble() * (0.01 / v).pow(t)
                phase += 2 * PI * f / sampleRate
                out[i] = (sin(phase) * env * Short.MAX_VALUE).toInt().toShort()
            }
            return out
        }
    }

    /** Two oscillators played together (used for the snap "ding"). */
    data class DualSweep(
        val a0: Double, val a1: Double,
        val b0: Double, val b1: Double,
        val durationMs: Int, val v: Float,
    ) : SoundSpec() {
        override fun synth(sampleRate: Int): ShortArray {
            val n = sampleRate * durationMs / 1000
            val out = ShortArray(n)
            var pa = 0.0; var pb = 0.0
            for (i in 0 until n) {
                val t = i.toDouble() / n
                val fa = a0 * (a1 / a0).pow(t)
                val fb = b0 * (b1 / b0).pow(t)
                val env = v.toDouble() * (0.01 / v).pow(t)
                pa += 2 * PI * fa / sampleRate
                pb += 2 * PI * fb / sampleRate
                val s = (sin(pa) + sin(pb)) * 0.5 * env
                out[i] = (s * Short.MAX_VALUE).toInt().toShort()
            }
            return out
        }
    }

    /** A sequence of overlapping notes (used for the win chime: C E G C↑). */
    data class Arpeggio(
        val notesHz: List<Double>,
        val noteMs: Int,
        val gapMs: Int,
        val v: Float,
    ) : SoundSpec() {
        override fun synth(sampleRate: Int): ShortArray {
            val totalMs = (notesHz.size - 1) * gapMs + noteMs
            val n = sampleRate * totalMs / 1000
            val noteSamples = sampleRate * noteMs / 1000
            val gapSamples = sampleRate * gapMs / 1000
            val mix = DoubleArray(n)

            for ((idx, freq) in notesHz.withIndex()) {
                val start = idx * gapSamples
                val endFreq = freq * 0.8
                var phase = 0.0
                var i = 0
                while (i < noteSamples) {
                    val k = start + i
                    if (k >= n) break
                    val t = i.toDouble() / noteSamples
                    val f = freq * (endFreq / freq).pow(t)
                    val env = v.toDouble() * (0.01 / v).pow(t)
                    phase += 2 * PI * f / sampleRate
                    mix[k] += sin(phase) * env
                    i++
                }
            }

            val out = ShortArray(n)
            for (i in 0 until n) {
                val s = mix[i].coerceIn(-1.0, 1.0)
                out[i] = (s * Short.MAX_VALUE).toInt().toShort()
            }
            return out
        }
    }
}
