package com.example.service.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

enum class SoundPreset(val displayName: String, val emoji: String, val description: String) {
    RAIN("Gentle Rain", "🌧️", "Soothing raindrops for calm mental clarity"),
    BINAURAL_ALPHA("Alpha Waves (10Hz)", "🧘", "432Hz deep focus binaural brainwave frequency"),
    OCEAN_WAVES("Ocean Surf", "🌊", "Rhythmic rolling waves to wash away distractions"),
    FOREST_BREEZE("Forest Breeze", "🍃", "Calm wind and rustling leaves"),
    WHITE_NOISE("Pure White Noise", "📻", "Steady broadband mask for loud surroundings")
}

object FocusSoundEngine {
    // Battery optimization: 22050 Hz halves DSP CPU cycles while maintaining crystal-clear ambient acoustics
    private const val SAMPLE_RATE = 22050
    private var audioTrack: AudioTrack? = null
    private var soundJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentPreset: SoundPreset? = null
        private set

    var isPlaying: Boolean = false
        private set

    var volume: Float = 0.8f
        private set

    fun play(preset: SoundPreset) {
        stop()
        currentPreset = preset
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volume)
            audioTrack?.play()

            soundJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                val random = Random()
                var phase1 = 0.0
                var phase2 = 0.0
                var filterState = 0.0
                var waveModPhase = 0.0

                while (isActive && isPlaying) {
                    for (i in buffer.indices) {
                        val sample: Double = when (preset) {
                            SoundPreset.RAIN -> {
                                val white = random.nextDouble() * 2.0 - 1.0
                                filterState = 0.92 * filterState + 0.08 * white
                                val drop = if (random.nextDouble() < 0.002) (random.nextDouble() - 0.5) * 1.5 else 0.0
                                (filterState * 0.7 + drop * 0.3) * 0.5
                            }
                            SoundPreset.BINAURAL_ALPHA -> {
                                val f1 = 432.0
                                val f2 = 442.0
                                val s1 = sin(phase1)
                                val s2 = sin(phase2)
                                phase1 += 2.0 * Math.PI * f1 / SAMPLE_RATE
                                phase2 += 2.0 * Math.PI * f2 / SAMPLE_RATE
                                if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                                if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                                (s1 + s2) * 0.25
                            }
                            SoundPreset.OCEAN_WAVES -> {
                                waveModPhase += 2.0 * Math.PI * 0.12 / SAMPLE_RATE
                                if (waveModPhase > 2.0 * Math.PI) waveModPhase -= 2.0 * Math.PI
                                val swell = (sin(waveModPhase) + 1.0) * 0.45 + 0.1
                                val white = random.nextDouble() * 2.0 - 1.0
                                filterState = 0.86 * filterState + 0.14 * white
                                filterState * swell * 0.6
                            }
                            SoundPreset.FOREST_BREEZE -> {
                                waveModPhase += 2.0 * Math.PI * 0.05 / SAMPLE_RATE
                                if (waveModPhase > 2.0 * Math.PI) waveModPhase -= 2.0 * Math.PI
                                val gust = (sin(waveModPhase) + 1.0) * 0.35 + 0.3
                                val white = random.nextDouble() * 2.0 - 1.0
                                filterState = 0.90 * filterState + 0.10 * white
                                filterState * gust * 0.4
                            }
                            SoundPreset.WHITE_NOISE -> {
                                (random.nextDouble() * 2.0 - 1.0) * 0.25
                            }
                        }

                        val clamped = sample.coerceIn(-1.0, 1.0)
                        buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                }
            }
        } catch (_: Exception) {
            stop()
        }
    }

    fun setSoundVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volume)
        } catch (_: Exception) {}
    }

    fun stop() {
        isPlaying = false
        currentPreset = null
        soundJob?.cancel()
        soundJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
