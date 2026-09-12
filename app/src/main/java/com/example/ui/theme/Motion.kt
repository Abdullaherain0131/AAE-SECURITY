package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Hareket (motion) belirteçleri.
 *
 * Projedeki animasyonlar şu ana kadar her çağrı yerinde ayrı ayrı sayılarla
 * yazılmıştı: 150, 400, 500, 600, 700, 800, 1500, 1600, 2500, 3000, 4000 ms.
 * Sonuç, aynı ekranda birbiriyle ilgisiz hızlarda hareket eden bileşenlerdi.
 * Burada tek bir sözlük var ve her süre bir *amaca* bağlı.
 *
 * ## Ölçek
 *
 * | Belirteç | Süre | Ne için |
 * |---|---|---|
 * | [Instant] | 90 ms | Basma geri bildirimi — parmak kalkmadan bitmeli |
 * | [Quick] | 180 ms | Renk/alfa değişimi, rozet güncellemesi |
 * | [Standard] | 280 ms | Çoğu geçiş: açılma, kapanma, yerleşim |
 * | [Emphasized] | 420 ms | Ekran değişimi, öne çıkarılan giriş |
 * | [Slow] | 650 ms | Büyük yüzey, ilk açılış sahnelemesi |
 * | [Ambient] | 9000 ms | Arka plandaki sürekli, fark edilmeyen sürüklenme |
 *
 * Süreler yaklaşık 1.5 katlık adımlarla ilerler; iki komşu adım gözle ayırt
 * edilebilir ama uyumsuz görünmez.
 */
object Motion {

    const val Instant = 90
    const val Quick = 180
    const val Standard = 280
    const val Emphasized = 420
    const val Slow = 650
    const val Ambient = 9000

    /**
     * Kademeli giriş için öğe başına gecikme. 45 ms bilinçli olarak kısa:
     * 12 öğelik bir liste hâlâ yarım saniyede yerleşir.
     */
    const val StaggerStep = 45

    /** Kademeli girişte toplam bekleme süresini sınırla — uzun listede sıra beklenmez. */
    const val StaggerMaxSteps = 10

    // -------------------------------------------------------------------- easing

    /**
     * Varsayılan. Hızlı başlar, yavaşça oturur (M3 "standard").
     * Hemen hemen her şey bunu kullanır.
     */
    val Standard_Easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Ekrana *giren* öğeler: sert giriş, uzun yavaşlama. */
    val Enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Ekrandan *çıkan* öğeler: yavaş başla, hızlı kaybol. */
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Nefes alma / süzülme gibi başı sonu belli olmayan döngüler. */
    val Gentle: Easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

    /** Sabit hızlı dönüş (radar süpürmesi). Hızlanma bir motorda yapaylık yaratır. */
    val Linear: Easing = LinearEasing

    // -------------------------------------------------------------------- spring

    /** Basma/bırakma: geri sekme yok, hemen oturur. */
    fun <T> snappy(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    /** Yerleşim değişimi: yumuşak, hafif taşma. */
    fun <T> settle(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Dikkat çekmesi gereken tek seferlik hareket. */
    fun <T> expressive(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    // -------------------------------------------------------------------- hazır spec'ler

    fun <T> quick(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Quick, delayMillis, Standard_Easing)

    fun <T> standard(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Standard, delayMillis, Standard_Easing)

    fun <T> enter(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Emphasized, delayMillis, Enter)

    fun <T> exit(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Quick, delayMillis, Exit)

    // -------------------------------------------------------------------- döngüsel spec'ler

    /**
     * Sürekli döngü (Restart): faz 0f'ten 1f'e akar ve başa sarar. Radar
     * süpürmesi, dönen küre, kayan doku. Süre bir geçiş değil, *tam tur*
     * süresidir — bu yüzden ölçekten bağımsız, etkinin kendi ritmidir.
     */
    fun <T> loop(durationMillis: Int = Ambient): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(tween(durationMillis, easing = Linear), RepeatMode.Restart)

    /**
     * Sürekli salınım (Reverse): 0f ↔ 1f arasında gidip gelir — nefes alma,
     * durum nabzı, uyarı vurgusu. [loop]'tan farkı başa sarmamasıdır: göz
     * sıçrama anını görmez, hareket "yüzeyde" kalır.
     */
    fun <T> pulse(
        durationMillis: Int = Slow,
        easing: Easing = Gentle
    ): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(tween(durationMillis, easing = easing), RepeatMode.Reverse)
}

/**
 * Sistemin animasyon ölçeği (0f = kullanıcı animasyonları kapatmış).
 *
 * Geliştirici seçenekleri veya erişilebilirlik ayarlarından animasyonları
 * kapatan bir kullanıcı bunu bir tercih olarak yapar; sürekli dönen bir radar
 * halkası vestibüler rahatsızlık yaratabilir. Süreleri bu ölçekle çarpmak
 * yerine, [reduceMotion] true olduğunda bileşenler *döngüsel* animasyonlarını
 * tamamen bırakır ve son karesini çizer.
 */
val LocalMotionScale: ProvidableCompositionLocal<Float> = compositionLocalOf { 1f }

/** Kullanıcı hareketi azaltmayı tercih etmiş mi? */
val reduceMotion: Boolean
    @Composable get() = LocalMotionScale.current < 0.05f

/** Sistem ayarından animasyon ölçeğini okur. Okunamazsa 1f (animasyonlar açık). */
@Composable
fun rememberSystemMotionScale(): Float {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (e: Exception) {
            1f
        }
    }
}
