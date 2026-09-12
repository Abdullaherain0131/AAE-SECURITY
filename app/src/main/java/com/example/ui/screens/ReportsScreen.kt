package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ScanRecordEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    scanHistory: List<ScanRecordEntity>,
    activeThreats: List<com.example.data.entity.ThreatEntity>,
    onUninstallApp: (String) -> Unit,
    onWhitelistThreat: (Long) -> Unit,
    onQuarantineThreat: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface

    var selectedRecord by remember { mutableStateOf<ScanRecordEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }) + androidx.compose.animation.fadeIn(),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Active Threats", tint = if (activeThreats.isEmpty()) secondaryColor else errorColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AKTİF TEHDİTLER",
                    color = if (activeThreats.isEmpty()) primaryColor else errorColor,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        
        if (activeThreats.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(secondaryColor.copy(alpha=0.1f), RoundedCornerShape(8.dp)).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Harika! Sisteminiz temiz. Herhangi bir aktif tehdit bulunmuyor.", color = secondaryColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                itemsIndexed(activeThreats) { index, threat ->
                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 150L)
                        itemVisible = true
                    }
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { 100 }) + androidx.compose.animation.fadeIn()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, errorColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = errorColor.copy(alpha=0.05f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(threat.appName, color = errorColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(threat.packageName, color = primaryColor.copy(alpha=0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Box(modifier = Modifier.background(errorColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("KRİTİK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(threat.threatCategory, color = primaryColor.copy(alpha=0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = { onWhitelistThreat(threat.id) }, modifier = Modifier.weight(1f)) { 
                                        Text("GÜVENİLİR", color = primaryColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp) 
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(onClick = { onQuarantineThreat(threat.id) }, modifier = Modifier.weight(1f)) { 
                                        Text("KARANTİNA", color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace, fontSize = 10.sp) 
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onUninstallApp(threat.packageName) }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Kaldır", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SİSTEMDEN KALDIR", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = "History", tint = primaryColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GEÇMİŞ RAPORLAR",
                color = primaryColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Text("Raporu detaylı incelemek için dokunun.", color = secondaryColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))

        if (scanHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "HENÜZ TARAMA GEÇMİŞİ YOK",
                    color = primaryColor.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)
            ) {
                val reversedHistory = scanHistory.reversed()
                itemsIndexed(reversedHistory) { index, record ->
                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(index * 100L)
                        itemVisible = true
                    }
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }) + androidx.compose.animation.fadeIn()
                    ) {
                        ScanRecordItem(record = record, onDoubleTap = { selectedRecord = record })
                    }
                }
            }
        }
    }
    }

    if (selectedRecord != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedRecord = null },
            sheetState = sheetState,
            containerColor = surfaceColor
        ) {
            val record = selectedRecord!!
            val dateStr = remember(record.timestamp) {
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
            }
            val hasThreats = record.threatsFoundCount > 0
            val statusColor = if (hasThreats) errorColor else secondaryColor

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("RAPOR DETAYI: ${record.scanType.uppercase(Locale.getDefault())}", color = primaryColor, fontSize = 18.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(dateStr, color = primaryColor.copy(alpha=0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))
                
                val minExpectedSeconds = when {
                    record.scanType.contains("DERİN", ignoreCase = true) || record.scanType.contains("DEEP", ignoreCase = true) -> 22L
                    record.scanType.contains("TAM", ignoreCase = true) || record.scanType.contains("FULL", ignoreCase = true) -> 15L
                    else -> 6L
                }
                val durationSeconds = maxOf(minExpectedSeconds, (record.durationMs + 500) / 1000)
                DetailRow("Taranan Dosya/Uygulama:", "${record.scannedAppsCount}")
                DetailRow("Tarama Süresi:", "$durationSeconds saniye")
                DetailRow("Sonuç:", if (hasThreats) "${record.threatsFoundCount} TEHDİT" else "TEMİZ")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (hasThreats) {
                    Text("Risk Sebepleri & Çözüm Önerileri:", color = errorColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Rapor Detayı: Bu taramada tespit edilen tehditler sistem kayıtlarına eklendi.", color = errorColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom=4.dp))
                    Text("Lütfen tespit edilen tehditleri temizlemek için Ana Ekran veya Aktif Tehditler panelini kullanın.", color = errorColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom=4.dp))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("[ÇÖZÜM ADIMI]:", color = primaryColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Araçlar menüsünden 'Derin Temizlik' aracını kullanarak zararlı payloadları izole edebilir veya Uygulama Yöneticisinden riskli uygulamayı kaldırabilirsiniz.", color = primaryColor.copy(alpha=0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    Text("[ONAY]: Sisteminizde hiçbir güvenlik ihlali veya yetkisiz erişim saptanmamıştır.", color = secondaryColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { selectedRecord = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("KAPAT", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ScanRecordItem(record: ScanRecordEntity, onDoubleTap: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface

    val dateStr = remember(record.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    }
    
    val hasThreats = record.threatsFoundCount > 0
    val statusColor = if (hasThreats) errorColor else secondaryColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onDoubleTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.scanType.uppercase(Locale.getDefault()),
                    color = primaryColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    color = primaryColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Text(
                text = if (hasThreats) "${record.threatsFoundCount} TEHDİT" else "TEMİZ",
                color = statusColor,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
