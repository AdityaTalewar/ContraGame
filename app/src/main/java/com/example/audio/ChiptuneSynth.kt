package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sin
import kotlin.random.Random

/**
 * Authentic 8-bit NES Chiptune sound synthesis engine.
 * Synthesizes Pulse (Square), Triangle (Bass), and Noise channels in real-time
 * via AudioTrack PCM 16-bit audio streaming.
 */
class ChiptuneSynth {

    companion object {
        const val SAMPLE_RATE = 22050
        const val BUFFER_SIZE = 1024
    }

    enum class Track {
        TITLE, JUNGLE, WATERFALL, ALIEN_CORE, BOSS, VICTORY, GAME_OVER, NONE
    }

    enum class Sfx {
        SHOOT_NORMAL, SHOOT_MACHINE, SHOOT_SPREAD, SHOOT_LASER, SHOOT_FIRE,
        EXPLOSION, EXPLOSION_BIG, POWERUP_PICKUP, JUMP, PLAYER_HIT, PLAYER_DEATH,
        BOSS_ALARM, ONE_UP, BUTTON_CLICK
    }

    private var audioTrack: AudioTrack? = null
    @Volatile private var isRunning = false
    private var synthesisThread: Thread? = null

    // Volume controls (0.0 to 1.0)
    var musicVolume = 0.75f
    var sfxVolume = 0.9f
    var isMuted = false

    // Music playback state
    @Volatile private var currentTrack = Track.NONE
    private var trackTick = 0
    private var noteStep = 0
    private val tempoBpm = 140

    // Active Sound Effects
    private class ActiveSfx(
        val type: Sfx,
        var currentFrame: Int = 0,
        val totalFrames: Int
    )

    private val activeSfxQueue = ConcurrentLinkedQueue<ActiveSfx>()

    // Chiptune channel phase accumulators
    private var pulse1Phase = 0.0
    private var pulse2Phase = 0.0
    private var trianglePhase = 0.0
    private var noiseSeed = 0x7FFF

    init {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferCapacity = maxOf(minBufSize, BUFFER_SIZE * 4)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferCapacity)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isRunning = true

            synthesisThread = Thread {
                runAudioLoop()
            }.apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTrack(track: Track) {
        if (currentTrack == track) return
        currentTrack = track
        trackTick = 0
        noteStep = 0
    }

    fun stopMusic() {
        currentTrack = Track.NONE
    }

    fun playSfx(sfx: Sfx) {
        if (isMuted) return
        val durationFrames = when (sfx) {
            Sfx.SHOOT_NORMAL -> SAMPLE_RATE / 12       // ~80ms
            Sfx.SHOOT_MACHINE -> SAMPLE_RATE / 18      // ~55ms
            Sfx.SHOOT_SPREAD -> SAMPLE_RATE / 8        // ~125ms
            Sfx.SHOOT_LASER -> SAMPLE_RATE / 6         // ~160ms
            Sfx.SHOOT_FIRE -> SAMPLE_RATE / 7          // ~140ms
            Sfx.EXPLOSION -> SAMPLE_RATE / 4           // ~250ms
            Sfx.EXPLOSION_BIG -> SAMPLE_RATE / 2       // ~500ms
            Sfx.POWERUP_PICKUP -> SAMPLE_RATE / 3      // ~330ms
            Sfx.JUMP -> SAMPLE_RATE / 8                // ~125ms
            Sfx.PLAYER_HIT -> SAMPLE_RATE / 6          // ~160ms
            Sfx.PLAYER_DEATH -> SAMPLE_RATE / 2        // ~500ms
            Sfx.BOSS_ALARM -> SAMPLE_RATE * 3 / 4      // ~750ms
            Sfx.ONE_UP -> SAMPLE_RATE * 2 / 3          // ~660ms
            Sfx.BUTTON_CLICK -> SAMPLE_RATE / 25       // ~40ms
        }
        activeSfxQueue.add(ActiveSfx(sfx, 0, durationFrames))
    }

