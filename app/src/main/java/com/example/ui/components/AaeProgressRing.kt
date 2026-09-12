package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Motion
import com.example.ui.theme.reduceMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ölçüm halkası.
 *
 * Material'ın [androidx.compose.material3.CircularProgressIndicator]'ı yerine
 * yazıldı çünkü üç şeyi yapamıyor:
 *
 * 1. **Boşluklu gösterge (gauge)** — halkanın altında bir açıklık bırakmak, dolu
 *    dairenin aksine "bu bir ölçek, tamamlanma değil" der. Sistem bütünlüğü %100
 *    olduğunda kapanan tam bir daire, ölçeğin nerede başladığını gizler.
 * 2. **Gölge yay (ghost arc)** — değer değiştiğinde nereden geldiğini gösterir.
 *    Ana yay yayla hızlı oturur, gölge yay geriden gelir; aradaki boşluk değişimin
 *    yönünü ve büyüklüğünü tek bakışta verir.
 * 3. **Kertikler (tick)** — geçilen kertikler vurgu renginde, geçilmeyenler yatak
 *    renginde. Yüzde okumaya gerek kalmadan kabaca konum anlaşılır.
 *
 * Belirsiz (indeterminate) kipte yay hem döner hem *uzunluğu değişir*; sabit
 * uzunlukta dönen bir yay mekanik ve bitmeyecekmiş gibi durur.
 */
@Composable
fun AaeProgressRing(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 76.dp,
    strokeWidth: Dp = 4.dp,
    /** Halkanın altındaki açıklık. 0f verilirse tam daire çizilir. */
    gapDegrees: Float = 68f,
    tickCount: Int = 36,
    indeterminate: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    val target = progress.coerceIn(0f, 1f)

    // Ana yay: hızlı ve kararlı oturur.
    val animatedValue by animateFloatAsState(
        targetValue = target,
        animationSpec = Motion.settle(),
        label = "ring_value"
    )
    // Gölge yay: aynı hedefe belirgin biçimde daha yavaş gider, böylece ana yayın
    // arkasında kısa süreli bir fark bırakır.
    val ghostValue by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(Motion.Slow + Motion.Standard, easing = Motion.Gentle),
        label = "ring_ghost"
    )

    val reduce = reduceMotion
    val spinTransition = rememberInfiniteTransition(label = "ring_spin")
    val spin by if (indeterminate && !reduce) {
        spinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = Motion.Linear),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_spin_angle"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
    val breath by if (indeterminate && !reduce) {
        spinTransition.animateFloat(
            initialValue = 0.10f,
            targetValue = 0.62f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = Motion.Gentle),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_spin_sweep"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.35f) }
    }

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx())
            val topLeft = Offset(inset, inset)

            // Compose'da 0° saat 3 yönü, pozitif yön saat ibresi yönü. Açıklığı
            // altta bırakmak için 90°'den (saat 6) yarım açıklık kadar ileriden başla.
            val totalSweep = 360f - gapDegrees
            val startAngle = 90f + gapDegrees / 2f

            // --- yatak
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            if (indeterminate) {
                drawArc(
                    color = color.copy(alpha = 0.85f),
                    startAngle = startAngle + spin,
                    sweepAngle = totalSweep * breath,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            } else {
                // --- gölge yay (yalnızca ana yayın gerisindeyken görünür)
                if (ghostValue > animatedValue + 0.004f) {
                    drawArc(
                        color = color.copy(alpha = 0.28f),
                        startAngle = startAngle + totalSweep * animatedValue,
                        sweepAngle = totalSweep * (ghostValue - animatedValue),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }

                // --- ana yay
                if (animatedValue > 0f) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = totalSweep * animatedValue,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
            }

            // --- kertikler
            if (tickCount > 0) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f - strokeWidth.toPx() - 2.dp.toPx()
                val tickLength = 3.dp.toPx()
                for (i in 0..tickCount) {
                    val fraction = i / tickCount.toFloat()
                    val angleDeg = startAngle + totalSweep * fraction
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()
                    val passed = !indeterminate && fraction <= animatedValue
                    drawLine(
                        color = if (passed) color.copy(alpha = 0.55f) else trackColor.copy(alpha = 0.7f),
                        start = Offset(
                            center.x + cosA * (outerRadius - tickLength),
                            center.y + sinA * (outerRadius - tickLength)
                        ),
                        end = Offset(center.x + cosA * outerRadius, center.y + sinA * outerRadius),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        content()
    }
}
