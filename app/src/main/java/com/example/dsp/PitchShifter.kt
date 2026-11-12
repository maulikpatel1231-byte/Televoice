package com.example.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance DSP Voice Modulation Engine.
 * Supports both real-time low-latency streaming (for live mic monitoring)
 * and high-fidelity block processing (for recorded voice notes).
 */
class PitchShifter(private val sampleRate: Int = 22050) {

    // Dual-tap rotating delay line for low-latency real-time live modulation
    private val delayBufferSize = 4096
    private val delayBuffer = FloatArray(delayBufferSize)
    private var writeIndex = 0
    private var phase1 = 0.0f
    private var phase2 = 0.5f

    // Biquad EQ state for female formant boosting
    private var eqX1 = 0.0f
    private var eqX2 = 0.0f
    private var eqY1 = 0.0f
    private var eqY2 = 0.0f

    // High pass filter state
    private var hpX1 = 0.0f
    private var hpY1 = 0.0f

    /**
     * Converts pitch semitones to frequency shift ratio.
     * E.g. +4 semitones -> 2^(4/12) ≈ 1.2599
     */
    fun semitonesToRatio(semitones: Float): Float {
        return 2.0f.pow(semitones / 12.0f)
    }

    /**
     * Resets internal delay buffers and filter history.
     */
    fun reset() {
        delayBuffer.fill(0f)
        writeIndex = 0
        phase1 = 0.0f
        phase2 = 0.5f
        eqX1 = 0f; eqX2 = 0f; eqY1 = 0f; eqY2 = 0f
        hpX1 = 0f; hpY1 = 0f
    }

