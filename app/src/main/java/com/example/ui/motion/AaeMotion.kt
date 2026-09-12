package com.example.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Motion
import com.example.ui.theme.reduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Yeniden kullanılabilir hareket ilkelleri.
 *
 * ## Tasarım kuralı: hareket bilgi taşır
 *
 * Buradaki hiçbir animasyon süs değil. Her biri bir soruya cevap verir:
 * *Bu öğe nereden geldi? • Dokunuşum algılandı mı? • Sistem şu anda çalışıyor mu? •
 * Bu sayı arttı mı azaldı mı?* Bir hareket bu sorulardan birine cevap vermiyorsa
 * kaldırılmalıdır — özellikle sürekli döngüde çalışan bir hareketse, çünkü her
 * kare GPU ve pil demektir.
 *
 * ## Erişilebilirlik
 *
 * Kullanıcı sistem animasyonlarını kapattıysa ([reduceMotion]) döngüsel
 * animasyonlar *hiç başlamaz* ve bileşen son karesini çizer. Süreyi kısaltmak
 * yetmez: vestibüler hassasiyeti olan biri için hızlı dönen bir halka, yavaş
 * dönenden daha rahatsız edicidir.
 */

// ---------------------------------------------------------------- basma geri bildirimi

/**
 * Basıldığında hafifçe küçülür, bırakıldığında yayla yerine oturur.
 *
 * Projede bu etki her düğmede `tween(150)` + `%85 ölçek` ile elle yazılıyordu.
 * %85 fazla agresif — düğme parmağın altından kaçıyor gibi duruyor. %96, dokunuşun
 * algılandığını bildirir ama yerleşimi bozmaz.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val reduce = reduceMotion

    LaunchedEffect(pressed, reduce) {
        if (reduce) {
            scale.snapTo(1f)
        } else {
            scale.animateTo(
                targetValue = if (pressed) pressedScale else 1f,
                animationSpec = Motion.snappy()
            )
        }
    }

    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** Kendi [MutableInteractionSource]'unu oluşturan kısa yol. */
@Composable
fun rememberPressSource(): MutableInteractionSource = remember { MutableInteractionSource() }

// ---------------------------------------------------------------- kademeli giriş

/**
 * Öğeleri sırayla sahneye alır: her biri bir öncekinden [Motion.StaggerStep] ms
 * sonra, aşağıdan yukarı süzülerek belirir.
 *
 * Hepsinin aynı anda belirmesi gözü tek bir noktaya bağlamaz; sıralı giriş okuma
 * yönünü kurar. Gecikme [Motion.StaggerMaxSteps] öğeden sonra sabitlenir, yoksa
 * 40 öğelik bir listede son satır 1.8 saniye bekler.
 */
fun Modifier.enterStaggered(
    index: Int = 0,
    visible: Boolean = true,
    offsetY: Dp = 18.dp
): Modifier = composed {
    val reduce = reduceMotion
    val progress = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { offsetY.toPx() }

    LaunchedEffect(visible, reduce) {
        if (reduce) {
            progress.snapTo(if (visible) 1f else 0f)
            return@LaunchedEffect
        }
        if (visible) {
            delay(index.coerceIn(0, Motion.StaggerMaxSteps) * Motion.StaggerStep.toLong())
            progress.animateTo(1f, tween(Motion.Emphasized, easing = Motion.Enter))
        } else {
            progress.snapTo(0f)
        }
    }

    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

// ---------------------------------------------------------------- nefes alma

/**
 * Çok yavaş, çok küçük bir ölçek/opaklık salınımı — "bu bileşen canlı" demenin
 * en sessiz yolu. Genlik bilerek küçük: %3'ten fazlası nabız gibi değil, titreme
 * gibi görünür.
 *
 * @param active false ise hareket durur ve bileşen 1f ölçekte kalır.
 */
fun Modifier.breathe(
    active: Boolean = true,
    amplitude: Float = 0.03f,
    durationMillis: Int = 3200
): Modifier = composed {
    if (!active || reduceMotion) return@composed this

    val transition = rememberInfiniteTransition(label = "breathe")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = Motion.Gentle),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_phase"
    )

    graphicsLayer {
        val scale = 1f + phase.value * amplitude
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Nefes alma değerini doğrudan okumak isteyen Canvas'lar için.
 * @return 0f..1f arası, [RepeatMode.Reverse] ile gidip gelen faz. Hareket
 *         azaltma açıksa sabit 0.5f (ortalama kare).
 */
@Composable
fun rememberBreathPhase(
    active: Boolean = true,
    durationMillis: Int = 3200
): State<Float> {
    if (!active || reduceMotion) {
        return remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }
    val transition = rememberInfiniteTransition(label = "breath_value")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = Motion.Gentle),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_value_phase"
    )
}

/**
 * Sürekli dönen bir faz (0f..1f, sıçramasız). Radar süpürmesi, yörünge, kayan
 * doku gibi döngüsel çizimler için.
 */
