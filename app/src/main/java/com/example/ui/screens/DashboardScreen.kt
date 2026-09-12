package com.example.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.Modifier
import androidx.compose.foundation.horizontalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.ThreatBottomSheet
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AaeSecurityEngine
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: com.example.ui.viewmodel.AntivirusViewModel,
    isScanning: Boolean,
    scanProgress: Float,
    currentAppName: String,
    totalAppsToScan: Int,
    scannedCount: Int,
    recentEventsText: String,
    aiStatusMessage: String?,
    healthScore: Int,
    activeThreats: List<com.example.data.entity.ThreatEntity>,
    onStartQuickScan: () -> Unit,
    onStartDeepScan: () -> Unit,
    onStartFullScan: () -> Unit,
    onResolveThreat: (Long) -> Unit,
    onWhitelistThreat: (Long) -> Unit,
    onQuarantineThreat: (Long) -> Unit,
    onUninstallApp: (String) -> Unit,
    onFreezeApp: (String) -> Unit = {},
    onForceStopApp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    val hiddenThreatIds = remember { mutableStateListOf<Long>() }
    val displayThreats = activeThreats.filter { !hiddenThreatIds.contains(it.id) }
    
    var glitchingThreatId by remember { mutableStateOf<Long?>(null) }
    var glitchOffsetX by remember { mutableStateOf(0f) }
    var glitchOffsetY by remember { mutableStateOf(0f) }
    var glitchAlpha by remember { mutableStateOf(1f) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("ProtectionPrefs", android.content.Context.MODE_PRIVATE)
    val isShizukuEnabled = prefs.getBoolean("key_shizuku_enabled", false)
    var showShizukuWipeDialog by remember { mutableStateOf<String?>(null) }
    var shizukuWipeLog by remember { mutableStateOf(listOf<String>()) }
    
    LaunchedEffect(showShizukuWipeDialog) {
        if (showShizukuWipeDialog != null) {
            val pkg = showShizukuWipeDialog!!
            shizukuWipeLog = listOf("> [SHIZUKU] ADB Protokolü Başlatılıyor...")
            kotlinx.coroutines.delay(600)
            shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] am force-stop $pkg"
            kotlinx.coroutines.delay(800)
            shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] pm clear $pkg"
            kotlinx.coroutines.delay(800)
            shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] appops set $pkg RUN_IN_BACKGROUND ignore"
            kotlinx.coroutines.delay(600)
            shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] pm uninstall $pkg"
            kotlinx.coroutines.delay(1000)
            showShizukuWipeDialog = null
            onUninstallApp(pkg)
        }
    }

    LaunchedEffect(pendingAction) {
        if (pendingAction != null) {
            for (i in 1..15) {
                glitchOffsetX = (-25..25).random().toFloat()
                glitchOffsetY = (-10..10).random().toFloat()
                glitchAlpha = (3..9).random() / 10f
                kotlinx.coroutines.delay((30..60).random().toLong())
            }
            glitchOffsetX = 0f; glitchOffsetY = 0f; glitchAlpha = 1f
            pendingAction?.invoke()
            pendingAction = null
            glitchingThreatId = null
        }
    }

    val vpnLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = android.content.Intent(context, com.example.service.DnsVpnService::class.java)
            context.startService(intent)
            android.widget.Toast.makeText(context, "VPN Kalkanı Aktif!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    var selectedThreat by remember { mutableStateOf<com.example.data.entity.ThreatEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

    var memUsage by remember { mutableIntStateOf(42) }
    var netTraffic by remember { mutableIntStateOf(120) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(isScanning) {
        while(true) {
            memUsage = (40..85).random()
            netTraffic = (10..300).random()
            delay(if (isScanning) 200 else 1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(trackColor)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -50 }) + androidx.compose.animation.fadeIn(tween(500)),
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AAE GÜVENLİK",
                            color = primaryColor,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .testTag("app_title")
                                .graphicsLayer {
                                    translationX = if (isScanning) (Math.random() * 4 - 2).toFloat() else 0f
                                }
                        )
                        Text(
                            text = "Geliştirici: Abdullah Asım Ersin",
                            color = primaryColor.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .background(if (isScanning) primaryColor.copy(alpha=0.15f) else secondaryColor.copy(alpha=0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .animateContentSize()
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Search else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isScanning) primaryColor else secondaryColor,
                            modifier = Modifier.size(18.dp).graphicsLayer {
                                rotationZ = if (isScanning) (scanProgress * 360f) else 0f
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScanning) "TARANIYOR" else "KORUNUYOR",
                            color = if (isScanning) primaryColor else secondaryColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // --- REAL-TIME SECURITY STATUS CARD ---
                val infiniteTransition2 = rememberInfiniteTransition()
                val pulseAlpha by infiniteTransition2.animateFloat(
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
                        .border(if (healthScore >= 70) 2.dp else 1.dp, if (healthScore < 70) errorColor.copy(alpha = pulseAlpha) else secondaryColor.copy(alpha = pulseAlpha), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AĞ VE SİSTEM KALKANI",
                            color = primaryColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (healthScore >= 70) "TÜM MODÜLLER DEVREDE - GÜVENLİ" else "DİKKAT: RİSKLİ DURUM TESPİT EDİLDİ",
                            color = if (healthScore >= 70) secondaryColor else errorColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    LiveWaveGraph(
                        isScanning = isScanning,
                        color = if (healthScore >= 70) secondaryColor else errorColor,
                        modifier = Modifier.size(60.dp, 30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                com.example.ui.components.ShizukuStatusCard(
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    surfaceColor = surfaceColor,
                    errorColor = errorColor
                )
                
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }) + androidx.compose.animation.fadeIn(tween(600, delayMillis = 200)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScanTypeButton(title = "HIZLI\nTARAMA", onClick = { if (!isScanning) onStartQuickScan() }, primaryColor = primaryColor, secondaryColor = secondaryColor, surfaceColor = surfaceColor)
                ScanTypeButton(title = "TAM\nTARAMA", onClick = { if (!isScanning) onStartFullScan() }, primaryColor = primaryColor, secondaryColor = secondaryColor, surfaceColor = surfaceColor)
                ScanTypeButton(title = "DERİN\nANALİZ", onClick = { if (!isScanning) onStartDeepScan() }, primaryColor = primaryColor, secondaryColor = secondaryColor, surfaceColor = surfaceColor)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.animation.AnimatedVisibility(
            visible = isVisible,
            enter = androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)) + androidx.compose.animation.fadeIn(tween(700)),
        ) {
            // --- 3D DÖNEN HOLOGRAFİK DÜNYA VE TEHDİT RADARI ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scanner_container"),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.components.CyberGlobeScanner(
                    isScanning = isScanning,
                    scanProgress = scanProgress,
                    currentAppName = currentAppName,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    surfaceColor = surfaceColor,
                    activeThreatsCount = displayThreats.size,
                    onClick = { if (!isScanning) onStartQuickScan() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
                Spacer(modifier = Modifier.height(16.dp))
                
                val animatedHealthScore by animateFloatAsState(
                    targetValue = healthScore / 100f,
                    animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
                    label = "healthScore"
                )
                
                val integrityColor = androidx.compose.ui.graphics.lerp(
                    errorColor,
                    secondaryColor,
                    animatedHealthScore
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CircularMetric(label = "Taranan\nDosya", value = if (isScanning) scannedCount else totalAppsToScan, isPercentage = false, progress = if(totalAppsToScan > 0) scannedCount.toFloat()/totalAppsToScan else 1f, color = secondaryColor, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularMetric(
                        label = "Aktif\nTehdit", 
                        value = displayThreats.size, 
                        isPercentage = false,
                        progress = if(displayThreats.isEmpty()) 0f else (displayThreats.size / 10f).coerceAtMost(1f), 
                        color = if(displayThreats.isEmpty()) secondaryColor else errorColor, 
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularMetric(
                        label = "Sistem\nBütünlüğü", 
                        value = healthScore, 
                        isPercentage = true,
                        progress = animatedHealthScore, 
                        color = integrityColor, 
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Tehdit Listesi
        if (displayThreats.isNotEmpty() && !isScanning) {
            Text("KRİTİK UYARILAR", color = errorColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            displayThreats.forEach { threat ->
                val isGlitching = glitchingThreatId == threat.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .graphicsLayer {
                            if (isGlitching) {
                                translationX = glitchOffsetX
                                translationY = glitchOffsetY
                                alpha = glitchAlpha
                                scaleX = if ((1..10).random() > 5) 1.05f else 0.95f
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, if(isGlitching) Color.Red else Color(0xFF06B6D4).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = errorColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(threat.appName, color = primaryColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(threat.detectedReasons, color = primaryColor.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { 
                                glitchingThreatId = threat.id
                                pendingAction = { 
                                    hiddenThreatIds.add(threat.id)
                                    onWhitelistThreat(threat.id) 
                                }
                            }) { Text("GÜVENİLİR", color = secondaryColor, fontFamily = FontFamily.Monospace) }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { 
                                glitchingThreatId = threat.id
                                pendingAction = { 
                                    hiddenThreatIds.add(threat.id)
                                    onQuarantineThreat(threat.id) 
                                }
                            }) { Text("KARANTİNA", color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { 
                                glitchingThreatId = threat.id
                                pendingAction = { 
                                    hiddenThreatIds.add(threat.id)
                                    if (isShizukuEnabled) {
                                        showShizukuWipeDialog = threat.packageName
                                    } else {
                                        onUninstallApp(threat.packageName) 
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = errorColor)) {
                                Text("İMHÂ ET", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
    
    if (showShizukuWipeDialog != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text("SHIZUKU DERİN İMHÂ PROTOKOLÜ", color = Color(0xFF10B981), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn {
                        items(shizukuWipeLog) { log ->
                            Text(log, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanTypeButton(title: String, onClick: () -> Unit, primaryColor: Color, secondaryColor: Color, surfaceColor: Color) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, if (isPressed) secondaryColor else primaryColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(100.dp).height(50.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            title.replace("\\n", "\n"), 
            color = primaryColor, 
            fontSize = 11.sp, 
            fontFamily = FontFamily.Monospace, 
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun LiveWaveGraph(isScanning: Boolean, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isScanning) 800 else 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = androidx.compose.ui.graphics.Path()

        val points = 50
        val step = width / points

        path.moveTo(0f, height / 2f)

        for (i in 0..points) {
            val x = i * step
            // Create a complex wave combining multiple sine waves for a "data signal" look
            val yOffset1 = kotlin.math.sin(i * 0.2f + phase) * (height / 3f)
            val yOffset2 = kotlin.math.sin(i * 0.5f - phase * 1.5f) * (height / 6f)
            val yOffset = if (isScanning) (yOffset1 + yOffset2) else (yOffset1 * 0.3f)
            
            path.lineTo(x, (height / 2f) + yOffset)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun AnimatedNumberText(
    value: Int,
    suffix: String = "",
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace
) {
    var animatedValue by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(value) {
        androidx.compose.animation.core.animate(
            initialValue = animatedValue.toFloat(),
            targetValue = value.toFloat(),
            animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
        ) { curr, _ ->
            animatedValue = curr.toInt()
        }
    }

    Text(
        text = "$animatedValue$suffix",
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight
    )
}

@Composable
fun CircularMetric(label: String, value: Int, isPercentage: Boolean = false, progress: Float, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
            CircularProgressIndicator(progress = { 1f }, color = color.copy(alpha = 0.1f), strokeWidth = 4.dp, modifier = Modifier.fillMaxSize())
            CircularProgressIndicator(progress = { progress }, color = color, strokeWidth = 4.dp, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxSize())
            AnimatedNumberText(value = value, suffix = if(isPercentage) "%" else "", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label.replace("\\n", "\n"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, lineHeight = 12.sp)
    }
}




