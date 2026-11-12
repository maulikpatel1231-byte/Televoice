package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardVariant
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RealTimeWaveformVisualizer(
    amplitude: Float,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCardVariant)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // Baseline center grid line
            drawLine(
                color = StudioBorder,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.dp.toPx()
            )

            val baseAmp = if (isLive) maxOf(0.12f, amplitude) else 0.05f
            val maxWaveHeight = (height * 0.42f) * baseAmp

            // Primary glowing sine curve
            val path1 = Path()
            path1.moveTo(0f, centerY)
            val segments = 80
            val dx = width / segments

            for (i in 0..segments) {
                val x = i * dx
                val normX = i.toFloat() / segments
                // Envelope window: 0 at ends, 1 at center
                val envelope = sin(normX * PI.toFloat())
                val y = centerY + sin(normX * 4f * PI.toFloat() + phase) * maxWaveHeight * envelope
                if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
            }

            drawPath(
                path = path1,
                brush = Brush.horizontalGradient(
                    colors = listOf(NeonPink, NeonPurple, NeonCyan)
                ),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Secondary harmonic curve
            if (isLive && amplitude > 0.05f) {
                val path2 = Path()
                path2.moveTo(0f, centerY)
                for (i in 0..segments) {
                    val x = i * dx
                    val normX = i.toFloat() / segments
                    val envelope = sin(normX * PI.toFloat())
                    val y = centerY + sin(normX * 6f * PI.toFloat() - phase * 1.3f) * (maxWaveHeight * 0.65f) * envelope
                    if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                }

                drawPath(
                    path = path2,
                    brush = Brush.horizontalGradient(
                        colors = listOf(NeonCyan, NeonPink, NeonPurple)
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Frequency bars across bottom/center
            val barCount = 36
            val barWidth = width / (barCount * 1.5f)
            val barSpacing = width / barCount

            for (b in 0 until barCount) {
                val x = b * barSpacing + barWidth / 2
                val normB = b.toFloat() / barCount
                val barOscillation = (sin(normB * 5f * PI.toFloat() + phase * 2f) + 1f) * 0.5f
                val barHeight = (height * 0.35f) * baseAmp * barOscillation + 4f

                val top = centerY - barHeight / 2
                val bottom = centerY + barHeight / 2

                drawLine(
                    color = if (normB < 0.5f) NeonPink.copy(alpha = 0.55f) else NeonCyan.copy(alpha = 0.55f),
                    start = Offset(x, top),
                    end = Offset(x, bottom),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun AudioPlaybackWaveform(
    progress: Float, // 0.0 to 1.0
    barCount: Int = 36,
    modifier: Modifier = Modifier
) {
    val barHeights = rememberStaticBarProfile(barCount)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / (barCount * 1.6f)
        val spacing = width / barCount

        for (i in 0 until barCount) {
            val x = i * spacing + barWidth / 2
            val normIndex = i.toFloat() / barCount
            val barH = height * barHeights[i] * 0.85f

            val isPlayed = normIndex <= progress
            val color = if (isPlayed) NeonPink else StudioBorder

            drawLine(
                color = color,
                start = Offset(x, centerY - barH / 2),
                end = Offset(x, centerY + barH / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun rememberStaticBarProfile(count: Int): FloatArray {
    return FloatArray(count) { i ->
        val x = i.toFloat() / count
        // Natural speech formants profile
        val h = 0.25f + 0.55f * sin(x * PI.toFloat()) * (0.8f + 0.2f * sin(x * 8f * PI.toFloat()))
        h.coerceIn(0.15f, 0.95f)
    }
}