@Composable
fun rememberLoopPhase(
    active: Boolean = true,
    durationMillis: Int = Motion.Ambient
): State<Float> {
    if (!active || reduceMotion) {
        return remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }
    val transition = rememberInfiniteTransition(label = "loop")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loop_phase"
    )
}

// ---------------------------------------------------------------- kenar izi (border trace)

/**
 * Bileşenin çevresinde dolaşan kısa bir ışık izi — "bu panel şu anda çalışıyor".
 *
 * Neon kenarlıktan farkı: iz, kenarlığın *kendi rengidir*, yalnızca opaklığı
 * yüksektir ve arkasında sönen bir kuyruk bırakır. Çevreye yayılan bir hale yok;
 * çizgi 1.5 dp kalınlığında kalır. Kuyruk, sabit alfalı tek bir parça yerine
 * [tailSegments] alt parçaya bölünerek çiziliyor — böylece hareket yönü okunuyor.
 *
 * Yol ölçümü ([PathMeasure]) boyut değişmedikçe yeniden hesaplanmaz; her karede
 * yalnızca parça uçları güncellenir.
 */
fun Modifier.borderTrace(
    active: Boolean,
    shape: Shape,
    color: Color,
    strokeWidth: Dp = 1.5.dp,
    segmentFraction: Float = 0.16f,
    tailSegments: Int = 7,
    durationMillis: Int = 2800
): Modifier = composed {
    if (!active || reduceMotion) return@composed this

    val transition = rememberInfiniteTransition(label = "border_trace")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "trace_phase"
    )
    val density = LocalDensity.current

    drawWithCache {
        val outlinePath = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, density))
        }
        val measure = PathMeasure().apply { setPath(outlinePath, true) }
        val totalLength = measure.length
        val stroke = Stroke(
            width = with(density) { strokeWidth.toPx() },
            cap = StrokeCap.Round
        )
        // Parça yolları bir kez ayrılır, her karede içeriği yeniden doldurulur;
        // kare başına Path ayırmak çöp toplayıcıyı gereksiz yere çalıştırır.
        val scratch = List(tailSegments) { Path() }

        onDrawWithContent {
            drawContent()
            if (totalLength <= 0f) return@onDrawWithContent

            val headPosition = phase.value * totalLength
            val segmentLength = totalLength * segmentFraction
            val step = segmentLength / tailSegments

            for (i in 0 until tailSegments) {
                val path = scratch[i]
                path.reset()
                val end = headPosition - i * step
                val start = end - step
                appendWrappedSegment(measure, start, end, totalLength, path)
                // Baş parça en opak, kuyruğa doğru karesel olarak sönüyor:
                // doğrusal sönüm gözde hâlâ "çizgi", karesel sönüm "iz" okunuyor.
                val t = 1f - i / tailSegments.toFloat()
                drawPath(path = path, color = color.copy(alpha = t * t * 0.9f), style = stroke)
            }
        }
    }
}

/**
 * Yolun [start]..[end] aralığını, gerekirse başa sararak [out]'a ekler.
 * Kapalı bir çevrede baş parça sıfır noktasını geçtiğinde iz kopmasın diye.
 */
private fun appendWrappedSegment(
    measure: PathMeasure,
    start: Float,
    end: Float,
    totalLength: Float,
    out: Path
) {
    var from = start
    var to = end
    while (from < 0f) {
        from += totalLength
        to += totalLength
    }
    from %= totalLength
    to = from + (end - start)

    if (to <= totalLength) {
        measure.getSegment(from, to, out, true)
    } else {
        measure.getSegment(from, totalLength, out, true)
        val wrapped = Path()
        measure.getSegment(0f, to - totalLength, wrapped, true)
        out.addPath(wrapped)
    }
}

// ---------------------------------------------------------------- parıltı (shimmer)

/**
 * Yükleniyor durumundaki iskelet kutular için yüzeyden geçen mat parıltı.
 *
 * Tepe alfası bilerek 0.06: koyu zeminde bundan fazlası "ışık patlaması" olur.
 * Parıltı beyaz değil, bileşenin kendi ön plan renginin soluk hali — böylece
 * aydınlık temada da doğru yönde çalışır.
 */
fun Modifier.shimmer(
    active: Boolean = true,
    color: Color = Color.White,
    peakAlpha: Float = 0.06f,
    durationMillis: Int = 1400
): Modifier = composed {
    if (!active || reduceMotion) return@composed this

    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    drawWithCache {
        val bandWidth = size.width * 0.45f
        onDrawWithContent {
            drawContent()
            // -bandWidth..width+bandWidth arasında süpürerek kenarlarda sıçrama olmaz.
            val x = -bandWidth + phase.value * (size.width + 2 * bandWidth)
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.5f to color.copy(alpha = peakAlpha),
                        1f to Color.Transparent
                    ),
                    startX = x,
                    endX = x + bandWidth
                )
            )
        }
    }
}

// ---------------------------------------------------------------- eğim (tilt)

