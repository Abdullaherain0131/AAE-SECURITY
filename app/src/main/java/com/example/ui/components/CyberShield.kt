package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.motion.rememberBreathPhase
import com.example.ui.motion.rememberLoopPhase
import com.example.ui.motion.reduceMotion
import com.example.ui.theme.LocalAaeColors
import com.example.ui.theme.Motion
import kotlin.math.cos
import kotlin.math.sin

/**
 * Katmanlı koruma göstergesi.
 *
 * ## Öncesi
 *
 * Eski sürüm iki daire, bir yay ve bir ikondu; ölçek 0.85–1.15 arasında nabız
 * atıyor, yay 4 saniyede tur atıyordu. İki sorunu vardı: nabız `alpha 0.12`
 * dolu bir daire üzerinde çalıştığı için ekranda büyüyüp küçülen bir ışık lekesi
 * gibi görünüyordu (tam olarak kaçınılmak istenen etki), ve dört durumun üçü
 * (`hasThreats`, `!isRealTimeActive`, `isScanning`) tek bir renge indirgeniyordu.
 *
 * ## Katmanlar
 *
 * Dıştan içe, hepsi mat:
 *
 * 1. **Sonar halkası** — koruma etkinken dışa doğru genişleyip sönen tek piksel
 *    kalınlığında bir çember. Dolu daire değil, çizgi: ışık lekesi oluşturmaz.
 *    Bu halka "sistem canlı" bilgisini taşır; koruma kapalıyken hiç çizilmez.
 * 2. **Çentik halkası** — 48 çentik, çok yavaş döner. Tarama sırasında süpürme
 *    başının önündeki çentikler parlar; ilerlemenin nerede olduğunu bu gösterir.
 * 3. **Karşı yönde iki yay** — biri saat yönünde, diğeri tersine. Farklı hızlarda
 *    olduğu için desen hiç tekrar etmez, ama ikisi de düşük alfada durur.
 * 4. **Radar süpürmesi** — yalnızca [isScanning] iken. Baş + arkasında sönümlenen
 *    12 parçalı kuyruk. Kuyruğun alfası karesel azalır; doğrusal azalma "iz" değil
 *    "kalın çizgi" olarak okunuyor.
 * 5. **İç disk** — kart zemininden bir ton yüksek, saç teli kenarlıklı. İkonun
 *    okunabilmesi için gerekli sakin alan.
 * 6. **İkon** — durumlar arasında çapraz geçiş (crossfade); ani takla atmaz.
 *
 * ## Hareket azaltma
 *
 * Sistemde animasyonlar kapalıysa dönen/süpüren/nefes alan katmanlar durur ve
 * bileşen tek bir okunaklı durağan kare olarak çizilir — bilgi kaybı yok, hareket
 * yok.
 */
