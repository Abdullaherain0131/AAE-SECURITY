package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.motion.rememberLoopPhase
import com.example.ui.theme.LocalAaeColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arka plandaki ortam (ambient) alanı.
 *
 * ## Neyin yerine geçti
 *
 * Öncesinde `MainScreen` tüm ekranı kaplayan, 3 saniyede bir başa saran sabit
 * aralıklı bir ızgara çiziyordu. İki sorunu vardı: ızgara her yerde aynı
 * yoğunlukta olduğu için içeriğin altında gürültü yapıyordu, ve 100 piksellik
 * sıçramalarla başa sardığı için hareket "kayma" değil "zıplama" olarak
 * görünüyordu.
 *
 * ## Yerine ne var
 *
 * Üç mat katman, hiçbirinde renk vurgusu yok:
 *
 * 1. **Derinlik havuzları** — çok geniş, çok düşük alfalı iki yumuşak daire.
 *    Elips yörüngelerde, birbirinden farklı hızlarda sürüklenirler; kesiştikleri
 *    yerde zemin bir tık açılır. Tek başına fark edilmez, yokluğu fark edilir.
 * 2. **Ufuk çizgileri** — aşağı doğru aralığı açılan yatay saç teli çizgiler.
 *    Aralık karesel arttığı için göz bunu perspektif olarak okur; ekranın bir
 *    derinliği varmış hissi verir.
 * 3. **Kenar karartması (vignette)** — köşelere doğru koyulaşma. İçeriği
 *    ortalamaya yardım eder ve OLED ekranda kenarlardaki bant etkisini gizler.
 *
 * ## Maliyet
 *
 * Kare başına 2 radyal gradyan + ~14 çizgi. Hareket azaltma açıksa ya da
 * [active] false ise faz sabitlenir ve alan tek bir durağan kare olarak çizilir —
 * animasyon tamamen durur, çizim yine de doğru görünür.
 */
@Composable
fun AmbientField(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    /** Çizgilerin ve havuzların temel opaklığı. Tarama sırasında yükseltilebilir. */
    intensity: Float = 1f
) {
    val colors = LocalAaeColors.current
    val phase by rememberLoopPhase(active = active, durationMillis = 24_000)
    val slowPhase by rememberLoopPhase(active = active, durationMillis = 41_000)

    val lineColor = colors.ambientLine
    val voidColor = colors.void

    Canvas(modifier = modifier.fillMaxSize()) {
        drawDepthPools(phase, slowPhase, lineColor, intensity)
        drawHorizonLines(phase, lineColor, intensity)
        drawVignette(voidColor)
    }
}

/**
 * İki geniş yumuşak havuz. Yarıçapları ekranın kısa kenarından büyük, bu yüzden
 * kenarları hiç görünmez — yalnızca merkezlerindeki hafif açılma algılanır.
 */
private fun DrawScope.drawDepthPools(
    phase: Float,
    slowPhase: Float,
    color: Color,
    intensity: Float
) {
    val tau = (2 * Math.PI).toFloat()
    val radius = size.minDimension * 0.95f

    val firstCenter = Offset(
        x = size.width * (0.30f + 0.18f * cos(phase * tau)),
        y = size.height * (0.22f + 0.14f * sin(phase * tau))
    )
    val secondCenter = Offset(
        // Ters yön ve farklı periyot: ikisi düzenli bir ritim kurmasın diye.
        x = size.width * (0.74f - 0.16f * cos(slowPhase * tau)),
        y = size.height * (0.70f + 0.12f * sin(slowPhase * tau * 1.3f))
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.055f * intensity), Color.Transparent),
            center = firstCenter,
            radius = radius
        ),
        radius = radius,
        center = firstCenter
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.040f * intensity), Color.Transparent),
            center = secondCenter,
            radius = radius * 0.85f
        ),
        radius = radius * 0.85f,
        center = secondCenter
    )
}

/**
 * Aşağı indikçe aralığı açılan yatay çizgiler; yukarı doğru sürüklenirler.
 *
 * `t` üzerinden karesel bir dağılım kullanılıyor: eşit aralıklı çizgiler düz bir
 * ızgara, karesel aralıklı çizgiler bir zemin düzlemi olarak okunur. Faz sürekli
 * olduğu ve çizgiler tepede zaten görünmez olduğu için başa sarma fark edilmez.
 */
private fun DrawScope.drawHorizonLines(
    phase: Float,
    color: Color,
    intensity: Float
) {
    val lineCount = 14
    for (i in 0 until lineCount) {
        // Sürekli kaydırma: i + faz, tam sayı sınırında sıçrama yaratmaz.
        val t = ((i + phase) % lineCount) / lineCount
        val y = size.height * (t * t)
        // Tepeye yakın çizgiler soluk (uzak), aşağıdakiler belirgin (yakın).
        val depthAlpha = (t * t * 0.10f + 0.012f) * intensity
        drawLine(
            color = color.copy(alpha = depthAlpha),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

/** Köşelere doğru karartma. */
private fun DrawScope.drawVignette(voidColor: Color) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, voidColor.copy(alpha = 0.55f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.72f
        )
    )
}
