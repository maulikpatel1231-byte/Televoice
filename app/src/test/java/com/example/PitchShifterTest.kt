package com.example

import com.example.dsp.PitchShifter
import com.example.dsp.WavUtils
import com.example.model.VoicePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.sin

class PitchShifterTest {

    @Test
    fun testSemitonesToRatio() {
        val shifter = PitchShifter(22050)
        // 0 semitones should be exactly 1.0 ratio
        assertEquals(1.0f, shifter.semitonesToRatio(0f), 0.001f)
        // +12 semitones (one octave) should be 2.0 ratio
        assertEquals(2.0f, shifter.semitonesToRatio(12f), 0.001f)
        // +4.8 semitones (Sweet Girl) should be ~1.3195
        assertTrue(shifter.semitonesToRatio(4.8f) > 1.30f)
    }

    @Test
    fun testRealTimeBufferProcessing() {
        val shifter = PitchShifter(22050)
        val input = ShortArray(512) { i ->
            (sin(i * 0.1) * 10000).toInt().toShort()
        }
        val output = ShortArray(512)

        shifter.processRealTime(
            input = input,
            output = output,
            length = 512,
            pitchSemitones = 4.8f,
            formantBrightness = 0.65f
        )

        // Ensure output is non-empty and has modulated samples
        var hasNonZero = false
        for (sample in output) {
            if (sample != 0.toShort()) {
                hasNonZero = true
                break
            }
        }
        assertTrue(hasNonZero)
    }

    @Test
    fun testFullRecordingProcessingAndWavExport() {
        val shifter = PitchShifter(22050)
        val dummyPcm = ShortArray(2205) { i ->
            (sin(i * 0.05) * 12000).toInt().toShort()
        }

        val modulated = shifter.processFullRecording(
            rawPcm = dummyPcm,
            pitchSemitones = VoicePreset.SweetGirl.pitchShiftSemitones,
            formantBrightness = VoicePreset.SweetGirl.formantBrightness
        )

        assertTrue(modulated.isNotEmpty())

        val tempWav = File.createTempFile("test_voice", ".wav")
        try {
            WavUtils.writePcmToWav(tempWav, modulated, 22050, 1)
            assertTrue(tempWav.exists())
            // 44 bytes header + modulated.size * 2
            assertEquals(44L + modulated.size * 2L, tempWav.length())
        } finally {
            tempWav.delete()
        }
    }

    @Test
    fun testVoicePresetsConfigured() {
        val presets = VoicePreset.ALL_PRESETS
        assertTrue(presets.isNotEmpty())
        assertNotNull(VoicePreset.SweetGirl)
        assertNotNull(VoicePreset.CuteAnime)
        assertNotNull(VoicePreset.NaturalWoman)
        assertTrue(VoicePreset.CuteAnime.pitchShiftSemitones > VoicePreset.NaturalWoman.pitchShiftSemitones)
    }
}
