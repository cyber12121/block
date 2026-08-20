package com.example.service.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

enum class SoundPreset(val displayName: String, val emoji: String, val description: String) {
    RAIN("Gentle Rain", "🌧️", "Soothing raindrops for calm mental clarity"),
    BINAURAL_ALPHA("Alpha Waves (10Hz)", "🧘", "432Hz binaural beats — use headphones for full effect"),
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

    // Audio focus management
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusListener: AudioManager.OnAudioFocusChangeListener? = null

    var currentPreset: SoundPreset? = null
        private set

    var isPlaying: Boolean = false
        private set

    var volume: Float = 0.8f
        private set

    fun play(preset: SoundPreset, context: Context? = null) {
        stop()
        currentPreset = preset
        isPlaying = true

        // Request audio focus so we pause during calls / navigation
        context?.let { requestAudioFocus(it) }

        val isBinaural = preset == SoundPreset.BINAURAL_ALPHA
        // Binaural needs stereo (one frequency per ear); everything else is mono
        val channelMask = if (isBinaural) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            channelMask,
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
                        .setChannelMask(channelMask)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volume)
            audioTrack?.play()

            soundJob = scope.launch {
                if (isBinaural) {
                    playBinauralStereo()
                } else {
                    playMonoPreset(preset, bufferSize)
                }
            }
        } catch (_: Exception) {
            stop()
        }
    }

    /**
     * True binaural: left ear gets 432 Hz, right ear gets 442 Hz.
     * The 10 Hz difference is perceived as a beat inside the listener's head
     * when using stereo headphones.
     */
    private suspend fun CoroutineScope.playBinauralStereo() {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(2048)

        // Stereo: interleaved L/R samples, so buffer holds bufferSize/2 frames
        val buffer = ShortArray(bufferSize)
        var phaseL = 0.0
        var phaseR = 0.0
        val freqL = 432.0   // left ear
        val freqR = 442.0   // right ear — 10 Hz beat (alpha wave range)
        val stepL = 2.0 * Math.PI * freqL / SAMPLE_RATE
        val stepR = 2.0 * Math.PI * freqR / SAMPLE_RATE

        while (isActive && isPlaying) {
            var i = 0
            while (i < buffer.size - 1) {
                // Left channel
                buffer[i] = (sin(phaseL) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                phaseL += stepL
                if (phaseL > 2.0 * Math.PI) phaseL -= 2.0 * Math.PI
                // Right channel
                buffer[i + 1] = (sin(phaseR) * 0.25 * Short.MAX_VALUE).toInt().toShort()
                phaseR += stepR
                if (phaseR > 2.0 * Math.PI) phaseR -= 2.0 * Math.PI
                i += 2
            }
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }

    private suspend fun CoroutineScope.playMonoPreset(preset: SoundPreset, bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val random = Random()
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
                    SoundPreset.BINAURAL_ALPHA -> 0.0 // handled separately in stereo
                }

                val clamped = sample.coerceIn(-1.0, 1.0)
                buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
            }
            audioTrack?.write(buffer, 0, buffer.size)
        }
    }

    private fun requestAudioFocus(context: Context) {
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Phone call or navigation started — pause audio
                    audioTrack?.setVolume(0f)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Call ended — restore volume
                    audioTrack?.setVolume(volume)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Another app wants audio briefly — duck to 20%
                    audioTrack?.setVolume(volume * 0.2f)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener!!)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .build()
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(audioFocusListener)
            }
        } catch (_: Exception) {}
        audioFocusRequest = null
        audioFocusListener = null
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
        abandonAudioFocus()
    }
}
