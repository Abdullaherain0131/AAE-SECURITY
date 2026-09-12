package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mat yükseklik (elevation) sistemi.
 *
 * Material'ın varsayılan yaklaşımı yüksekliği **gölge** ve **renk bindirmesi**
 * ile anlatır. İkisi de burada istenmiyor: gölge koyu zeminde görünmez, bindirme
 * ise yüzeyi parlatır — yani tam olarak kaçınılan neon etkisini üretir.
 *
 * Bunun yerine yükseklik üç mat ipucuyla anlatılır:
 *
 * 1. **Zemin katmanı** — her seviye slate merdiveninde bir basamak yukarı.
 * 2. **Saç teli kenarlık** — 1 dp, düşük alfa. Bileşenin sınırını gölgesiz çizer.
 * 3. **Üst kenar aydınlığı** — bileşenin yalnızca en üst pikselinde, %4-6 alfalı
 *    beyaz bir çizgi. Işığın yukarıdan geldiğini ima eder; ışıma yapmaz çünkü
 *    tek piksel yüksekliğinde ve yayılmıyor.
 */
@Immutable
enum class Elevation {
    /** Zeminin *altında*: girinti, ilerleme çubuğu yatağı, boş durum kutusu. */
    Sunken,

    /** Sayfanın kendi zemini. */
    Base,

    /** Standart kart / panel. */
    Raised,

    /** Öne çıkan kart, seçili satır, yüzen buton. */
    Floating,

    /** Diyalog, alt sayfa, açılır menü. */
    Overlay
}

/** Seviyenin zemin rengi. */
val Elevation.container: Color
    @Composable get() = when (this) {
        Elevation.Sunken -> MaterialTheme.aae.void
        Elevation.Base -> MaterialTheme.colorScheme.background
        Elevation.Raised -> MaterialTheme.colorScheme.surface
        Elevation.Floating -> MaterialTheme.aae.surfaceHigh
        Elevation.Overlay -> MaterialTheme.aae.surfaceHighest
    }

/** Seviyenin kenarlık rengi. Yükseldikçe kenarlık belirginleşir. */
val Elevation.outline: Color
    @Composable get() = when (this) {
        Elevation.Sunken -> MaterialTheme.aae.border.copy(alpha = 0.35f)
        Elevation.Base -> MaterialTheme.aae.border.copy(alpha = 0.40f)
        Elevation.Raised -> MaterialTheme.aae.border.copy(alpha = 0.55f)
        Elevation.Floating -> MaterialTheme.aae.border.copy(alpha = 0.75f)
        Elevation.Overlay -> MaterialTheme.aae.borderStrong.copy(alpha = 0.85f)
    }

/** Üst kenar aydınlığının şiddeti. */
private val Elevation.sheenAlpha: Float
    get() = when (this) {
        Elevation.Sunken -> 0f
        Elevation.Base -> 0f
        Elevation.Raised -> 0.045f
        Elevation.Floating -> 0.065f
        Elevation.Overlay -> 0.085f
    }

/**
 * Bir bileşene mat yüzey verir: zemin + saç teli kenarlık + üst kenar aydınlığı.
 *
 * @param accent verildiğinde kenarlık bu renge kayar (ör. tehdit kartı için
 *        [AaeSemanticColors.danger]). Dolgu değişmez — vurgu rengini geniş
 *        alana yaymak mat görünümü bozar.
 * @param accentStrength 0f..1f arası; kenarlığın vurgu rengine ne kadar
 *        kaydığını belirler. Canlandırılabilir bir değerdir (bkz. pulseBorder).
 */
fun Modifier.aaeSurface(
    elevation: Elevation = Elevation.Raised,
    shape: Shape? = null,
    accent: Color? = null,
    accentStrength: Float = 1f,
    borderWidth: Dp = 1.dp,
    clip: Boolean = true
): Modifier = composed {
    val resolvedShape = shape ?: MaterialTheme.shapes.medium
    val baseOutline = elevation.outline
    val outlineColor = if (accent != null) {
        androidx.compose.ui.graphics.lerp(baseOutline, accent, accentStrength.coerceIn(0f, 1f))
    } else {
        baseOutline
    }
    val sheen = elevation.sheenAlpha

    this
        .then(if (clip) Modifier.clip(resolvedShape) else Modifier)
        .background(elevation.container, resolvedShape)
        .then(
            if (sheen > 0f) {
                Modifier.drawWithCache {
                    // Yalnızca bileşenin üst dörtte birinde, yukarıdan aşağı sönen
                    // çok düşük alfalı beyaz. Yayılan bir hale değil, bir kenar payı.
                    val brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = sheen),
                            0.25f to Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = brush)
                    }
                }
            } else {
                Modifier
            }
        )
        .border(borderWidth, outlineColor, resolvedShape)
}

/**
 * İki kenarı aydınlatılmış ince ayraç. Koyu arayüzde `Divider` tek başına ya
 * görünmez ya da fazla sert durur; üstte koyu altta açık bir çift çizgi, ayrımı
 * kontrast yükseltmeden okunur kılar.
 */
fun Modifier.aaeDivider(): Modifier = composed {
    val line = MaterialTheme.aae.border
    drawWithCache {
        onDrawWithContent {
            drawContent()
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(0f, size.height - 1.5f),
                end = Offset(size.width, size.height - 1.5f),
                strokeWidth = 1f
            )
            drawLine(
                color = line.copy(alpha = 0.5f),
                start = Offset(0f, size.height - 0.5f),
                end = Offset(size.width, size.height - 0.5f),
                strokeWidth = 1f
            )
        }
    }
}