/**
 * Parmakla sürüklendiğinde kart 3B olarak eğilir, bırakılınca yayla düzelir.
 *
 * [graphicsLayer.cameraDistance] bilerek yüksek (8 * yoğunluk): düşük değerlerde
 * perspektif abartılı olur ve kart "kopuyor" gibi görünür. Eğim ±[maxTiltDegrees]
 * ile sınırlı, çünkü daha fazlası metni okunamaz hale getirir.
 */
fun Modifier.tiltOnDrag(
    enabled: Boolean = true,
    maxTiltDegrees: Float = 8f,
    travel: Dp = 120.dp
): Modifier = composed {
    if (!enabled || reduceMotion) return@composed this

    val scope = rememberCoroutineScope()
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    val travelPx = with(LocalDensity.current) { travel.toPx() }
    val density = LocalDensity.current.density

    this
        .pointerInput(enabled) {
            detectDragGestures(
                onDragEnd = {
                    scope.launch { tiltX.animateTo(0f, Motion.settle()) }
                    scope.launch { tiltY.animateTo(0f, Motion.settle()) }
                },
                onDragCancel = {
                    scope.launch { tiltX.animateTo(0f, Motion.settle()) }
                    scope.launch { tiltY.animateTo(0f, Motion.settle()) }
                }
            ) { change, dragAmount ->
                change.consume()
                scope.launch {
                    // Dikey sürükleme X ekseninde döndürür (kartın üstü geri yatar).
                    val nextX = (tiltX.value - dragAmount.y / travelPx * maxTiltDegrees)
                        .coerceIn(-maxTiltDegrees, maxTiltDegrees)
                    tiltX.snapTo(nextX)
                }
                scope.launch {
                    val nextY = (tiltY.value + dragAmount.x / travelPx * maxTiltDegrees)
                        .coerceIn(-maxTiltDegrees, maxTiltDegrees)
                    tiltY.snapTo(nextY)
                }
            }
        }
        .graphicsLayer {
            rotationX = tiltX.value
            rotationY = tiltY.value
            cameraDistance = 8f * density
            // Eğildikçe minik bir yükselme: derinlik hissini perspektif tek başına vermiyor.
            val tiltMagnitude = (abs(tiltX.value) + abs(tiltY.value)) / (2f * maxTiltDegrees)
            scaleX = 1f + tiltMagnitude * 0.02f
            scaleY = 1f + tiltMagnitude * 0.02f
        }
}

// ---------------------------------------------------------------- dikkat çekme

/**
 * Tek seferlik yatay sarsıntı — reddedilen işlem, geçersiz giriş, engellenen tehdit.
 *
 * Sönümlenen bir salınım (genlik her geçişte azalır); sabit genlikli sarsıntı
 * mekanik ve sinir bozucu durur.
 *
 * @param trigger değeri her değiştiğinde sarsıntı bir kez oynar.
 */
fun Modifier.shakeOn(
    trigger: Any?,
    amplitude: Dp = 7.dp,
    durationMillis: Int = 420
): Modifier = composed {
    val offset = remember { Animatable(0f) }
    val amplitudePx = with(LocalDensity.current) { amplitude.toPx() }
    val reduce = reduceMotion

    LaunchedEffect(trigger) {
        if (trigger == null || reduce) return@LaunchedEffect
        offset.snapTo(0f)
        offset.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearEasing)
        )
        offset.snapTo(0f)
    }

    graphicsLayer {
        val t = offset.value
        translationX = if (t == 0f || t == 1f) {
            0f
        } else {
            // 3 tam salınım, doğrusal sönüm.
            kotlin.math.sin(t * 3f * 2f * Math.PI).toFloat() * amplitudePx * (1f - t)
        }
    }
}

// ---------------------------------------------------------------- açılma (reveal)

/**
 * Dairesel açılma: içerik bir merkez noktadan dışa doğru görünür olur.
 * Tam ekran durum değişimlerinde (tarama bitti → sonuç) kullanılır.
 */
fun Modifier.circularReveal(
    progress: Float,
    center: Offset? = null
): Modifier = composed {
    graphicsLayer {
        clip = true
        this.shape = CircularRevealShape(progress.coerceIn(0f, 1f), center)
    }
}

private class CircularRevealShape(
    private val progress: Float,
    private val center: Offset?
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val origin = center ?: Offset(size.width / 2f, size.height / 2f)
        // Köşeye olan en uzak mesafe: yarıçap buna ulaştığında tüm alan açılmış olur.
        val maxRadius = listOf(
            Offset(0f, 0f), Offset(size.width, 0f),
            Offset(0f, size.height), Offset(size.width, size.height)
        ).maxOf { (it - origin).getDistance() }

        val radius = maxRadius * progress
        return androidx.compose.ui.graphics.Outline.Generic(
            Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = origin.x - radius,
                        top = origin.y - radius,
                        right = origin.x + radius,
                        bottom = origin.y + radius
                    )
                )
            }
        )
    }
}