@Composable
fun CyberShield(
    isRealTimeActive: Boolean,
    hasThreats: Boolean,
    isScanning: Boolean,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier,
    /** 0f..1f — verildiğinde çentik halkası ilerleme çubuğu gibi dolar. */
    scanProgress: Float = 0f
) {
    val aae = LocalAaeColors.current
    val reduced = reduceMotion

    val targetAccent = when {
        hasThreats -> aae.danger
        !isRealTimeActive -> aae.warning
        isScanning -> aae.info
        else -> aae.safe
    }
    // Durum değişimi ani renk sıçraması yerine yumuşak geçişle bildirilir:
    // "güvenli → tehdit" geçişini gözün yakalaması için 420 ms yeterli.
    val accent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(Motion.Emphasized, easing = Motion.Standard_Easing),
        label = "shieldAccent"
    )
    val progress by animateFloatAsState(
        targetValue = scanProgress.coerceIn(0f, 1f),
        animationSpec = Motion.settle(),
        label = "shieldProgress"
    )

    val tickPhase by rememberLoopPhase(active = true, durationMillis = 26_000)
    val counterPhase by rememberLoopPhase(active = true, durationMillis = 11_000)
    val sweepPhase by rememberLoopPhase(active = isScanning, durationMillis = 1_900)
    val sonarPhase by rememberLoopPhase(active = isRealTimeActive && !hasThreats, durationMillis = 3_400)
    val breath by rememberBreathPhase(active = isRealTimeActive, durationMillis = 3_600)

    val surfaceLow = aae.surfaceLow
    val surfaceHigh = aae.surfaceHigh
    val border = aae.border

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2f

            drawBaseWell(surfaceLow, radius)

            if (isRealTimeActive && !hasThreats && !reduced) {
                drawSonarRing(sonarPhase, accent, radius)
            }

            drawTickRing(
                phase = if (reduced) 0f else tickPhase,
                sweepPhase = if (reduced) 0.25f else sweepPhase,
                isScanning = isScanning,
                progress = progress,
                accent = accent,
                idleColor = border,
                radius = radius
            )

            drawCounterArcs(
                phase = if (reduced) 0f else tickPhase,
                counterPhase = if (reduced) 0.5f else counterPhase,
                accent = accent,
                radius = radius
            )

            if (isScanning && !reduced) {
                drawRadarSweep(sweepPhase, accent, radius)
            }

            drawInnerDisc(
                breath = if (reduced) 0.5f else breath,
                fill = surfaceHigh,
                outline = border,
                accent = accent,
                radius = radius
            )
        }

        val icon: ImageVector = when {
            hasThreats -> Icons.Default.Dangerous
            !isRealTimeActive -> Icons.Default.Warning
            else -> Icons.Default.Security
        }

        Crossfade(
            targetState = icon,
            animationSpec = tween(Motion.Standard, easing = Motion.Standard_Easing),
            label = "shieldIcon"
        ) { current ->
            Icon(
                imageVector = current,
                contentDescription = when {
                    hasThreats -> "Tehdit tespit edildi"
                    !isRealTimeActive -> "Gerçek zamanlı koruma kapalı"
                    isScanning -> "Tarama sürüyor"
                    else -> "Koruma etkin"
                },
                tint = accent,
                modifier = Modifier.size(size * 0.30f)
            )
        }
    }
}

/** Merkeze doğru çok hafif açılan zemin. Kenarı yok; nerede bittiği belli olmaz. */
private fun DrawScope.drawBaseWell(surfaceLow: Color, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(surfaceLow.copy(alpha = 0.85f), surfaceLow.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * Dışa doğru genişleyip sönen tek çizgilik çember.
 *
 * Kalınlık da alfa ile birlikte azalır: sabit kalınlıkta bir halka uzaklaştıkça
 * "yaklaşan bir nesne" gibi görünüyor, incelen halka ise dağılan bir dalga gibi.
 */
private fun DrawScope.drawSonarRing(phase: Float, accent: Color, radius: Float) {
    val t = phase
    val ringRadius = radius * (0.62f + 0.38f * t)
    val fade = (1f - t)
    drawCircle(
        color = accent.copy(alpha = 0.22f * fade * fade),
        radius = ringRadius,
        center = center,
        style = Stroke(width = (1.4f * fade + 0.4f).dp.toPx())
    )
}

/**
 * 48 çentikli dış halka.
 *
 * Tarama yokken çentikler eşit ve sönük — durağan bir ölçek. Tarama varken
 * süpürme başının **önündeki** 6 çentik parlar: ışık, radar kolunun gideceği yeri
 * gösterir, böylece dönüş yönü tek bakışta anlaşılır. [progress] verilmişse
 * çentiklerin o oranı kalıcı olarak vurgu renginde kalır ve halka bir ilerleme
 * çubuğu gibi dolar.
 */
private fun DrawScope.drawTickRing(
    phase: Float,
    sweepPhase: Float,
    isScanning: Boolean,
    progress: Float,
    accent: Color,
    idleColor: Color,
    radius: Float
) {
    val tickCount = 48
    val outer = radius * 0.97f
    val inner = radius * 0.90f
    val tau = (2 * Math.PI).toFloat()
    val rotation = phase * tau / tickCount * 4f   // çok yavaş: bir çentiğin dörtte biri kadar

    val sweepIndex = sweepPhase * tickCount

    for (i in 0 until tickCount) {
        val angle = rotation + i.toFloat() / tickCount * tau - (Math.PI / 2).toFloat()
        val filled = i.toFloat() / tickCount <= progress

        // Süpürme başına olan mesafe (halka üzerinde, sarmalı hesaba katarak).
        val raw = (i - sweepIndex + tickCount) % tickCount
        val leadDistance = if (raw > tickCount / 2f) tickCount - raw else raw
        val lead = if (isScanning) (1f - (leadDistance / 6f)).coerceIn(0f, 1f) else 0f

        val color = when {
            lead > 0f -> accent.copy(alpha = 0.30f + 0.55f * lead)
            filled -> accent.copy(alpha = 0.55f)
            else -> idleColor.copy(alpha = 0.45f)
        }
        val length = if (i % 6 == 0) 1f else 0.55f   // her 6 çentikte bir uzun işaret
        val start = Offset(
            center.x + cos(angle) * (outer - (outer - inner) * length),
            center.y + sin(angle) * (outer - (outer - inner) * length)
        )
        val end = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer)
        drawLine(color = color, start = start, end = end, strokeWidth = 1.6f, cap = StrokeCap.Round)
    }
}

/**
 * Ters yönlerde dönen iki ince yay.
 *
 * Periyotları asal olmayan ama oranı irrasyonele yakın seçildi (26 s / 11 s):
 * desen pratikte hiç tekrar etmez, bu yüzden göz onu bir döngü olarak ezberleyip
 * görmezden gelmez.
 */
private fun DrawScope.drawCounterArcs(
    phase: Float,
    counterPhase: Float,
    accent: Color,
    radius: Float
) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)

    val outerR = radius * 0.84f
    drawArc(
        color = accent.copy(alpha = 0.34f),
        startAngle = phase * 360f,
        sweepAngle = 96f,
        useCenter = false,
        topLeft = Offset(center.x - outerR, center.y - outerR),
        size = Size(outerR * 2, outerR * 2),
        style = stroke
    )
    drawArc(
        color = accent.copy(alpha = 0.18f),
        startAngle = phase * 360f + 180f,
        sweepAngle = 54f,
        useCenter = false,
        topLeft = Offset(center.x - outerR, center.y - outerR),
        size = Size(outerR * 2, outerR * 2),
        style = stroke
    )

    val innerR = radius * 0.73f
    drawArc(
        color = accent.copy(alpha = 0.22f),
        startAngle = -counterPhase * 360f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - innerR, center.y - innerR),
        size = Size(innerR * 2, innerR * 2),
        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Radar kolu: bir baş çizgisi + arkasında karesel sönümlenen 12 parçalı kuyruk.
 *
 * Kuyruk tek bir gradyan yerine ayrı yaylardan yapıldı; `sweepGradient` her karede
 * yeniden shader kuruyor ve dönen bir gradyanın kenarında görünür bir dikiş izi
 * bırakıyor.
 */
