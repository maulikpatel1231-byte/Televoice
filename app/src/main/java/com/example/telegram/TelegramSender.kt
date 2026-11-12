package com.example.telegram

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object TelegramSender {

    private val TELEGRAM_PACKAGES = listOf(
        "org.telegram.messenger",
        "org.telegram.plus",
        "org.telegram.messenger.web",
        "org.telegram.BETA"
    )

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks if any Telegram app client is installed on the device.
     */
    fun getInstalledTelegramPackage(context: Context): String? {
        val pm = context.packageManager
        for (pkg in TELEGRAM_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
                return pkg
            } catch (e: PackageManager.NameNotFoundException) {
                // Not installed
            }
        }
        return null
    }

    /**
     * Directly launches Telegram with the audio file attached for sending.
     * If Telegram is installed, it opens directly into Telegram's chat selector!
     */
    fun sendAudioToTelegram(context: Context, audioFile: File): Boolean {
        if (!audioFile.exists()) {
            Toast.makeText(context, "Audio file not found!", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, audioFile)

            val telegramPkg = getInstalledTelegramPackage(context)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Voice Note")
                putExtra(Intent.EXTRA_TEXT, "Sent via TeleVoice 🎙️")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (telegramPkg != null) {
                shareIntent.setPackage(telegramPkg)
                context.startActivity(shareIntent)
                return true
            } else {
                // Telegram not found: open chooser and offer Play Store link
                val chooser = Intent.createChooser(shareIntent, "Share voice note to Telegram").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Toast.makeText(context, "Tip: Install Telegram to send directly to chats", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            Log.e("TelegramSender", "Failed to share to Telegram", e)
            Toast.makeText(context, "Error opening Telegram: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    /**
     * Sends voice note directly via Telegram Bot API (`sendVoice` / `sendAudio`).
     * Allows automated sending to a specific Chat ID / Channel / User ID.
     */
    suspend fun sendViaTelegramBot(
        botToken: String,
        chatId: String,
        audioFile: File,
        caption: String = "TeleVoice Female Audio 🌸"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token and Chat ID cannot be empty"))
        }

        if (!audioFile.exists()) {
            return@withContext Result.failure(IllegalArgumentException("Audio file does not exist"))
        }

        try {
            val url = "https://api.telegram.org/bot${botToken.trim()}/sendAudio"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.trim())
                .addFormDataPart("caption", caption)
                .addFormDataPart(
                    "audio",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("Voice note sent to Telegram successfully!")
            } else {
                Result.failure(Exception("Telegram API Error (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Log.e("TelegramSender", "Bot API send failed", e)
            Result.failure(e)
        }
    }
}
