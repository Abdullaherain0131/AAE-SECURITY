package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Motion
import com.example.ui.theme.reduceMotion
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var textIndex by remember { mutableIntStateOf(0) }
    val lines = listOf(
        "AAE Security [Neural-Core]",
        "> [OK] Donanım sensörleri & RAM doğrulandı.",
        "> [OK] Kriptografik motor başlatıldı.",
        "> [OK] Ağ arayüzleri & VPN kalkanı devrede.",
        "> [OK] Çekirdek (Kernel) bütünlüğü sağlandı.",
        "> KORUMA SİSTEMLERİ AKTİF..."
    )
    var displayedText by remember { mutableStateOf("") }
    var blink by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Hareket azaltma açıksa daktilo etkisi oynamaz: satırlar tek seferde
        // yazılır, yalnızca sabit kısa bir bekleme kalır.
        if (reduceMotion) {
            displayedText = lines.joinToString(separator = "\n") + "\n"
            delay(Motion.Slow.toLong())
            onFinish()
            return@LaunchedEffect
        }
        // Toplam geçiş süresini tam 1.0 saniye (1000ms) yapacak şekilde ayarlıyoruz.
        // 6 satır x 135ms = 810ms + 190ms bekleme = 1000ms
        for (i in lines.indices) {
            textIndex = i
            displayedText += lines[i] + "\n"
            delay(135)
        }
        delay(190)
        onFinish()
    }

    LaunchedEffect(Unit) {
        // İmleç yanıp sönmesi de döngüsel bir animasyondur; azaltan kullanıcıda
        // imleç sürekli görünür kalır (blink başlangıç değeri true).
        if (reduceMotion) return@LaunchedEffect
        while(true) {
            blink = !blink
            delay(180)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B0E))
            .padding(24.dp)
    ) {
        // Skip Button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0xFF00E676).copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .clickable { onFinish() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "ATLA >>",
                color = Color(0xFF00E676),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = displayedText + if (blink && textIndex < lines.size - 1) "█" else "",
                color = Color(0xFF00E676),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )
        }
    }
}
