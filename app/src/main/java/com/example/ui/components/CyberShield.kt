package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CyberShield(
    isRealTimeActive: Boolean,
    hasThreats: Boolean,
    isScanning: Boolean,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_angle"
    )

    val primaryColor = when {
        hasThreats -> androidx.compose.material3.MaterialTheme.colorScheme.error
        !isRealTimeActive -> androidx.compose.material3.MaterialTheme.colorScheme.error
        isScanning -> androidx.compose.material3.MaterialTheme.colorScheme.primary
        else -> androidx.compose.material3.MaterialTheme.colorScheme.secondary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = this.center
            val maxRadius = this.size.minDimension / 2

            // Outer pulsing wave
            if (isRealTimeActive || isScanning) {
                drawCircle(
                    color = primaryColor.copy(alpha = 0.12f),
                    radius = maxRadius * pulseScale,
                    center = center
                )
                drawCircle(
                    color = primaryColor.copy(alpha = 0.22f),
                    radius = maxRadius * 0.95f,
                    center = center
                )
            }

            // High tech segmented radar arc
            drawArc(
                color = primaryColor.copy(alpha = 0.85f),
                startAngle = if (isScanning) rotateAngle else 30f,
                sweepAngle = if (isScanning) 140f else 280f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Inner security circle
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = maxRadius * 0.68f,
                center = center
            )
        }

        // Center Icon
        val icon = when {
            hasThreats -> Icons.Default.Dangerous
            !isRealTimeActive -> Icons.Default.Warning
            else -> Icons.Default.Security
        }

        Icon(
            imageVector = icon,
            contentDescription = "Güvenlik Kalkanı",
            tint = primaryColor,
            modifier = Modifier.size(size * 0.44f)
        )
    }
}
