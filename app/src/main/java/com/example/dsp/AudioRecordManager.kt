package com.example.dsp

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

class AudioRecordManager(private val context: Context) {

    val sampleRate = 22050
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val pitchShifter = PitchShifter(sampleRate)

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _isLiveMonitoring = MutableStateFlow(false)
    val isLiveMonitoring = _isLiveMonitoring.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f) // 0.0 to 1.0
    val currentAmplitude = _currentAmplitude.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs = _recordingDurationMs.asStateFlow()

    private var recordJob: Job? = null
    private var liveJob: Job? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // Raw PCM buffer for currently recording session
    private var recordedPcmList = mutableListOf<Short>()

    // Current parameters for live modulation
    @Volatile var livePitchSemitones = 4.8f
    @Volatile var liveFormantBrightness = 0.65f

    /**
     * Starts recording user's raw voice from microphone into memory.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        stopLiveMonitoring() // stop live loopback if active to avoid mic conflict

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val bufferSize = maxOf(minBufferSize, 2048)

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Failed to init AudioRecord", e)
            return false
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecordManager", "AudioRecord not initialized")
            audioRecord.release()
            return false
        }

        recordedPcmList.clear()
        _isRecording.value = true
        _recordingDurationMs.value = 0L

        recordJob = ioScope.launch {
            val startTime = System.currentTimeMillis()
            val buffer = ShortArray(1024)
            try {
                audioRecord.startRecording()
                while (isActive && _isRecording.value) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        for (i in 0 until read) {
                            recordedPcmList.add(buffer[i])
                        }

                        // Calculate RMS amplitude
                        var sum = 0.0
                        for (i in 0 until read) {
                            val v = buffer[i] / 32768.0
                            sum += v * v
                        }
                        val rms = sqrt(sum / read).toFloat()
                        _currentAmplitude.value = (rms * 4.5f).coerceIn(0f, 1f)

                        _recordingDurationMs.value = System.currentTimeMillis() - startTime
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecordManager", "Recording error", e)
            } finally {
                try {
                    audioRecord.stop()
                    audioRecord.release()
                } catch (e: Exception) {
                    // ignored
                }
                _isRecording.value = false
                _currentAmplitude.value = 0f
            }
        }

        return true
    }

    /**
     * Stops current recording and returns raw captured PCM short array.
     */
    fun stopRecording(): ShortArray {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        _currentAmplitude.value = 0f

        val result = recordedPcmList.toShortArray()
        return result
    }

    /**
     * Starts Real-Time Voice Modulation (Live Monitoring).
     * Speech from microphone is modulated to female pitch in real-time
     * and played through earphones/speakers with minimal latency!
     */
    @SuppressLint("MissingPermission")
    fun startLiveMonitoring(): Boolean {
        if (_isLiveMonitoring.value) return true
        if (_isRecording.value) return false // can't run both at once on single mic

        val minRecBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val recBufSize = maxOf(minRecBuf, 1024)

        val minTrackBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
        val trackBufSize = maxOf(minTrackBuf, 1024)

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                recBufSize
            )
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Live AudioRecord init failed", e)
            return false
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }

        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfigOut)
                        .build()
                )
                .setBufferSizeInBytes(trackBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            audioRecord.release()
            Log.e("AudioRecordManager", "Live AudioTrack init failed", e)
            return false
        }

        pitchShifter.reset()
        _isLiveMonitoring.value = true

        liveJob = ioScope.launch {
            val chunkSize = 512
            val inBuf = ShortArray(chunkSize)
            val outBuf = ShortArray(chunkSize)

            try {
                audioRecord.startRecording()
                audioTrack.play()

                while (isActive && _isLiveMonitoring.value) {
                    val read = audioRecord.read(inBuf, 0, chunkSize)
                    if (read > 0) {
                        // Modulate in real-time with dual-tap low-latency engine
                        pitchShifter.processRealTime(
                            input = inBuf,
                            output = outBuf,
                            length = read,
                            pitchSemitones = livePitchSemitones,
                            formantBrightness = liveFormantBrightness
                        )

                        audioTrack.write(outBuf, 0, read)

                        // Amplitude calculation for visual feedback
                        var sum = 0.0
                        for (i in 0 until read) {
                            val v = outBuf[i] / 32768.0
                            sum += v * v
                        }
                        val rms = sqrt(sum / read).toFloat()
                        _currentAmplitude.value = (rms * 4.0f).coerceIn(0f, 1f)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecordManager", "Live monitor error", e)
            } finally {
                try {
                    audioRecord.stop()
                    audioRecord.release()
                } catch (e: Exception) { /* ignored */ }

                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) { /* ignored */ }

                _isLiveMonitoring.value = false
                _currentAmplitude.value = 0f
            }
        }

        return true
    }

    fun stopLiveMonitoring() {
        _isLiveMonitoring.value = false
        liveJob?.cancel()
        liveJob = null
        _currentAmplitude.value = 0f
    }

    /**
     * Modulates raw recorded PCM to female voice and saves it as a WAV file
     * ready to send to Telegram or playback.
     */
    fun processAndSaveVoiceNote(
        rawPcm: ShortArray,
        pitchSemitones: Float,
        formantBrightness: Float,
        speedFactor: Float = 1.0f,
        destinationFile: File
    ): Boolean {
        return try {
            val modulatedPcm = pitchShifter.processFullRecording(
                rawPcm = rawPcm,
                pitchSemitones = pitchSemitones,
                formantBrightness = formantBrightness,
                speedFactor = speedFactor
            )

            destinationFile.parentFile?.mkdirs()
            WavUtils.writePcmToWav(destinationFile, modulatedPcm, sampleRate, 1)
            true
        } catch (e: Exception) {
            Log.e("AudioRecordManager", "Error saving voice note", e)
            false
        }
    }
}
