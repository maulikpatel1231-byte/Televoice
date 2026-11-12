package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoicePreset
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PresetSelectorRow(
    presets: List<VoicePreset>,
    selectedPreset: VoicePreset,
    onSelectPreset: (VoicePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            val isSelected = preset.id == selectedPreset.id
            PresetCard(
                preset = preset,
                isSelected = isSelected,
                onClick = { onSelectPreset(preset) }
            )
        }
    }
}

@Composable
fun PresetCard(
    preset: VoicePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val border = if (isSelected) {
        BorderStroke(2.dp, Brush.horizontalGradient(listOf(NeonPink, NeonPurple)))
    } else {
        BorderStroke(1.dp, StudioBorder)
    }

    val containerColor = if (isSelected) StudioCardVariant else StudioCardBg

    Card(
        modifier = Modifier
            .width(135.dp)
            .height(130.dp)
            .testTag("preset_card_${preset.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(preset.badgeColorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.iconEmoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1
            )

            Text(
                text = preset.hindiName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isSelected) NeonPink else TextMuted,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Pitch badge
            val pitchBadge = if (preset.id == VoicePreset.Custom.id) {
                "Adjustable"
            } else {
                "+${String.format(java.util.Locale.US, "%.1f", preset.pitchShiftSemitones)} st"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NeonPink.copy(alpha = 0.15f) else StudioBorder.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = pitchBadge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) NeonPink else TextMuted
                )
            }
        }
    }
}
