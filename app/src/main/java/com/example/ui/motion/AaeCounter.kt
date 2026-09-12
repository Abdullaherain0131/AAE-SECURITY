package com.example.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.Motion
import com.example.ui.theme.reduceMotion

/**
 * Rakam rakam dönen sayaç (odometre).
 *
 * ## Neden bu, `animate(0f -> value)` yerine
 *
 * Projede sayılar `animate()` ile 0'dan hedefe *sayılarak* gösteriliyordu: 0, 7,
 * 23, 41, 58, 73 … Bu, iki yanlış izlenim verir. Birincisi, değer gerçekten o
 * ara değerlerden geçmiş gibi görünür — 3 aktif tehdit varken ekranda bir an
 * "1 tehdit" yazar. İkincisi, her ölçüm açılışta sıfırdan başladığı için
 * "hesaplanıyor" hissi verir; oysa değer zaten elimizdedir.
 *
 * Odometre yalnızca *değişen rakamı* oynatır. 42 → 43 geçişinde ekranda yalnızca
 * birler basamağı kayar; "4" yerinde durur. Böylece hareket, hangi büyüklüğün
 * değiştiğini de söyler.
 *
 * Rakamların yerinden oynamaması monospace yazı tipine bağlıdır ([AaeText]
 * ölçeğindeki tüm sayısal stiller monospace).
 */
@Composable
fun AaeCounter(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    prefix: String = "",
    suffix: String = "",
    /** Değer bu basamak sayısına kadar soldan sıfırla doldurulur (ör. `07`). */
    minDigits: Int = 1
) {
    // Yön karşılaştırması *tüm sayı* üzerinden yapılır, rakam bazında değil:
    // 19 → 20 geçişinde birler basamağı 9'dan 0'a düşer ama sayı artmıştır; rakam
    // bazında bakılsa o basamak aşağı kayar ve göz "azaldı" okur.
    val lastValue = remember { intArrayOf(value) }
    val goingUp = value >= lastValue[0]
    SideEffect { lastValue[0] = value }

    val digits = value.toString().let { raw ->
        val negative = raw.startsWith('-')
        val body = if (negative) raw.substring(1) else raw
        (if (negative) "-" else "") + body.padStart(minDigits, '0')
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = style, color = color)
        }

        if (reduceMotion) {
            Text(text = digits, style = style, color = color)
        } else {
            digits.forEachIndexed { index, char ->
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val enterOffset: (Int) -> Int = { height -> if (goingUp) height else -height }
                        val exitOffset: (Int) -> Int = { height -> if (goingUp) -height else height }
                        (
                            slideInVertically(
                                animationSpec = tween(Motion.Standard, easing = Motion.Enter),
                                initialOffsetY = enterOffset
                            ) + fadeIn(tween(Motion.Quick))
                            ).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(Motion.Standard, easing = Motion.Exit),
                                targetOffsetY = exitOffset
                            ) + fadeOut(tween(Motion.Quick))
                        // clip = false: kayan rakam kutunun dışına taşabilsin, yoksa
                        // basamak yarı yolda kesilir.
                        ).using(SizeTransform(clip = false))
                    },
                    label = "digit_$index"
                ) { target ->
                    Text(text = target.toString(), style = style, color = color)
                }
            }
        }

        if (suffix.isNotEmpty()) {
            Text(text = suffix, style = style, color = color)
        }
    }
}
