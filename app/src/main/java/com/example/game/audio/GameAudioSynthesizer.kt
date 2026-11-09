package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class GameAudioSynthesizer(private val isEnabledProvider: () -> Boolean) {
    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val synthScope = CoroutineScope(Dispatchers.Default)

    @Volatile var targetRpmRatio: Float = 0.2f // 0.0 to 1.0
    @Volatile var isDrifting: Boolean = false
    @Volatile var isNitroActive: Boolean = false
    @Volatile var crashIntensity: Float = 0f
    @Volatile var isMuted: Boolean = false

    fun start() {
        if (synthJob != null) return
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufSize.coerceAtLeast(sampleRate / 10) // ~100ms buffer
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            synthJob = synthScope.launch {
                val chunkSize = 512
                val shortBuffer = ShortArray(chunkSize)
                var phaseEngine = 0.0
                var phaseEngine2 = 0.0
                var currentPitch = 120.0

                while (isActive) {
                    if (!isEnabledProvider() || isMuted) {
                        shortBuffer.fill(0)
                        audioTrack?.write(shortBuffer, 0, chunkSize)
                        continue
                    }

                    // Smooth pitch interpolation
                    val targetPitch = 80.0 + (targetRpmRatio * 420.0) // 80Hz idle to 500Hz redline
                    currentPitch += (targetPitch - currentPitch) * 0.15

                    val driftActive = isDrifting
                    val nitro = isNitroActive
                    val crash = crashIntensity
                    if (crashIntensity > 0.05f) {
                        crashIntensity *= 0.85f
                    } else {
                        crashIntensity = 0f
                    }

                    val twoPi = Math.PI * 2.0
                    val deltaPhase = (twoPi * currentPitch) / sampleRate

                    for (i in 0 until chunkSize) {
                        phaseEngine += deltaPhase
                        if (phaseEngine > twoPi) phaseEngine -= twoPi

                        phaseEngine2 += deltaPhase * 2.0 // second harmonic
                        if (phaseEngine2 > twoPi) phaseEngine2 -= twoPi

                        // Fundamental engine sawtooth + sine harmonic
                        val engineHarmonic1 = (phaseEngine / Math.PI - 1.0).toFloat()
                        val engineHarmonic2 = (sin(phaseEngine2)).toFloat() * 0.4f
                        var sample = (engineHarmonic1 * 0.6f + engineHarmonic2 * 0.4f) * 0.35f

                        // Drift squeal (high frequency band-pass noise)
                        if (driftActive) {
                            val tireNoise = (Random.nextFloat() * 2f - 1f) * 0.4f
                            sample += tireNoise * (sin(i * 0.35f).toFloat())
                        }

                        // Nitro roar (rumble noise + high hiss)
                        if (nitro) {
                            val nitroNoise = (Random.nextFloat() * 2f - 1f) * 0.35f
                            sample = sample * 0.8f + nitroNoise
                        }

                        // Crash thump
                        if (crash > 0.01f) {
                            val crashNoise = (Random.nextFloat() * 2f - 1f) * crash * 0.7f
                            sample += crashNoise
                        }

                        // Hard limit to prevent clipping
                        val clamped = sample.coerceIn(-1.0f, 1.0f)
                        shortBuffer[i] = (clamped * 26000).toInt().toShort()
                    }

                    audioTrack?.write(shortBuffer, 0, chunkSize)
                }
            }
        } catch (_: Exception) {
            // AudioTrack fallback
        }
    }

    fun playCountdownBeep(highPitch: Boolean) {
        if (!isEnabledProvider() || isMuted) return
        synthScope.launch {
            try {
                val freq = if (highPitch) 1046.5 else 523.25 // C6 or C5
                val durationMs = if (highPitch) 350 else 180
                val totalSamples = (sampleRate * durationMs) / 1000
                val buffer = ShortArray(totalSamples)
                val twoPi = Math.PI * 2.0
                var phase = 0.0
                val delta = (twoPi * freq) / sampleRate

                for (i in 0 until totalSamples) {
                    phase += delta
                    val envelope = 1.0f - (i.toFloat() / totalSamples) // linear fade out
                    val sample = (sin(phase) * envelope * 0.4).toFloat()
                    buffer[i] = (sample * 30000).toInt().toShort()
                }
                audioTrack?.write(buffer, 0, totalSamples)
            } catch (_: Exception) {}
        }
    }

    fun triggerCrash() {
        crashIntensity = 1.0f
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
