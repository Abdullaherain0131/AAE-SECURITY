package com.example.ui.screens
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.filled.Security
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Android


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AiModelInfo

@Composable
fun SettingsScreen(
    isRealTimeActive: Boolean,
    onToggleRealTime: (Boolean) -> Unit,
    isAutoScanNewApps: Boolean,
    onToggleAutoScan: (Boolean) -> Unit,
    isHeuristicsEnabled: Boolean,
    onToggleHeuristics: (Boolean) -> Unit,
    isBackgroundAiLearning: Boolean,
    onToggleBackgroundAi: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    isShizukuEnabled: Boolean,
    onToggleShizuku: (Boolean) -> Unit,
    scanScheduleMode: Int,
    onScheduleModeChange: (Int) -> Unit,
    scanTargetHour: Int = 3,
    scanTargetMinute: Int = 0,
    onScanTargetTimeChange: (Int, Int) -> Unit = { _, _ -> },
    isEnergyEfficiencyModeEnabled: Boolean = false,
    onToggleEnergyEfficiencyMode: (Boolean) -> Unit = {},
    energyOptimizationScheduleMode: Int = 0,
    onEnergyScheduleModeChange: (Int) -> Unit = {},
    aiModels: List<AiModelInfo> = emptyList(),
    onUploadAiWeights: (String) -> Unit = {},
    onLoadModelUri: (android.net.Uri) -> Unit = {},
    isBatteryOptimizationIgnored: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        val context = LocalContext.current
        if (!isBatteryOptimizationIgnored) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF8B0000).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "UYARI: ARKA PLAN KORUMASI DURABİLİR",
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "İşletim sistemi pil tasarrufu için antivirüs servisini kapatıyor. Gerçek zamanlı korumanın sürekli çalışabilmesi için pil optimizasyonunu kapatmalısınız.",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { 
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Ayarlar açılamadı.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("PİL KISITLAMASINI KALDIR", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }


        
        Text(
            text = "SİSTEM AYARLARI",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(36.dp))


        SettingsToggleItem(
            title = "KARANLIK TEMA",
            description = "Siber güvenlik koyu temasını kullan",
            checked = isDarkTheme,
            onCheckedChange = onToggleDarkTheme
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        var showShizukuAnim by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        
        SettingsToggleItem(
            title = "SHIZUKU KÖK YETKİSİ",
            description = "Sistem uygulamalarını durdurmak için yetkilendir",
            checked = isShizukuEnabled,
            onCheckedChange = { checked ->
                if (checked) {
                    showShizukuAnim = true
                } else {
                    onToggleShizuku(false)
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (showShizukuAnim) {
            com.example.ui.components.ShizukuWizardDialog(
                onDismiss = { showShizukuAnim = false },
                onSuccess = {
                    showShizukuAnim = false
                    onToggleShizuku(true)
                }
            )
        }


        SettingsScheduleItem(
            title = "OTOMATİK DERİN TARAMA",
            description = "Arka planda düzenli tarama zamanlaması",
            currentMode = scanScheduleMode,
            onModeChange = onScheduleModeChange
        )
        if (scanScheduleMode > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            com.example.ui.components.TimeSelectorItem(
                title = "TARAMA SAATİ",
                description = "Taramanın başlayacağı zamanı seçin",
                hour = scanTargetHour,
                minute = scanTargetMinute,
                onTimeSelected = onScanTargetTimeChange
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsToggleItem(
            title = "ENERJİ TASARRUF MODU (BATTERY SAVER)",
            description = "RAM temizliği ve arka plan güvenlik taramalarını düşük güçte senkronize et.",
            checked = isEnergyEfficiencyModeEnabled,
            onCheckedChange = onToggleEnergyEfficiencyMode
        )
        if (isEnergyEfficiencyModeEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsScheduleItem(
                title = "OPTİMİZASYON TAKVİMİ",
                description = "Pil dostu RAM ve Tarama döngüsü (6 Saat / 24 Saat)",
                currentMode = energyOptimizationScheduleMode,
                labels = listOf("Kapalı", "6 Saat", "24 Saat"),
                onModeChange = onEnergyScheduleModeChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        val allShieldsActive = isRealTimeActive && isAutoScanNewApps && isHeuristicsEnabled && isBackgroundAiLearning
        val shieldScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (allShieldsActive) 1.02f else 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            ), label = "shieldScale"
        )
        val shieldGlow by androidx.compose.animation.animateColorAsState(
            targetValue = if (allShieldsActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
            animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow), label = "shieldGlow"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = shieldScale
                    scaleY = shieldScale
                }
                .border(
                    width = if (allShieldsActive) 2.dp else 1.dp,
                    color = if (allShieldsActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(shieldGlow)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Security,
                                contentDescription = "Güvenlik",
                                tint = if (allShieldsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "HIZLI ERİŞİM & KALKANLAR",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Switch(
                            checked = allShieldsActive,
                            onCheckedChange = { active ->
                                onToggleRealTime(active)
                                onToggleAutoScan(active)
                                onToggleHeuristics(active)
                                onToggleBackgroundAi(active)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.background,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShieldChip(
                            title = "Gerçek Zamanlı",
                            isActive = isRealTimeActive,
                            onClick = { onToggleRealTime(!isRealTimeActive) }
                        )
                        ShieldChip(
                            title = "Otonom Tarama",
                            isActive = isAutoScanNewApps,
                            onClick = { onToggleAutoScan(!isAutoScanNewApps) }
                        )
                        ShieldChip(
                            title = "Yapay Zeka",
                            isActive = isBackgroundAiLearning,
                            onClick = { onToggleBackgroundAi(!isBackgroundAiLearning) }
                        )
                        ShieldChip(
                            title = "Sezgisel Analiz",
                            isActive = isHeuristicsEnabled,
                            onClick = { onToggleHeuristics(!isHeuristicsEnabled) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "OTONOM YAPAY ZEKA ÇEKİRDEĞİ",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Büyük dosyalar yüklemenize gerek kalmadan, yapay zeka bulut destekli ufak modellerle arka planda sürekli kendini geliştirir.",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulut Öğrenim Durumu",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AKTİF",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { 1f },
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Son Senkronizasyon: Şimdi\nYeni tehdit vektörleri entegre ediliyor...",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "© 2026 Abdullah Asım Ersin",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tüm Hakları Saklıdır",
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Yeni Nesil Mobil Tehdit İstihbaratı Olarak Geliştirilmiştir",
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = MaterialTheme.colorScheme.background,
                uncheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}


@Composable
fun SettingsScheduleItem(
    title: String,
    description: String,
    currentMode: Int,
    labels: List<String> = listOf("Kapalı", "Günlük", "Haftalık"),
    onModeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleButton(labels.getOrElse(0) { "Kapalı" }, currentMode == 0) { onModeChange(0) }
            ScheduleButton(labels.getOrElse(1) { "1" }, currentMode == 1) { onModeChange(1) }
            ScheduleButton(labels.getOrElse(2) { "2" }, currentMode == 2) { onModeChange(2) }
        }
    }
}

@Composable
fun ScheduleButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.background,
            contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AiStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldChip(title: String, isActive: Boolean, onClick: () -> Unit) {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
        label = "chipScale"
    )

    FilterChip(
        selected = isActive,
        onClick = onClick,
        interactionSource = interactionSource,
        label = { Text(title, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isActive,
            borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
        ),
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    )
}
