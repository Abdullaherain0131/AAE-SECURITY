package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografi ölçeği.
 *
 * Öncesinde yalnızca `bodyLarge` tanımlıydı; kalan 14 rol Material'ın kendi
 * varsayılanlarına düşüyordu. Ekranlar da bu yüzden her `Text` çağrısında
 * `fontSize`, `fontFamily` ve `fontWeight`'i elle yazıyordu — aynı "başlık"
 * ekranına göre 26 sp, 22 sp veya 13 sp olabiliyordu.
 *
 * ## İki aile
 *
 * - **Monospace** — sayısal ve teknik olan her şey: ölçüm, paket adı, sertifika
 *   özeti, günlük satırı, rozet. Uygulamanın karakteri buradan geliyor ve
 *   monospace'in asıl işlevsel faydası şu: rakamlar sabit genişlikte olduğu için
 *   canlı güncellenen bir sayaç (`%87` → `%88`) yanındaki metni oynatmaz.
 * - **Default (sans)** — okunması gereken düz metin: açıklama, uyarı gövdesi,
 *   onay diyaloğu. Uzun Türkçe cümleler monospace'te yorucu oluyor.
 */
private val Mono = FontFamily.Monospace
private val Sans = FontFamily.Default

/** Teknik/sayısal metinler için monospace ölçeği. */
object AaeText {
    /** Ekranın tepesindeki ana ölçüm (bütünlük skoru gibi). */
    val metricLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp
    )
    val metricMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    )
    val metricSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp
    )

    /** Bölüm başlığı — büyük harfle ve seyrek harf aralığıyla kullanılır. */
    val sectionLabel = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp
    )

    /** Durum rozeti: KORUNUYOR, TARANIYOR, KRİTİK. */
    val badge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.8.sp
    )

    /** Konsol/günlük satırı. */
    val console = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp
    )

    /** Paket adı, sertifika özeti gibi kırpılabilir teknik dizeler. */
    val technical = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.sp
    )
}

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.8.sp
    )
)