private fun DrawScope.drawRadarSweep(phase: Float, accent: Color, radius: Float) {
    val armRadius = radius * 0.80f
    val headAngle = phase * 360f - 90f
    val tailSegments = 12
    val segmentSpan = 7f

    for (i in tailSegments downTo 1) {
        val t = 1f - i.toFloat() / tailSegments
        drawArc(
            color = accent.copy(alpha = t * t * 0.40f),
            startAngle = headAngle - i * segmentSpan,
            sweepAngle = segmentSpan + 0.6f,   // dilimler arasında boşluk kalmasın
            useCenter = false,
            topLeft = Offset(center.x - armRadius, center.y - armRadius),
            size = Size(armRadius * 2, armRadius * 2),
            style = Stroke(width = 2.2.dp.toPx())
        )
    }

    // Kolun kendisi: merkezden dışa uzanan, uçta sönen çizgi.
    val rad = Math.toRadians(headAngle.toDouble())
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.55f)),
            start = center,
            end = Offset(
                center.x + (cos(rad) * armRadius).toFloat(),
                center.y + (sin(rad) * armRadius).toFloat()
            )
        ),
        start = center,
        end = Offset(
            center.x + (cos(rad) * armRadius).toFloat(),
            center.y + (sin(rad) * armRadius).toFloat()
        ),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )
}

/**
 * İkonun oturduğu sakin disk.
 *
 * Nefes (breath) yarıçapı yalnızca %1,5 oynatır. Daha fazlası ikonu titretiyor;
 * daha azı fark edilmiyor. Kenarlık vurgu rengine doğru karışır — dolgu değil,
 * yalnızca çizgi renklenir.
 */
private fun DrawScope.drawInnerDisc(
    breath: Float,
    fill: Color,
    outline: Color,
    accent: Color,
    radius: Float
) {
    val discRadius = radius * (0.615f + 0.015f * breath)
    drawCircle(color = fill, radius = discRadius, center = center)
    drawCircle(
        color = androidx.compose.ui.graphics.lerp(outline, accent, 0.35f + 0.25f * breath),
        radius = discRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}
