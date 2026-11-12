package com.example.dsp

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility to write standard RIFF WAV files that are natively recognized by
 * Android, Telegram, and standard media players.
 */
object WavUtils {

    fun writePcmToWav(file: File, pcmData: ShortArray, sampleRate: Int = 22050, channels: Int = 1) {
        val totalAudioLen = pcmData.size * 2
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2

        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            buffer.put('R'.code.toByte())
            buffer.put('I'.code.toByte())
            buffer.put('F'.code.toByte())
            buffer.put('F'.code.toByte())
            buffer.putInt(totalDataLen)
            buffer.put('W'.code.toByte())
            buffer.put('A'.code.toByte())
            buffer.put('V'.code.toByte())
            buffer.put('E'.code.toByte())

            // 'fmt ' chunk
            buffer.put('f'.code.toByte())
            buffer.put('m'.code.toByte())
            buffer.put('t'.code.toByte())
            buffer.put(' '.code.toByte())
            buffer.putInt(16) // Subchunk1Size for PCM
            buffer.putShort(1) // AudioFormat 1 = PCM
            buffer.putShort(channels.toShort())
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort((channels * 2).toShort()) // block align
            buffer.putShort(16) // bits per sample

            // 'data' chunk
            buffer.put('d'.code.toByte())
            buffer.put('a'.code.toByte())
            buffer.put('t'.code.toByte())
            buffer.put('a'.code.toByte())
            buffer.putInt(totalAudioLen)

            out.write(header)

            val byteBuf = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcmData) {
                byteBuf.putShort(s)
            }
            out.write(byteBuf.array())
        }
    }
}