    private fun runAudioLoop() {
        val pcmBuffer = ShortArray(BUFFER_SIZE)
        val samplesPerStep = (SAMPLE_RATE * 60) / (tempoBpm * 4) // 16th note length

        while (isRunning) {
            for (i in 0 until BUFFER_SIZE) {
                var musicSample = 0.0
                var sfxSample = 0.0

                if (!isMuted && currentTrack != Track.NONE && musicVolume > 0.01f) {
                    val (p1Freq, p2Freq, triFreq, noiseHit) = getMusicNote(currentTrack, noteStep)

                    // Channel 1: Pulse 1 (Lead 50% square)
                    if (p1Freq > 20.0) {
                        pulse1Phase += p1Freq / SAMPLE_RATE
                        if (pulse1Phase > 1.0) pulse1Phase -= 1.0
                        musicSample += if (pulse1Phase < 0.5) 0.22 else -0.22
                    }

                    // Channel 2: Pulse 2 (Harmony 25% square)
                    if (p2Freq > 20.0) {
                        pulse2Phase += p2Freq / SAMPLE_RATE
                        if (pulse2Phase > 1.0) pulse2Phase -= 1.0
                        musicSample += if (pulse2Phase < 0.25) 0.16 else -0.16
                    }

                    // Channel 3: Triangle (Punchy bass)
                    if (triFreq > 20.0) {
                        trianglePhase += triFreq / SAMPLE_RATE
                        if (trianglePhase > 1.0) trianglePhase -= 1.0
                        val tri = if (trianglePhase < 0.5) {
                            (trianglePhase * 4.0) - 1.0
                        } else {
                            3.0 - (trianglePhase * 4.0)
                        }
                        musicSample += tri * 0.32
                    }

                    // Channel 4: Percussion Noise
                    if (noiseHit > 0) {
                        val stepProgress = (trackTick % samplesPerStep).toDouble() / samplesPerStep
                        val noiseDecay = (1.0 - stepProgress).coerceIn(0.0, 1.0)
                        noiseSeed = ((noiseSeed shr 1) xor (if ((noiseSeed and 1) != 0) 0x6000 else 0)) and 0x7FFF
                        val noiseVal = if ((noiseSeed and 1) != 0) 1.0 else -1.0
                        musicSample += noiseVal * 0.18 * noiseDecay
                    }

                    trackTick++
                    if (trackTick >= samplesPerStep) {
                        trackTick = 0
                        noteStep++
                    }
                }

                // Render Active Sound Effects
                if (!isMuted && sfxVolume > 0.01f && !activeSfxQueue.isEmpty()) {
                    val it = activeSfxQueue.iterator()
                    while (it.hasNext()) {
                        val sfx = it.next()
                        sfxSample += generateSfxSample(sfx.type, sfx.currentFrame, sfx.totalFrames)
                        sfx.currentFrame++
                        if (sfx.currentFrame >= sfx.totalFrames) {
                            it.remove()
                        }
                    }
                }

                val finalOutput = ((musicSample * musicVolume) + (sfxSample * sfxVolume))
                    .coerceIn(-0.95, 0.95)
                pcmBuffer[i] = (finalOutput * Short.MAX_VALUE).toInt().toShort()
            }

            try {
                audioTrack?.write(pcmBuffer, 0, BUFFER_SIZE)
            } catch (e: Exception) {
                // Track might be released
                break
            }
        }
    }

    private data class StepNote(val p1: Double, val p2: Double, val tri: Double, val noise: Int)

