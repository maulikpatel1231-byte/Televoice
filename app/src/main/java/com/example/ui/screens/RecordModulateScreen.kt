package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoicePreset
import com.example.telegram.TelegramSender
import com.example.ui.components.AudioPlaybackWaveform
import com.example.ui.components.PitchControlPanel
import com.example.ui.components.PresetSelectorRow
import com.example.ui.components.RealTimeWaveformVisualizer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardVariant
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramBlueDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.VoiceViewModel
import java.util.Locale

@Composable
fun RecordModulateScreen(
    viewModel: VoiceViewModel,
    onOpenTelegramSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val amplitude by viewModel.currentAmplitude.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val lastFile by viewModel.lastModulatedFile.collectAsState()

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()

    val customPitch by viewModel.customPitchSemitones.collectAsState()
    val customFormant by viewModel.customFormantBrightness.collectAsState()
    val customSpeed by viewModel.customSpeedFactor.collectAsState()

    val telegramInstalled = remember(context) {
        TelegramSender.getInstalledTelegramPackage(context) != null
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        // Top Bar Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TeleVoice",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Female Voice Note for Telegram 🌸",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeonPink)
                )
            }

            // Telegram status pill & Settings Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (telegramInstalled) TelegramBlue.copy(alpha = 0.2f) else StudioBorder)
                        .clickable { onOpenTelegramSettings() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (telegramInstalled) "Telegram Ready ✈️" else "Telegram Setup ⚙️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (telegramInstalled) TelegramBlue else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onOpenTelegramSettings,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Telegram Settings",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Active Preset Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedPreset.iconEmoji,
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${selectedPreset.name} (${selectedPreset.hindiName})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = selectedPreset.description,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset selector horizontal list
        Text(
            text = "Select Female Voice Style",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        PresetSelectorRow(
            presets = VoicePreset.ALL_PRESETS,
            selectedPreset = selectedPreset,
            onSelectPreset = { viewModel.selectPreset(it) }
        )

        // Custom Tuning Panel if Custom is selected
        AnimatedVisibility(visible = selectedPreset.id == VoicePreset.Custom.id) {
            PitchControlPanel(
                pitchSemitones = customPitch,
                onPitchChange = { viewModel.setCustomPitch(it) },
                formantBrightness = customFormant,
                onFormantChange = { viewModel.setCustomFormant(it) },
                speedFactor = customSpeed,
                onSpeedChange = { viewModel.setCustomSpeed(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            RealTimeWaveformVisualizer(
                amplitude = amplitude,
                isLive = isRecording
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recording Controls & Big Action Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duration Timer
            if (isRecording) {
                val sec = (recordingDurationMs / 1000) % 60
                val min = (recordingDurationMs / (1000 * 60))
                val timeString = String.format(Locale.US, "%02d:%02d", min, sec)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonPink)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeonPink,
                            fontSize = 24.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            } else {
                Text(
                    text = if (lastFile != null) "Recording Ready! Listen or Send below" else "Tap microphone to record your message",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Big Glowing Mic / Stop Button
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp)
            ) {
                // Pulsing glow when recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(NeonPink.copy(alpha = 0.25f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isRecording) listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                else listOf(NeonPink, NeonPurple)
                            )
                        )
                        .clickable {
                            if (isRecording) {
                                viewModel.stopRecording()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("record_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            Text(
                text = if (isRecording) "Tap to Stop & Modulate" else "Record Audio (माइक पर बोलें)",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRecording) NeonPink else TextPrimary
                ),
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Converted Audio Player & Send to Telegram Card
        if (lastFile != null) {
            ConvertedAudioPlayerCard(
                file = lastFile!!,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onPlayPause = {
                    if (isPlaying) viewModel.pauseAudio() else viewModel.playAudio(lastFile!!.absolutePath)
                },
                onSeek = { viewModel.seekAudio(it) },
                onSendToTelegram = { viewModel.sendToTelegram(context, lastFile) },
                onSendViaBot = { viewModel.sendViaBot(lastFile) },
                selectedPreset = selectedPreset
            )
        }
    }
}

@Composable
fun ConvertedAudioPlayerCard(
    file: java.io.File,
    isPlaying: Boolean,
    currentPositionMs: Int,
    durationMs: Int,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onSendToTelegram: () -> Unit,
    onSendViaBot: () -> Unit,
    selectedPreset: VoicePreset
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("converted_audio_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(TelegramBlue, NeonPink)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = selectedPreset.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Female Voice Ready! 🌸",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${selectedPreset.name} • ${file.name}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Waveform Seek Bar
            val safeDuration = if (durationMs > 0) durationMs else 1
            val progress = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

            AudioPlaybackWaveform(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Tapping on waveform seeks approximately
                    }
            )

            // Slider & Timers
            Slider(
                value = currentPositionMs.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 0f..maxOf(durationMs.toFloat(), 1f),
                colors = SliderDefaults.colors(
                    thumbColor = NeonPink,
                    activeTrackColor = NeonPink,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = formatMs(durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Play / Pause and Telegram Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Play / Pause button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(StudioCardVariant)
                        .border(1.dp, StudioBorder, CircleShape)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = NeonPink,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // DIRECT SEND TO TELEGRAM BUTTON (Highlight Action)
                Button(
                    onClick = onSendToTelegram,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("send_to_telegram_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send to Telegram",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send to Telegram",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Option: Bot Send
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onSendViaBot,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.testTag("send_via_bot_button")
                ) {
                    Text(
                        text = "Send via Bot API 🤖",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
