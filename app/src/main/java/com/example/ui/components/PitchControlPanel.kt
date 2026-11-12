package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PitchControlPanel(
    pitchSemitones: Float,
    onPitchChange: (Float) -> Unit,
    formantBrightness: Float,
    onFormantChange: (Float) -> Unit,
    speedFactor: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCardBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Fine Tune Female Voice (आवाज़ एडजस्ट करें)",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Pitch Shift Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Female Pitch (आवाज़ का तीखापन)",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
            Text(
                text = "+${String.format(Locale.US, "%.1f", pitchSemitones)} st",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
            )
        }

        Slider(
            value = pitchSemitones,
            onValueChange = onPitchChange,
            valueRange = 1.0f..10.0f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = NeonPink,
                activeTrackColor = NeonPink,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pitch_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Formant Brightness Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Formant / Vocal Air (क्लैरिटी)",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
            Text(
                text = "${(formantBrightness * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            )
        }

        Slider(
            value = formantBrightness,
            onValueChange = onFormantChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("formant_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Speed / Tempo slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speech Speed (बोलने की गति)",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
            Text(
                text = "${String.format(Locale.US, "%.2f", speedFactor)}x",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Slider(
            value = speedFactor,
            onValueChange = onSpeedChange,
            valueRange = 0.85f..1.25f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = TextPrimary,
                activeTrackColor = TextSecondary,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("speed_slider")
        )

        Text(
            text = "Higher pitch gives younger female sound; Formant boost adds vocal clarity.",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
