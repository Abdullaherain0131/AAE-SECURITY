package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material'ın rol setine sığmayan anlamsal (semantic) renkler.
 *
 * M3'te "güvenli", "uyarı", "bilgi" diye roller yoktur; yalnızca `error` vardır.
 * Ekranlar bu üçünü kod içine gömülü hex değerleriyle yazıyordu — bu yüzden tema
 * değiştiğinde renkler değişmiyor, üstelik neon tonlar (`#06B6D4`, `#00E676`,
 * `#FF5252`) araya karışıyordu. Artık hepsi buradan gelir.
 *
 * `...Muted` karşılıkları geniş dolgular içindir: vurgu rengini büyük bir alana
 * yaymak, rengi ne kadar mat seçerseniz seçin ekranı ışıklandırır.
 */
@Immutable
data class AaeSemanticColors(
    val safe: Color,
    val safeMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val danger: Color,
    val dangerMuted: Color,
    val info: Color,
    val infoMuted: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    /** Zemin merdiveni — [Elevation] ile birlikte kullanılır. */
    val void: Color,
    val surfaceLow: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val border: Color,
    val borderStrong: Color,
    /** Arka plan alan animasyonlarının (bkz. AmbientField) çizgi rengi. */
    val ambientLine: Color
)

private val DarkSemanticColors = AaeSemanticColors(
    safe = StatusSafe,
    safeMuted = StatusSafeMuted,
    warning = StatusWarning,
    warningMuted = StatusWarningMuted,
    danger = StatusDanger,
    dangerMuted = StatusDangerMuted,
    info = AccentSteel,
    infoMuted = AccentSteelMuted,
    textTertiary = TextTertiary,
    textDisabled = TextDisabled,
    void = SlateVoid,
    surfaceLow = SlateSurfaceLow,
    surfaceHigh = SlateSurfaceHigh,
    surfaceHighest = SlateSurfaceHighest,
    border = SlateBorder,
    borderStrong = SlateBorderStrong,
    ambientLine = Color(0xFF93A4BC)
)

private val LightSemanticColors = AaeSemanticColors(
    safe = LightStatusSafe,
    safeMuted = LightStatusSafeMuted,
    warning = LightStatusWarning,
    warningMuted = LightStatusWarningMuted,
    danger = LightStatusDanger,
    dangerMuted = LightStatusDangerMuted,
    info = LightAccentSteel,
    infoMuted = LightAccentSteelMuted,
    textTertiary = LightTextTertiary,
    textDisabled = LightTextDisabled,
    void = LightBackground,
    surfaceLow = LightSurfaceLow,
    surfaceHigh = LightSurfaceHigh,
    surfaceHighest = LightSurfaceHighest,
    border = LightBorder,
    borderStrong = LightBorderStrong,
    ambientLine = Color(0xFF334155)
)

/**
 * Tema dışında da okunabilsin diye statik. Varsayılan koyu şema: uygulama koyu
 * doğar, aydınlık tema bir seçenektir.
 */
val LocalAaeColors = staticCompositionLocalOf { DarkSemanticColors }

/**
 * ## Rol eşlemesi hakkında
 *
 * Bu tema M3'ün alışılmış eşlemesini **bilerek** kullanmaz:
 * `primary` = ana metin rengi, `secondary` = ikincil metin rengi. Tüm ekranlar
 * (7 ekran, 7 bileşen) bu varsayıma göre yazıldığı için eşleme korunuyor.
 *
 * Bu sürümde eksik bırakılmış roller dolduruldu. Öncesinde `surfaceVariant`,
 * `onSurfaceVariant`, `outline`, `tertiary`, `surfaceContainer*` tanımlı değildi;
 * Material bunlar için kendi varsayılan mor/lila tonlarına düşüyordu. Örneğin
 * `CircularMetric` etiketleri `onSurfaceVariant` kullanıyor ve mat slate yerine
 * mor-gri çiziliyordu.
 *
 * `surfaceTint` kasıtlı olarak şeffaf: M3, yükseklik arttıkça yüzeyin üstüne bu
 * rengi bindirir ve parlatır. Mat görünüm istendiği için yükseklik farkını
 * bindirme yerine ayrık zemin katmanlarıyla ([Elevation]) veriyoruz.
 */
private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = SlateBackground,
    primaryContainer = SlateSurfaceHigh,
    onPrimaryContainer = TextPrimary,

    secondary = TextSecondary,
    onSecondary = SlateBackground,
    secondaryContainer = SlateSurfaceLow,
    onSecondaryContainer = TextSecondary,

    tertiary = AccentSteel,
    onTertiary = SlateVoid,
    tertiaryContainer = AccentSteelMuted,
    onTertiaryContainer = TextPrimary,

    background = SlateBackground,
    onBackground = TextPrimary,

    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceLow,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Color.Transparent,

    surfaceContainerLowest = SlateVoid,
    surfaceContainerLow = SlateBackground,
    surfaceContainer = SlateSurfaceLow,
    surfaceContainerHigh = SlateSurface,
    surfaceContainerHighest = SlateSurfaceHigh,
    surfaceBright = SlateSurfaceHighest,
    surfaceDim = SlateVoid,

    outline = SlateBorderStrong,
    outlineVariant = SlateBorder,

    error = StatusDanger,
    onError = TextPrimary,
    errorContainer = StatusDangerMuted,
    onErrorContainer = TextPrimary,

    inverseSurface = TextPrimary,
    inverseOnSurface = SlateBackground,
    inversePrimary = SlateSurface,
    scrim = SlateVoid
)

private val LightColorScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceHighest,
    onPrimaryContainer = LightTextPrimary,

    secondary = LightTextSecondary,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceHigh,
    onSecondaryContainer = LightTextSecondary,

    tertiary = LightAccentSteel,
    onTertiary = LightSurface,
    tertiaryContainer = LightAccentSteelMuted,
    onTertiaryContainer = LightTextPrimary,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = Color.Transparent,

    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightBackground,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    surfaceBright = LightSurface,
    surfaceDim = LightSurfaceHighest,

    outline = LightBorderStrong,
    outlineVariant = LightBorder,

    error = LightStatusDanger,
    onError = LightSurface,
    errorContainer = LightStatusDangerMuted,
    onErrorContainer = LightTextPrimary,

    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightSurface,
    inversePrimary = LightSurfaceHigh,
    scrim = LightTextPrimary
)

@Composable
fun AntivirusTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            // Durum ve gezinme çubuğu arka planla aynı tonda: çerçevesiz (edge-to-edge)
            // görünüm, ekranın tepesinde ayrı bir şerit oluşmasını engeller.
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAaeColors provides semanticColors,
        LocalMotionScale provides rememberSystemMotionScale()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AaeShapes,
            content = content
        )
    }
}

/** Kısayol: `MaterialTheme.colorScheme` ile aynı kullanım biçimi. */
val MaterialTheme.aae: AaeSemanticColors
    @Composable
    get() = LocalAaeColors.current