    private fun getMusicNote(track: Track, step: Int): StepNote {
        val s = step % 64
        return when (track) {
            Track.TITLE -> {
                // Classic Contra heroic title fanfare
                val bassScale = doubleArrayOf(110.0, 110.0, 130.8, 146.8, 164.8, 146.8, 130.8, 110.0)
                val tri = bassScale[(s / 4) % bassScale.size]
                val lead = when ((s / 2) % 16) {
                    0, 1 -> 440.0 // A4
                    2 -> 523.25   // C5
                    3 -> 587.33   // D5
                    4, 5 -> 659.25 // E5
                    6 -> 587.33
                    7 -> 523.25
                    8, 9 -> 659.25
                    10 -> 783.99  // G5
                    11 -> 880.0   // A5
                    12, 13 -> 783.99
                    14 -> 659.25
                    else -> 587.33
                }
                val harm = lead * 0.75
                val drum = if (s % 4 == 0 || s % 8 == 4) 1 else 0
                StepNote(lead, harm, tri, drum)
            }

            Track.JUNGLE -> {
                // Fast driving rock/synth Jungle stage theme
                val bassNotes = doubleArrayOf(110.0, 110.0, 146.8, 110.0, 164.8, 146.8, 130.8, 123.4)
                val tri = bassNotes[(s / 2) % bassNotes.size]
                val lead = when (s % 32) {
                    0 -> 440.0; 2 -> 440.0; 4 -> 523.25; 6 -> 587.33
                    8 -> 659.25; 10 -> 659.25; 12 -> 587.33; 14 -> 523.25
                    16 -> 587.33; 18 -> 659.25; 20 -> 523.25; 22 -> 440.0
                    24 -> 392.0; 26 -> 440.0; 28 -> 493.88; 30 -> 523.25
                    else -> 0.0
                }
                val harm = if (lead > 0.0) lead * 1.25 else 0.0
                val drum = if (s % 4 == 2 || s % 8 == 6) 1 else (if (s % 2 == 0) 1 else 0)
                StepNote(lead, harm, tri, drum)
            }

            Track.WATERFALL -> {
                // Syncopated ascending techno groove
                val bassNotes = doubleArrayOf(130.8, 130.8, 155.6, 174.6, 196.0, 174.6, 155.6, 130.8)
                val tri = bassNotes[(s / 2) % bassNotes.size]
                val lead = when (s % 32) {
                    0 -> 523.25; 3 -> 622.25; 6 -> 698.46; 8 -> 783.99
                    11 -> 830.61; 14 -> 783.99; 16 -> 698.46; 19 -> 622.25
                    22 -> 523.25; 24 -> 587.33; 27 -> 622.25; 30 -> 698.46
                    else -> 0.0
                }
                val harm = if (lead > 0) lead * 0.8 else 0.0
                val drum = if (s % 4 == 2) 1 else 0
                StepNote(lead, harm, tri, drum)
            }

            Track.ALIEN_CORE -> {
                // Ominous, chromatic alien tension
                val tri = if ((s / 2) % 2 == 0) 87.3 else 92.5
                val lead = when (s % 16) {
                    0 -> 349.23; 2 -> 370.0; 4 -> 392.0; 6 -> 415.3
                    8 -> 440.0; 10 -> 415.3; 12 -> 392.0; 14 -> 370.0
                    else -> 0.0
                }
                val harm = if (lead > 0) lead * 1.06 else 0.0
                val drum = if (s % 4 == 0) 1 else 0
                StepNote(lead, harm, tri, drum)
            }

            Track.BOSS -> {
                // Fast intense adrenaline battle theme (double time)
                val tri = if (s % 2 == 0) 146.8 else 164.8
                val lead = when (s % 16) {
                    0 -> 587.33; 1 -> 659.25; 2 -> 698.46; 3 -> 783.99
                    4 -> 880.0; 6 -> 783.99; 8 -> 698.46; 10 -> 659.25
                    12 -> 587.33; 14 -> 523.25; else -> 0.0
                }
                val harm = lead * 1.5
                val drum = 1 // Constant driving snare
                StepNote(lead, harm, tri, drum)
            }

            Track.VICTORY -> {
                // Triumphant stage clear fanfare
                val lead = when (s % 16) {
                    0 -> 523.25; 2 -> 659.25; 4 -> 783.99; 6 -> 1046.5
                    8, 9, 10, 11, 12, 13, 14 -> 1046.5
                    else -> 0.0
                }
                val harm = lead * 0.75
                val tri = 261.63
                val drum = if (s % 4 == 0) 1 else 0
                StepNote(lead, harm, tri, drum)
            }

            Track.GAME_OVER -> {
                val lead = when (s % 16) {
                    0 -> 440.0; 3 -> 415.3; 6 -> 392.0; 9 -> 370.0
                    12, 13, 14, 15 -> 349.23
                    else -> 0.0
                }
                StepNote(lead, 0.0, 110.0, 0)
            }

            Track.NONE -> StepNote(0.0, 0.0, 0.0, 0)
        }
    }

