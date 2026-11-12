package com.example.ui.screens

import android.Manifest
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.PitchControlPanel
import com.example.ui.components.PresetSelectorRow
import com.example.ui.components.RealTimeWaveformVisualizer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.VoiceViewModel

@Composable
fun LiveMicScreen(
    viewModel: VoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isLive by viewModel.isLiveMonitoring.collectAsState()
    val amplitude by viewModel.currentAmplitude.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()

    val customPitch by viewModel.customPitchSemitones.collectAsState()
    val customFormant by viewModel.customFormantBrightness.collectAsState()
    val customSpeed by viewModel.customSpeedFactor.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleLiveMonitoring()
        } else {
            Toast.makeText(context, "Microphone permission is required for live voice modulation", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Real-Time Modulation",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Speak into mic & hear live female voice in earphones 🎧",
                style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan)
            )
        }

        // Headphone safety banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardBg),
            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Headphones",
                    tint = WarningAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Headphones / Earphones Recommended",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    )
                    Text(
                        text = "Using earphones prevents speaker-to-mic feedback echo while talking.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Live Mic Toggle Center
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                if (isLive) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.25f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isLive) listOf(NeonCyan, Color(0xFF0091EA))
                                else listOf(StudioCardVariant, StudioBorder)
                            )
                        )
                        .clickable {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        .testTag("live_mic_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLive) Icons.Default.VolumeUp else Icons.Default.MicOff,
                        contentDescription = "Toggle Live Voice Modulation",
                        tint = if (isLive) Color.Black else TextSecondary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isLive) "LIVE MODULATION ON 🔴" else "TAP TO START LIVE MIC",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isLive) NeonCyan else TextSecondary
                )
            )

            Text(
                text = if (isLive) "Low-Latency DSP Active • 22kHz 16-Bit" else "Talk and listen to your voice pitch shift in real-time",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Waveform Oscilloscope
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            RealTimeWaveformVisualizer(
                amplitude = amplitude,
                isLive = isLive
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Female Presets
        Text(
            text = "Switch Female Voice Model Live",
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

        Spacer(modifier = Modifier.height(12.dp))

        // Real-time pitch control sliders
        PitchControlPanel(
            pitchSemitones = customPitch,
            onPitchChange = { viewModel.setCustomPitch(it) },
            formantBrightness = customFormant,
            onFormantChange = { viewModel.setCustomFormant(it) },
            speedFactor = customSpeed,
            onSpeedChange = { viewModel.setCustomSpeed(it) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
