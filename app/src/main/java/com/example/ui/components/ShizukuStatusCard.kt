package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ShizukuUtils
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

@Composable
fun ShizukuStatusCard(
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    errorColor: Color
) {
    var isShizukuRunning by remember { mutableStateOf(ShizukuUtils.isAvailable()) }
    var hasPermission by remember { mutableStateOf(ShizukuUtils.hasPermission()) }
    var shizukuVersion by remember { mutableIntStateOf(if (isShizukuRunning) Shizuku.getVersion() else 0) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentlyRunning = ShizukuUtils.isAvailable()
            val currentlyGranted = ShizukuUtils.hasPermission()
            if (isShizukuRunning != currentlyRunning) {
                isShizukuRunning = currentlyRunning
                if (currentlyRunning) {
                    shizukuVersion = Shizuku.getVersion()
                }
            }
            if (hasPermission != currentlyGranted) {
                hasPermission = currentlyGranted
            }
            delay(2000)
        }
    }

    val statusColor = if (isShizukuRunning && hasPermission) secondaryColor else if (isShizukuRunning) Color(0xFFFFD54F) else errorColor
    
    val infiniteTransition = rememberInfiniteTransition(label = "shizukuPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(surfaceColor, shape = RoundedCornerShape(8.dp))
            .border(1.dp, statusColor.copy(alpha = pulseAlpha), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SHIZUKU BAĞLANTI DURUMU",
                color = primaryColor,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            val statusText = if (isShizukuRunning && hasPermission) {
                "SİSTEM KÖK ERİŞİMİ AKTİF (v$shizukuVersion)"
            } else if (isShizukuRunning) {
                "SERVİS ÇALIŞIYOR - İZİN BEKLENİYOR (v$shizukuVersion)"
            } else {
                "BAĞLANTI YOK - SERVİS KAPALI"
            }
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
