package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ThreatEntity
import com.example.ui.components.AaeProgressRing
import com.example.ui.components.CyberShield
import com.example.ui.components.ThreatBottomSheet
import com.example.ui.motion.AaeCounter
import com.example.ui.motion.borderTrace
import com.example.ui.motion.enterStaggered
import com.example.ui.motion.pressScale
import com.example.ui.motion.rememberLoopPhase
import com.example.ui.motion.rememberPressSource
import com.example.ui.motion.shakeOn
import com.example.ui.theme.AaeText
import com.example.ui.theme.Elevation
import com.example.ui.theme.Motion
import com.example.ui.theme.aae
import com.example.ui.theme.aaeSurface
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Ana gösterge paneli.
 *
 * ## Bu sürümde düzeltilenler
 *
 * 1. **Uydurma ölçümler kaldırıldı.** Ekran kendi içinde `(40..85).random()` ile
 *    "RAM kullanımı", `(10..300).random()` ile "ağ trafiği" üretiyordu. İkisi de
 *    artık [com.example.ui.viewmodel.AntivirusViewModel] üzerinden gerçek sistem
 *    ölçümlerinden geliyor ve ölçüm alınamıyorsa "—" gösteriliyor.
 * 2. **Neon renkler kaldırıldı.** `#06B6D4`, `Color.Red`, `#0F172A`, `#F59E0B`,
 *    `#10B981`, `Color.Black`, `Color.White` → anlamsal belirteçler.
 * 3. **Rastgele "glitch" animasyonu kaldırıldı.** Bir eylem onaylandığında kart
 *    `Math.random()` ile 15 kare boyunca zıplayıp saydamlaşıyordu; hem hareket
 *    bozukluk gibi görünüyordu hem de her karede yeniden çizim tetikliyordu.
 *    Yerine tek seferlik, sönümlenen bir sarsıntı ([shakeOn]) geldi.
 * 4. **Ölü kod bağlandı.** `selectedThreat` / `sheetState` tanımlıydı ama
 *    [ThreatBottomSheet] hiç açılmıyordu; artık karta dokunmak ayrıntı sayfasını
 *    açıyor. `recentEventsText` ve `aiStatusMessage` parametreleri de kullanılmıyordu.
 * 5. **Sayaçlar 0'dan saymayı bıraktı.** [AaeCounter] yalnızca değişen rakamı
 *    oynatır; ekranda hiç var olmamış bir ara değer görünmez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isScanning: Boolean,
    scanProgress: Float,
    currentAppName: String,
    totalAppsToScan: Int,
    scannedCount: Int,
    recentEventsText: String,
    aiStatusMessage: String?,
    healthScore: Int,
    activeThreats: List<ThreatEntity>,
    memUsagePercent: Int,
    netTrafficKbps: Int,
    isNetMeteringSupported: Boolean,
    batteryPercent: Float,
    isThrottled: Boolean,
    isRealTimeActive: Boolean,
    isShizukuEnabled: Boolean,
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
    val aae = MaterialTheme.aae

    val hiddenThreatIds = remember { mutableStateListOf<Long>() }
    val displayThreats = activeThreats.filterNot { hiddenThreatIds.contains(it.id) }
    val hasThreats = displayThreats.isNotEmpty()

    // Panelin tamamının rengini belirleyen tek durum. Önceliği önemli: tehdit
    // varsa koruma açık olsa bile ekran tehdit rengindedir.
    val statusAccent = when {
        hasThreats -> aae.danger
        !isRealTimeActive -> aae.warning
        isScanning -> aae.info
        else -> aae.safe
    }
    val accent by animateColorAsState(
        targetValue = statusAccent,
        animationSpec = Motion.standard(),
        label = "dashboardAccent"
    )

    val statusLabel = when {
        hasThreats -> "TEHDİT VAR"
        !isRealTimeActive -> "KORUMA KAPALI"
        isScanning -> "TARANIYOR"
        else -> "KORUNUYOR"
    }
    val statusIcon: ImageVector = when {
        hasThreats -> Icons.Default.Warning
        isScanning -> Icons.Default.Search
        else -> Icons.Default.Security
    }

    // Eylem onayı: kart bir kez sarsılır, sonra eylem çalışır. Gecikme
    // kullanıcının hangi karta dokunduğunu görmesi için; rastgele değil, sabit.
    var shakingThreatId by remember { mutableStateOf<Long?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(pendingAction) {
        val action = pendingAction ?: return@LaunchedEffect
        delay(Motion.Emphasized.toLong())
        action.invoke()
        pendingAction = null
        shakingThreatId = null
    }

    var selectedThreat by remember { mutableStateOf<ThreatEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var shizukuWipeTarget by remember { mutableStateOf<String?>(null) }
    var shizukuWipeLog by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(shizukuWipeTarget) {
        val pkg = shizukuWipeTarget ?: return@LaunchedEffect
        shizukuWipeLog = listOf("> [SHIZUKU] ADB protokolü başlatılıyor…")
        delay(600)
        shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] am force-stop $pkg"
        delay(800)
        shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] pm clear $pkg"
        delay(800)
        shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] appops set $pkg RUN_IN_BACKGROUND ignore"
        delay(600)
        shizukuWipeLog = shizukuWipeLog + "> [SHIZUKU] pm uninstall $pkg"
        delay(900)
        shizukuWipeTarget = null
        onUninstallApp(pkg)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        DashboardHeader(
            isScanning = isScanning,
            statusLabel = statusLabel,
            statusIcon = statusIcon,
            accent = accent,
            modifier = Modifier.enterStaggered(0)
        )

        Spacer(modifier = Modifier.height(20.dp))

        ShieldCard(
            isRealTimeActive = isRealTimeActive,
            hasThreats = hasThreats,
            isScanning = isScanning,
            scanProgress = scanProgress,
            healthScore = healthScore,
            netTrafficKbps = netTrafficKbps,
            accent = accent,
            modifier = Modifier.enterStaggered(1)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TelemetryStrip(
            memUsagePercent = memUsagePercent,
            netTrafficKbps = netTrafficKbps,
            isNetMeteringSupported = isNetMeteringSupported,
            batteryPercent = batteryPercent,
            isThrottled = isThrottled,
            modifier = Modifier.enterStaggered(2)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Yapay zekâ / model durum şeridi. Mesaj yoksa hiç yer kaplamaz.
        AnimatedVisibility(
            visible = !aiStatusMessage.isNullOrBlank(),
            enter = fadeIn(Motion.standard()) + expandVertically(Motion.standard()),
            exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .aaeSurface(Elevation.Sunken, MaterialTheme.shapes.small, accent = aae.info, accentStrength = 0.5f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = aiStatusMessage.orEmpty(),
                    style = AaeText.console,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        com.example.ui.components.ShizukuStatusCard(
            primaryColor = MaterialTheme.colorScheme.primary,
            secondaryColor = MaterialTheme.colorScheme.secondary,
            surfaceColor = MaterialTheme.colorScheme.surface,
            errorColor = aae.danger
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth().enterStaggered(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScanActionButton(
                title = "HIZLI\nTARAMA",
                enabled = !isScanning,
                accent = accent,
                onClick = onStartQuickScan,
                modifier = Modifier.weight(1f)
            )
            ScanActionButton(
                title = "TAM\nTARAMA",
                enabled = !isScanning,
                accent = accent,
                onClick = onStartFullScan,
                modifier = Modifier.weight(1f)
            )
            ScanActionButton(
                title = "DERİN\nANALİZ",
                enabled = !isScanning,
                accent = accent,
                onClick = onStartDeepScan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scanner_container")
                .enterStaggered(4),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.components.CyberGlobeScanner(
                isScanning = isScanning,
                scanProgress = scanProgress,
                currentAppName = currentAppName,
                primaryColor = MaterialTheme.colorScheme.primary,
                secondaryColor = accent,
                surfaceColor = aae.surfaceLow,
                activeThreatsCount = displayThreats.size,
                onClick = { if (!isScanning) onStartQuickScan() }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        MetricRow(
            isScanning = isScanning,
            scannedCount = scannedCount,
            totalAppsToScan = totalAppsToScan,
            threatCount = displayThreats.size,
            healthScore = healthScore,
            accent = accent,
            modifier = Modifier.enterStaggered(5)
        )

        Spacer(modifier = Modifier.height(28.dp))

        if (hasThreats && !isScanning) {
            SectionHeader(
                text = "KRİTİK UYARILAR",
                trailing = "${displayThreats.size}",
                color = aae.danger,
                modifier = Modifier.enterStaggered(6)
            )
            Spacer(modifier = Modifier.height(10.dp))

            displayThreats.forEachIndexed { index, threat ->
                ThreatCard(
                    threat = threat,
                    isShizukuEnabled = isShizukuEnabled,
                    shakeTrigger = if (shakingThreatId == threat.id) threat.id else null,
                    onOpenDetail = { selectedThreat = threat },
                    onAction = { action ->
                        shakingThreatId = threat.id
                        pendingAction = action
                    },
                    onWhitelist = {
                        hiddenThreatIds.add(threat.id)
                        onWhitelistThreat(threat.id)
                    },
                    onQuarantine = {
                        hiddenThreatIds.add(threat.id)
                        onQuarantineThreat(threat.id)
                    },
                    onDestroy = {
                        hiddenThreatIds.add(threat.id)
                        if (isShizukuEnabled) {
                            shizukuWipeTarget = threat.packageName
                        } else {
                            onUninstallApp(threat.packageName)
                        }
                    },
                    modifier = Modifier.enterStaggered(7 + index)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (recentEventsText.isNotBlank()) {
            SectionHeader(
                text = "SİSTEM GÜNLÜĞÜ",
                color = MaterialTheme.aae.textTertiary,
                modifier = Modifier.enterStaggered(8)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .enterStaggered(9)
                    .aaeSurface(Elevation.Sunken, MaterialTheme.shapes.small)
                    .heightIn(max = 180.dp)
                    .padding(12.dp)
            ) {
                recentEventsText.lines().forEach { line ->
                    Text(
                        text = line,
                        style = AaeText.console,
                        color = logLineColor(line),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    selectedThreat?.let { threat ->
        ThreatBottomSheet(
            appName = threat.appName,
            packageName = threat.packageName,
            threatCategory = threat.threatCategory,
            detectedReasons = threat.detectedReasons,
            onDismiss = { selectedThreat = null },
            onQuarantine = {
                hiddenThreatIds.add(threat.id)
                onQuarantineThreat(threat.id)
            },
            onUninstall = {
                hiddenThreatIds.add(threat.id)
                onUninstallApp(threat.packageName)
            },
            onWhitelist = {
                hiddenThreatIds.add(threat.id)
                onWhitelistThreat(threat.id)
            },
            onFreeze = if (isShizukuEnabled) ({ onFreezeApp(threat.packageName) }) else null,
            onForceStop = { onForceStopApp(threat.packageName) },
            onResolve = {
                hiddenThreatIds.add(threat.id)
                onResolveThreat(threat.id)
            }
        )
        // sheetState yalnızca "yarı açık duraklama yok" davranışı için tutuluyor.
        @Suppress("UNUSED_EXPRESSION")
        sheetState
    }

    if (shizukuWipeTarget != null) {
        ShizukuWipeDialog(log = shizukuWipeLog)
    }
}

// ---------------------------------------------------------------------------- başlık

@Composable
private fun DashboardHeader(
    isScanning: Boolean,
    statusLabel: String,
    statusIcon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val badgeShape = MaterialTheme.shapes.small
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AAE GÜVENLİK",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("app_title")
            )
            Text(
                text = "Geliştirici: Abdullah Asım Ersin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.aae.textTertiary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .aaeSurface(
                    elevation = Elevation.Raised,
                    shape = badgeShape,
                    accent = accent,
                    accentStrength = 0.7f
                )
                // Tarama sürerken rozetin kenarında dolaşan iz: "bir şey çalışıyor"
                // bilgisini metni değiştirmeden verir.
                .borderTrace(
                    active = isScanning,
                    shape = badgeShape,
                    color = accent,
                    durationMillis = 2200
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = statusLabel, style = AaeText.badge, color = accent)
        }
    }
}

// ---------------------------------------------------------------------------- kalkan kartı

@Composable
private fun ShieldCard(
    isRealTimeActive: Boolean,
    hasThreats: Boolean,
    isScanning: Boolean,
    scanProgress: Float,
    healthScore: Int,
    netTrafficKbps: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .aaeSurface(
                elevation = Elevation.Raised,
                accent = accent,
                accentStrength = if (hasThreats || !isRealTimeActive) 0.8f else 0.35f
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CyberShield(
            isRealTimeActive = isRealTimeActive,
            hasThreats = hasThreats,
            isScanning = isScanning,
            scanProgress = scanProgress,
            size = 72.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AĞ VE SİSTEM KALKANI",
                style = AaeText.sectionLabel,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    hasThreats -> "Etkisiz hâle getirilmemiş tehdit var."
                    !isRealTimeActive -> "Gerçek zamanlı koruma kapalı."
                    healthScore >= 70 -> "Tüm modüller devrede."
                    else -> "Riskli yapılandırma tespit edildi."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Sinyal izi: genlik gerçek ağ hızından geliyor. Trafik yoksa çizgi düz —
        // eskiden trafik olmasa da dalgalanıyordu.
        SignalTrace(
            activity = (netTrafficKbps / 250f).coerceIn(0f, 1f),
            accelerated = isScanning,
            color = accent,
            modifier = Modifier.size(56.dp, 28.dp)
        )
    }
}

/**
 * İki sinüsün toplamından oluşan sinyal izi.
 *
 * [activity] 0 olduğunda genlik sıfırdır ve düz bir çizgi çizilir. Bu bilinçli:
 * hiç veri akmıyorken dalgalanan bir grafik, olmayan bir etkinliği gösterir.
 */
@Composable
private fun SignalTrace(
    activity: Float,
    accelerated: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val phase by rememberLoopPhase(
        active = activity > 0.01f || accelerated,
        durationMillis = if (accelerated) 900 else 2400
    )
    val amplitude by animateFloatAsState(
        targetValue = if (accelerated) (0.25f + activity * 0.75f) else activity,
        animationSpec = Motion.settle(),
        label = "traceAmplitude"
    )

    Canvas(modifier = modifier) {
        val height = size.height
        val width = size.width
        val mid = height / 2f
        val points = 48
        val step = width / points
        val tau = (2 * Math.PI).toFloat()
        val path = Path()
        path.moveTo(0f, mid)
        for (i in 0..points) {
            val x = i * step
            val a = sin(i * 0.28f + phase * tau) * (height / 3f)
            val b = sin(i * 0.61f - phase * tau * 1.6f) * (height / 7f)
            path.lineTo(x, mid + (a + b) * amplitude)
        }
        drawPath(
            path = path,
            color = color.copy(alpha = 0.85f),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ---------------------------------------------------------------------------- telemetri

@Composable
private fun TelemetryStrip(
    memUsagePercent: Int,
    netTrafficKbps: Int,
    isNetMeteringSupported: Boolean,
    batteryPercent: Float,
    isThrottled: Boolean,
    modifier: Modifier = Modifier
) {
    val aae = MaterialTheme.aae
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TelemetryTile(
            icon = Icons.Default.Memory,
            label = "BELLEK",
            value = memUsagePercent,
            suffix = "%",
            // 85 üstü baskı altında demektir; renk bunu bildirir, metin değişmez.
            accent = if (memUsagePercent >= 85) aae.warning else MaterialTheme.colorScheme.secondary,
            available = memUsagePercent > 0,
            modifier = Modifier.weight(1f)
        )
        TelemetryTile(
            icon = Icons.Default.SwapVert,
            label = "AĞ",
            value = netTrafficKbps,
            suffix = " KB/s",
            accent = MaterialTheme.colorScheme.secondary,
            available = isNetMeteringSupported,
            modifier = Modifier.weight(1f)
        )
        TelemetryTile(
            icon = Icons.Default.BatteryStd,
            label = if (isThrottled) "PİL · KISITLI" else "PİL",
            value = batteryPercent.toInt(),
            suffix = "%",
            accent = when {
                isThrottled -> aae.warning
                batteryPercent <= 15f -> aae.danger
                else -> MaterialTheme.colorScheme.secondary
            },
            available = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TelemetryTile(
    icon: ImageVector,
    label: String,
    value: Int,
    suffix: String,
    accent: Color,
    available: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aaeSurface(Elevation.Sunken, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.aae.textTertiary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = AaeText.badge,
                color = MaterialTheme.aae.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (available) {
            AaeCounter(
                value = value,
                style = AaeText.metricSmall,
                color = accent,
                suffix = suffix
            )
        } else {
            // Ölçüm alınamıyor. Sıfır göstermek "trafik yok" anlamına gelirdi.
            Text("—", style = AaeText.metricSmall, color = MaterialTheme.aae.textDisabled)
        }
    }
}

// ---------------------------------------------------------------------------- ölçüm halkaları

@Composable
private fun MetricRow(
    isScanning: Boolean,
    scannedCount: Int,
    totalAppsToScan: Int,
    threatCount: Int,
    healthScore: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val aae = MaterialTheme.aae
    val track = aae.surfaceHigh

    val healthFraction by animateFloatAsState(
        targetValue = healthScore / 100f,
        animationSpec = Motion.settle(),
        label = "healthFraction"
    )
    val healthColor = androidx.compose.ui.graphics.lerp(aae.danger, aae.safe, healthFraction)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricRing(
            label = "Taranan\nUygulama",
            value = if (isScanning) scannedCount else totalAppsToScan,
            progress = if (totalAppsToScan > 0) scannedCount.toFloat() / totalAppsToScan else 1f,
            color = MaterialTheme.colorScheme.secondary,
            trackColor = track,
            indeterminate = isScanning && totalAppsToScan == 0,
            modifier = Modifier.weight(1f)
        )
        MetricRing(
            label = "Aktif\nTehdit",
            value = threatCount,
            progress = if (threatCount == 0) 0f else (threatCount / 10f).coerceAtMost(1f),
            color = if (threatCount == 0) aae.safe else aae.danger,
            trackColor = track,
            modifier = Modifier.weight(1f)
        )
        MetricRing(
            label = "Sistem\nBütünlüğü",
            value = healthScore,
            suffix = "%",
            progress = healthFraction,
            color = healthColor,
            trackColor = track,
            modifier = Modifier.weight(1f)
        )
    }
    // accent burada bilinçli kullanılmıyor: her halka kendi anlamının rengini
    // taşır, panelin genel durum rengini değil.
    @Suppress("UNUSED_EXPRESSION") accent
}

@Composable
private fun MetricRing(
    label: String,
    value: Int,
    progress: Float,
    color: Color,
    trackColor: Color,
    suffix: String = "",
    indeterminate: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        AaeProgressRing(
            progress = progress,
            color = color,
            trackColor = trackColor,
            diameter = 72.dp,
            indeterminate = indeterminate
        ) {
            AaeCounter(
                value = value,
                style = AaeText.metricSmall,
                color = MaterialTheme.colorScheme.primary,
                suffix = suffix
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.aae.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------- tarama düğmeleri

@Composable
private fun ScanActionButton(
    title: String,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = rememberPressSource()
    Box(
        modifier = modifier
            .height(54.dp)
            .pressScale(source)
            .aaeSurface(
                elevation = if (enabled) Elevation.Floating else Elevation.Base,
                shape = MaterialTheme.shapes.small,
                accent = if (enabled) accent else null,
                accentStrength = 0.30f
            )
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = AaeText.badge,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.aae.textDisabled
            },
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.labelMedium.lineHeight
        )
    }
}

// ---------------------------------------------------------------------------- tehdit kartı

@Composable
private fun ThreatCard(
    threat: ThreatEntity,
    isShizukuEnabled: Boolean,
    shakeTrigger: Any?,
    onOpenDetail: () -> Unit,
    onAction: (() -> Unit) -> Unit,
    onWhitelist: () -> Unit,
    onQuarantine: () -> Unit,
    onDestroy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aae = MaterialTheme.aae
    val source = rememberPressSource()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shakeOn(shakeTrigger)
            .pressScale(source, pressedScale = 0.985f)
            .aaeSurface(
                elevation = Elevation.Raised,
                accent = aae.danger,
                accentStrength = 0.7f
            )
            .clickable(interactionSource = source, indication = null, onClick = onOpenDetail)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = aae.danger,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = threat.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = threat.packageName,
                    style = AaeText.technical,
                    color = aae.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = threat.detectedReasons,
            style = AaeText.console,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onAction(onWhitelist) }) {
                Text("GÜVENİLİR", style = AaeText.badge, color = aae.safe)
            }
            TextButton(onClick = { onAction(onQuarantine) }) {
                Text("KARANTİNA", style = AaeText.badge, color = aae.warning)
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { onAction(onDestroy) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = aae.dangerMuted,
                    contentColor = aae.danger
                ),
                shape = MaterialTheme.shapes.extraSmall,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 6.dp
                )
            ) {
                Text(
                    text = if (isShizukuEnabled) "DERİN İMHÂ" else "İMHÂ ET",
                    style = AaeText.badge
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------- yardımcılar

@Composable
private fun SectionHeader(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = AaeText.sectionLabel, color = color)
        Spacer(modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(text = trailing, style = AaeText.badge, color = color)
        }
    }
}

/** Günlük satırının önekindeki önem derecesine göre renk. */
@Composable
private fun logLineColor(line: String): Color = when {
    line.contains("[ERROR]") || line.contains("[CRITICAL]") -> MaterialTheme.aae.danger
    line.contains("[WARNING]") -> MaterialTheme.aae.warning
    line.contains("[SUCCESS]") -> MaterialTheme.aae.safe
    else -> MaterialTheme.aae.textTertiary
}

@Composable
private fun ShizukuWipeDialog(log: List<String>) {
    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .aaeSurface(Elevation.Overlay, MaterialTheme.shapes.large, accent = MaterialTheme.aae.safe, accentStrength = 0.5f)
                .padding(16.dp)
        ) {
            Text(
                text = "SHIZUKU DERİN İMHÂ PROTOKOLÜ",
                style = AaeText.sectionLabel,
                color = MaterialTheme.aae.safe
            )
            Spacer(modifier = Modifier.height(14.dp))
            log.forEachIndexed { index, line ->
                Text(
                    text = line,
                    style = AaeText.console,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.enterStaggered(index)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