    /**
     * Processes a live PCM 16-bit audio buffer in real time with near-zero latency.
     * Ideal for live mic earphone monitoring!
     */
    fun processRealTime(
        input: ShortArray,
        output: ShortArray,
        length: Int,
        pitchSemitones: Float,
        formantBrightness: Float
    ) {
        val pitchRatio = semitonesToRatio(pitchSemitones)
        // Rate of phase advancement inside the delay window
        val windowSize = 1024f
        val rate = (pitchRatio - 1.0f) / windowSize

        // EQ coefficients for female formant enhancement
        val (b0, b1, b2, a1, a2) = calculatePeakingEqCoefficients(
            centerFreq = 3200f,
            q = 1.2f,
            gainDb = 4.0f + (formantBrightness * 6.0f),
            sampleRate = sampleRate.toFloat()
        )

        for (i in 0 until length) {
            val sample = input[i] / 32768.0f

            // Write to circular delay buffer
            delayBuffer[writeIndex] = sample

            // Update phases
            phase1 += rate
            if (phase1 >= 1.0f) phase1 -= 1.0f
            if (phase1 < 0.0f) phase1 += 1.0f

            phase2 += rate
            if (phase2 >= 1.0f) phase2 -= 1.0f
            if (phase2 < 0.0f) phase2 += 1.0f

            // Delay positions
            val delay1 = phase1 * windowSize
            val delay2 = phase2 * windowSize

            val rIdx1 = (writeIndex - delay1.toInt() + delayBufferSize) % delayBufferSize
            val rIdx2 = (writeIndex - delay2.toInt() + delayBufferSize) % delayBufferSize

            val s1 = delayBuffer[rIdx1]
            val s2 = delayBuffer[rIdx2]

            // Triangular / Hann cross-fade envelope
            val w1 = 0.5f * (1.0f - cos(2.0f * PI.toFloat() * phase1))
            val w2 = 0.5f * (1.0f - cos(2.0f * PI.toFloat() * phase2))

            var mixed = (s1 * w1 + s2 * w2)

            // High pass filter at 130Hz to remove male chest bass rumble
            val hpAlpha = 0.95f
            val hpOut = hpAlpha * (hpY1 + mixed - hpX1)
            hpX1 = mixed
            hpY1 = hpOut
            mixed = hpOut

            // Female vocal formant peak EQ
            val eqOut = b0 * mixed + b1 * eqX1 + b2 * eqX2 - a1 * eqY1 - a2 * eqY2
            eqX2 = eqX1
            eqX1 = mixed
            eqY2 = eqY1
            eqY1 = eqOut

            // Soft saturation limiter to prevent digital clipping
            val limited = softLimit(eqOut * 1.15f)

            // Output back to short
            output[i] = (limited * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()

            writeIndex = (writeIndex + 1) % delayBufferSize
        }
    }

    /**
     * High-quality offline block processing for full voice recordings.
     * Uses SOLA (Synchronized Overlap-Add) pitch shifting to produce crisp,
     * natural female speech without robotic artifacts.
     */
    fun processFullRecording(
        rawPcm: ShortArray,
        pitchSemitones: Float,
        formantBrightness: Float,
        speedFactor: Float = 1.0f
    ): ShortArray {
        val pitchRatio = semitonesToRatio(pitchSemitones)
        if (rawPcm.isEmpty()) return ShortArray(0)

        // Resampling step: resample by pitchRatio
        val resampled = resampleLinear(rawPcm, pitchRatio)

        // Time stretch step: stretch by (pitchRatio * speedFactor) to restore original speech duration
        val stretchRatio = pitchRatio * speedFactor
        val stretched = timeStretchWsola(resampled, stretchRatio, sampleRate)

        // Post-processing: Formant EQ + High pass + gentle presence boost
        return applyPostVoiceFilters(stretched, formantBrightness)
    }

    /**
     * Resamples audio array by given ratio using linear interpolation.
     */
    private fun resampleLinear(input: ShortArray, ratio: Float): FloatArray {
        if (ratio <= 0f) return FloatArray(input.size) { input[it].toFloat() }
        val outputLen = (input.size / ratio).toInt()
        val output = FloatArray(outputLen)

        for (i in 0 until outputLen) {
            val srcPos = i * ratio
            val srcIdx = srcPos.toInt()
            val frac = srcPos - srcIdx
            val s0 = if (srcIdx < input.size) input[srcIdx] / 32768.0f else 0f
            val s1 = if (srcIdx + 1 < input.size) input[srcIdx + 1] / 32768.0f else s0
            output[i] = s0 + frac * (s1 - s0)
        }
        return output
    }

    /**
     * Time-stretch using Synchronized Overlap-Add (SOLA).
     * Maintains pitch while expanding or compressing time.
     */
    private fun timeStretchWsola(input: FloatArray, stretchFactor: Float, sampleRate: Int): FloatArray {
        if (input.isEmpty() || stretchFactor <= 0.05f) return input

        // Window size ~25ms
        val frameSize = (sampleRate * 0.028f).toInt()
        val analysisHop = (frameSize * 0.5f).toInt()
        val synthesisHop = (analysisHop * stretchFactor).toInt().coerceAtLeast(1)

        val outputLen = (input.size * stretchFactor).toInt() + frameSize
        val output = FloatArray(outputLen)
        val window = FloatArray(frameSize) { i ->
            0.5f * (1.0f - cos(2.0f * PI.toFloat() * i / (frameSize - 1)))
        }

        var inPos = 0
        var outPos = 0

        // Copy first frame
        val initialCopyLen = min(frameSize, input.size)
        for (i in 0 until initialCopyLen) {
            output[i] = input[i] * window[i]
        }
        outPos += synthesisHop
        inPos += analysisHop

        val maxSearch = (frameSize * 0.4f).toInt()

        while (inPos + frameSize < input.size && outPos + frameSize < output.size) {
            // Find best cross-correlation alignment to prevent phase cancellations
            var bestOffset = 0
            var bestCorr = -Float.MAX_VALUE

            val searchStart = max(0, inPos - maxSearch)
            val searchEnd = min(input.size - frameSize, inPos + maxSearch)

            for (candidate in searchStart..searchEnd step 2) {
                var corr = 0.0f
                for (k in 0 until frameSize step 4) {
                    val outSample = output[outPos + k]
                    val inSample = input[candidate + k]
                    corr += outSample * inSample
                }
                if (corr > bestCorr) {
                    bestCorr = corr
                    bestOffset = candidate - inPos
                }
            }

            val alignedPos = inPos + bestOffset

            // Overlap-add with Hanning window
            for (k in 0 until frameSize) {
                if (alignedPos + k < input.size && outPos + k < output.size) {
                    output[outPos + k] += input[alignedPos + k] * window[k]
                }
            }

            inPos += analysisHop
            outPos += synthesisHop
        }

        // Normalize and trim
        val finalLen = min(outputLen, (input.size * stretchFactor).toInt())
        val result = FloatArray(finalLen)
        System.arraycopy(output, 0, result, 0, finalLen)
        return result
    }

    /**
     * Applies vocal tract resonance and high-pass filtering to enhance female voice clarity.
     */
    private fun applyPostVoiceFilters(audio: FloatArray, formantBrightness: Float): ShortArray {
        val result = ShortArray(audio.size)
        val (b0, b1, b2, a1, a2) = calculatePeakingEqCoefficients(
            centerFreq = 3300f,
            q = 1.3f,
            gainDb = 4.5f + (formantBrightness * 7.0f),
            sampleRate = sampleRate.toFloat()
        )

        var x1 = 0f; var x2 = 0f
        var y1 = 0f; var y2 = 0f
        var hpX = 0f; var hpY = 0f

        for (i in audio.indices) {
            val raw = audio[i]

            // Low-cut below 130 Hz (removes male chest base)
            val hpAlpha = 0.96f
            val hpOut = hpAlpha * (hpY + raw - hpX)
            hpX = raw
            hpY = hpOut

            // Formant EQ peaking filter (adds sweet female clarity)
            val eqOut = b0 * hpOut + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = hpOut
            y2 = y1
            y1 = eqOut

            // Gentle saturation limiter
            val limited = softLimit(eqOut * 1.25f)
            result[i] = (limited * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    /**
     * Soft limiter function using hyperbolic tangent curve.
     */
    private fun softLimit(x: Float): Float {
        return if (x > 1.0f) {
            1.0f - (1.0f / (1.0f + (x - 1.0f)))
        } else if (x < -1.0f) {
            -1.0f + (1.0f / (1.0f + (-x - 1.0f)))
        } else {
            x - (x * x * x) / 6.0f
        }
    }

    /**
     * Standard Audio Biquad peaking EQ formula.
     */
    private data class BiquadCoeffs(val b0: Float, val b1: Float, val b2: Float, val a1: Float, val a2: Float)

    private fun calculatePeakingEqCoefficients(
        centerFreq: Float,
        q: Float,
        gainDb: Float,
        sampleRate: Float
    ): BiquadCoeffs {
        val a = 10.0f.pow(gainDb / 40.0f)
        val w0 = (2.0f * PI.toFloat() * centerFreq / sampleRate)
        val alpha = sin(w0) / (2.0f * q)

        val b0 = 1.0f + alpha * a
        val b1 = -2.0f * cos(w0)
        val b2 = 1.0f - alpha * a
        val a0 = 1.0f + alpha / a
        val a1 = -2.0f * cos(w0)
        val a2 = 1.0f - alpha / a

        return BiquadCoeffs(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }
}
