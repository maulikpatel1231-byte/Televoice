package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dsp.AudioPlayerManager
import com.example.dsp.AudioRecordManager
import com.example.model.RecordingItem
import com.example.model.VoicePreset
import com.example.telegram.TelegramSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecordManager = AudioRecordManager(application)
    private val audioPlayerManager = AudioPlayerManager()

    val isRecording = audioRecordManager.isRecording
    val isLiveMonitoring = audioRecordManager.isLiveMonitoring
    val currentAmplitude = audioRecordManager.currentAmplitude
    val recordingDurationMs = audioRecordManager.recordingDurationMs

    val isPlaying = audioPlayerManager.isPlaying
    val currentPositionMs = audioPlayerManager.currentPositionMs
    val durationMs = audioPlayerManager.durationMs
    val currentPlayingPath = audioPlayerManager.currentPlayingPath

    // Active voice preset
    private val _selectedPreset = MutableStateFlow<VoicePreset>(VoicePreset.SweetGirl)
    val selectedPreset = _selectedPreset.asStateFlow()

    // Fine tuning controls for Custom preset
    private val _customPitchSemitones = MutableStateFlow(4.8f)
    val customPitchSemitones = _customPitchSemitones.asStateFlow()

    private val _customFormantBrightness = MutableStateFlow(0.65f)
    val customFormantBrightness = _customFormantBrightness.asStateFlow()

    private val _customSpeedFactor = MutableStateFlow(1.0f)
    val customSpeedFactor = _customSpeedFactor.asStateFlow()

    // Processing state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    // Last recorded audio file
    private val _lastModulatedFile = MutableStateFlow<File?>(null)
    val lastModulatedFile = _lastModulatedFile.asStateFlow()

    private var lastRawPcm: ShortArray? = null

    // Saved recordings
    private val _savedRecordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val savedRecordings = _savedRecordings.asStateFlow()

    // Telegram Bot Settings
    private val prefs = application.getSharedPreferences("televoice_prefs", Context.MODE_PRIVATE)
    private val _botToken = MutableStateFlow(prefs.getString("tg_bot_token", "") ?: "")
    val botToken = _botToken.asStateFlow()

    private val _botChatId = MutableStateFlow(prefs.getString("tg_bot_chat_id", "") ?: "")
    val botChatId = _botChatId.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    init {
        loadSavedRecordings()
    }

    fun selectPreset(preset: VoicePreset) {
        _selectedPreset.value = preset
        updateLiveDspParameters()

        // If there's an existing recording, automatically re-modulate it with the new preset!
        if (lastRawPcm != null && lastRawPcm!!.isNotEmpty() && !isRecording.value) {
            reprocessLastRecording()
        }
    }

    fun setCustomPitch(semitones: Float) {
        _customPitchSemitones.value = semitones
        updateLiveDspParameters()
    }

    fun setCustomFormant(brightness: Float) {
        _customFormantBrightness.value = brightness
        updateLiveDspParameters()
    }

    fun setCustomSpeed(speed: Float) {
        _customSpeedFactor.value = speed
    }

    private fun updateLiveDspParameters() {
        val preset = _selectedPreset.value
        if (preset.id == VoicePreset.Custom.id) {
            audioRecordManager.livePitchSemitones = _customPitchSemitones.value
            audioRecordManager.liveFormantBrightness = _customFormantBrightness.value
        } else {
            audioRecordManager.livePitchSemitones = preset.pitchShiftSemitones
            audioRecordManager.liveFormantBrightness = preset.formantBrightness
        }
    }

    fun toggleLiveMonitoring() {
        if (isLiveMonitoring.value) {
            audioRecordManager.stopLiveMonitoring()
        } else {
            updateLiveDspParameters()
            val started = audioRecordManager.startLiveMonitoring()
            if (!started) {
                emitMessage("Failed to start Live Mic. Check microphone permission.")
            }
        }
    }

    fun startRecording(): Boolean {
        audioPlayerManager.stop()
        return audioRecordManager.startRecording()
    }

    fun stopRecording() {
        val rawPcm = audioRecordManager.stopRecording()
        if (rawPcm.isEmpty()) {
            emitMessage("Recording was too short.")
            return
        }

        lastRawPcm = rawPcm
        processRawPcmToModulatedFile(rawPcm)
    }

    private fun reprocessLastRecording() {
        val rawPcm = lastRawPcm ?: return
        audioPlayerManager.stop()
        processRawPcmToModulatedFile(rawPcm)
    }

    private fun processRawPcmToModulatedFile(rawPcm: ShortArray) {
        viewModelScope.launch {
            _isProcessing.value = true
            val preset = _selectedPreset.value
            val pitch = if (preset.id == VoicePreset.Custom.id) _customPitchSemitones.value else preset.pitchShiftSemitones
            val formant = if (preset.id == VoicePreset.Custom.id) _customFormantBrightness.value else preset.formantBrightness
            val speed = if (preset.id == VoicePreset.Custom.id) _customSpeedFactor.value else preset.speedFactor

            val context = getApplication<Application>()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioDir = File(context.filesDir, "recordings")
            audioDir.mkdirs()
            val outputFile = File(audioDir, "TeleVoice_${preset.id}_$timestamp.wav")

            val success = withContext(Dispatchers.IO) {
                audioRecordManager.processAndSaveVoiceNote(
                    rawPcm = rawPcm,
                    pitchSemitones = pitch,
                    formantBrightness = formant,
                    speedFactor = speed,
                    destinationFile = outputFile
                )
            }

            _isProcessing.value = false
            if (success) {
                _lastModulatedFile.value = outputFile
                loadSavedRecordings()
                emitMessage("Voice converted to ${preset.name}! Ready to send to Telegram 🌸")
            } else {
                emitMessage("Error processing voice modulation.")
            }
        }
    }

    fun playAudio(filePath: String) {
        audioPlayerManager.playFile(filePath)
    }

    fun pauseAudio() {
        audioPlayerManager.pause()
    }

    fun resumeAudio() {
        audioPlayerManager.resume()
    }

    fun seekAudio(positionMs: Int) {
        audioPlayerManager.seekTo(positionMs)
    }

    fun stopAudio() {
        audioPlayerManager.stop()
    }

    fun sendToTelegram(context: Context, file: File?) {
        val target = file ?: _lastModulatedFile.value
        if (target == null || !target.exists()) {
            emitMessage("No audio file available to send.")
            return
        }

        TelegramSender.sendAudioToTelegram(context, target)
    }

    fun sendViaBot(file: File?) {
        val target = file ?: _lastModulatedFile.value
        if (target == null || !target.exists()) {
            emitMessage("No audio file available.")
            return
        }

        val token = _botToken.value
        val chat = _botChatId.value
        if (token.isBlank() || chat.isBlank()) {
            emitMessage("Please configure Telegram Bot Token and Chat ID in Settings.")
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            val result = TelegramSender.sendViaTelegramBot(
                botToken = token,
                chatId = chat,
                audioFile = target,
                caption = "TeleVoice Female Voice Note (${selectedPreset.value.name}) 🌸"
            )
            _isProcessing.value = false

            result.fold(
                onSuccess = { msg -> emitMessage(msg) },
                onFailure = { err -> emitMessage("Bot send failed: ${err.message}") }
            )
        }
    }

    fun saveTelegramBotConfig(token: String, chatId: String) {
        _botToken.value = token.trim()
        _botChatId.value = chatId.trim()
        prefs.edit()
            .putString("tg_bot_token", token.trim())
            .putString("tg_bot_chat_id", chatId.trim())
            .apply()
        emitMessage("Telegram Bot settings saved!")
    }

    fun deleteRecording(item: RecordingItem) {
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        if (_lastModulatedFile.value?.absolutePath == item.filePath) {
            _lastModulatedFile.value = null
        }
        loadSavedRecordings()
        emitMessage("Recording deleted.")
    }

    fun loadSavedRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val dir = File(context.filesDir, "recordings")
            if (!dir.exists()) {
                _savedRecordings.value = emptyList()
                return@launch
            }

            val files = dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()

            val items = files.map { file ->
                val preset = VoicePreset.ALL_PRESETS.firstOrNull { file.name.contains(it.id) } ?: VoicePreset.SweetGirl
                val durationMs = (file.length() / (audioRecordManager.sampleRate * 2) * 1000L).coerceAtLeast(1000L)

                RecordingItem(
                    id = file.name,
                    title = file.nameWithoutExtension.replace("TeleVoice_", "").replace("_", " "),
                    filePath = file.absolutePath,
                    durationMs = durationMs,
                    createdAt = file.lastModified(),
                    presetName = preset.name,
                    presetEmoji = preset.iconEmoji,
                    fileSizeBytes = file.length()
                )
            }

            _savedRecordings.value = items
        }
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _userMessage.emit(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecordManager.stopLiveMonitoring()
        audioRecordManager.stopRecording()
        audioPlayerManager.stop()
    }
}
