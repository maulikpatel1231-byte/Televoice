package com.example.model

/**
 * Voice presets specifically designed for female voice transformation.
 * Male voice typically ranges from 85-180 Hz fundamental frequency (F0).
 * Female voice typically ranges from 165-260 Hz, with brighter formants
 * in the 2.5kHz - 4.5kHz range.
 */
data class VoicePreset(
    val id: String,
    val name: String,
    val hindiName: String,
    val description: String,
    val pitchShiftSemitones: Float, // e.g. +4.5 semitones
    val formantBrightness: Float,   // 0.0 to 1.0 (treble shelf boost)
    val resonanceGain: Float,       // Q / peak warmth
    val speedFactor: Float = 1.0f,  // tempo preservation
    val iconEmoji: String,
    val badgeColorHex: Long
) {
    companion object {
        val SweetGirl = VoicePreset(
            id = "sweet_girl",
            name = "Sweet Girl",
            hindiName = "प्यारी लड़की",
            description = "Gentle, youthful and sweet feminine tone",
            pitchShiftSemitones = 4.8f, // ~1.32x pitch
            formantBrightness = 0.65f,
            resonanceGain = 1.2f,
            iconEmoji = "🌸",
            badgeColorHex = 0xFFFF4081
        )

        val CuteAnime = VoicePreset(
            id = "cute_anime",
            name = "Anime Waifu",
            hindiName = "क्यूट एनीमे",
            description = "High pitch, vibrant and adorable anime girl voice",
            pitchShiftSemitones = 6.8f, // ~1.48x pitch
            formantBrightness = 0.85f,
            resonanceGain = 1.4f,
            iconEmoji = "🎀",
            badgeColorHex = 0xFFFF1493
        )

        val NaturalWoman = VoicePreset(
            id = "natural_woman",
            name = "Natural Woman",
            hindiName = "नेचुरल महिला",
            description = "Mature, balanced and realistic woman voice",
            pitchShiftSemitones = 3.2f, // ~1.20x pitch
            formantBrightness = 0.45f,
            resonanceGain = 1.1f,
            iconEmoji = "💃",
            badgeColorHex = 0xFF9C27B0
        )

        val SoftFemale = VoicePreset(
            id = "soft_female",
            name = "Soft & Whisper",
            hindiName = "सॉफ्ट आवाज़",
            description = "Calm, airy and soft spoken girl voice",
            pitchShiftSemitones = 3.9f, // ~1.25x pitch
            formantBrightness = 0.50f,
            resonanceGain = 0.9f,
            iconEmoji = "🎙️",
            badgeColorHex = 0xFF00BCD4
        )

        val CyberGirl = VoicePreset(
            id = "cyber_girl",
            name = "Cyber Robo-Girl",
            hindiName = "रोबो गर्ल",
            description = "Futuristic AI female voice with slight electronic edge",
            pitchShiftSemitones = 4.4f, // ~1.29x pitch
            formantBrightness = 0.75f,
            resonanceGain = 1.5f,
            iconEmoji = "🤖",
            badgeColorHex = 0xFF00E676
        )

        val Custom = VoicePreset(
            id = "custom",
            name = "Custom Tuning",
            hindiName = "कस्टम ट्यूनिंग",
            description = "Manually adjust pitch, formant and fine frequencies",
            pitchShiftSemitones = 4.5f,
            formantBrightness = 0.6f,
            resonanceGain = 1.0f,
            iconEmoji = "🎛️",
            badgeColorHex = 0xFFFF9800
        )

        val ALL_PRESETS = listOf(SweetGirl, CuteAnime, NaturalWoman, SoftFemale, CyberGirl, Custom)
    }
}

data class RecordingItem(
    val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long,
    val presetName: String,
    val presetEmoji: String,
    val fileSizeBytes: Long
)