    private fun generateSfxSample(sfx: Sfx, frame: Int, totalFrames: Int): Double {
        val progress = frame.toDouble() / totalFrames
        val decay = (1.0 - progress).coerceIn(0.0, 1.0)

        return when (sfx) {
            Sfx.SHOOT_NORMAL -> {
                // Crisp downward frequency chirp
                val freq = 900.0 - (progress * 500.0)
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.3 else -0.3
                pulse * decay
            }

            Sfx.SHOOT_MACHINE -> {
                // Fast sharp pulse burst
                val freq = 1200.0 - (progress * 700.0)
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.3) 0.35 else -0.35
                pulse * decay
            }

            Sfx.SHOOT_SPREAD -> {
                // Chunky multi-tone blast
                val freq1 = 750.0 - (progress * 400.0)
                val freq2 = 1100.0 - (progress * 600.0)
                val p1 = if ((frame * freq1 / SAMPLE_RATE) % 1.0 < 0.5) 0.25 else -0.25
                val p2 = if ((frame * freq2 / SAMPLE_RATE) % 1.0 < 0.25) 0.25 else -0.25
                (p1 + p2) * decay
            }

            Sfx.SHOOT_LASER -> {
                // Sci-fi piercing sweep with FM modulation
                val mod = sin(frame * 0.1) * 200.0
                val freq = 1600.0 - (progress * 900.0) + mod
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.4 else -0.4
                pulse * decay
            }

            Sfx.SHOOT_FIRE -> {
                // Rotating fiery hiss
                val noise = Random.nextDouble(-0.35, 0.35)
                val lowSine = sin(frame * 0.04) * 0.2
                (noise + lowSine) * decay
            }

            Sfx.EXPLOSION -> {
                // Heavy noise explosion with low frequency rumble
                val noise = Random.nextDouble(-0.5, 0.5)
                val rumbleFreq = 80.0 * (1.0 - progress)
                val rumble = sin(frame * rumbleFreq * 2 * Math.PI / SAMPLE_RATE) * 0.3
                (noise * 0.7 + rumble) * decay
            }

            Sfx.EXPLOSION_BIG -> {
                // Massive boss explosion with resonant bass
                val noise = Random.nextDouble(-0.6, 0.6)
                val rumble = sin(frame * 50.0 * 2 * Math.PI / SAMPLE_RATE) * 0.4
                (noise * 0.6 + rumble) * decay
            }

            Sfx.POWERUP_PICKUP -> {
                // Ascending 3-note arpeggio (C5 -> E5 -> G5 -> C6)
                val noteIdx = (progress * 4).toInt().coerceIn(0, 3)
                val freq = when (noteIdx) {
                    0 -> 523.25; 1 -> 659.25; 2 -> 783.99; else -> 1046.5
                }
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.35 else -0.35
                pulse * 0.9
            }

            Sfx.JUMP -> {
                // Upward frequency chirp
                val freq = 200.0 + (progress * 600.0)
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.3 else -0.3
                pulse * decay
            }

            Sfx.PLAYER_HIT -> {
                // Noise clank + buzz
                val noise = Random.nextDouble(-0.4, 0.4)
                val buzz = if ((frame * 120.0 / SAMPLE_RATE) % 1.0 < 0.5) 0.3 else -0.3
                (noise + buzz) * 0.5 * decay
            }

            Sfx.PLAYER_DEATH -> {
                // Descending retro death fall
                val freq = (600.0 - (progress * 480.0)).coerceAtLeast(60.0)
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.4 else -0.4
                pulse * decay
            }

            Sfx.BOSS_ALARM -> {
                // Alternating siren (emergency red alert)
                val phase = (frame / (SAMPLE_RATE / 8)) % 2
                val freq = if (phase == 0) 880.0 else 660.0
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.3 else -0.3
                pulse * 0.8
            }

            Sfx.ONE_UP -> {
                // Classic 1-Up celebratory melody
                val note = (progress * 6).toInt().coerceIn(0, 5)
                val freq = when (note) {
                    0 -> 330.0; 1 -> 392.0; 2 -> 659.25; 3 -> 523.25; 4 -> 587.33; else -> 783.99
                }
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.3 else -0.3
                pulse * 0.85
            }

            Sfx.BUTTON_CLICK -> {
                val freq = 800.0 - (progress * 400.0)
                val pulse = if ((frame * freq / SAMPLE_RATE) % 1.0 < 0.5) 0.25 else -0.25
                pulse * decay
            }
        }
    }

    fun release() {
        isRunning = false
        try {
            synthesisThread?.interrupt()
            synthesisThread = null
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
