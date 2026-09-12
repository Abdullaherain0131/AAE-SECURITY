package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AmbientField
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NetworkScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.Motion
import com.example.ui.theme.aae
import com.example.ui.viewmodel.AntivirusViewModel

/** Alt gezinme çubuğundaki bir sekme. */
private data class NavTab(
    val label: String,
    val icon: ImageVector
)

private val NavTabs = listOf(
    NavTab("Ana Ekran", Icons.Default.Home),
    NavTab("Araçlar", Icons.Default.Build),
    NavTab("Raporlar", Icons.AutoMirrored.Filled.List),
    NavTab("Ağ İzleme", Icons.Default.Public),
    NavTab("Ayarlar", Icons.Default.Settings)
)

@Composable
fun MainScreen(viewModel: AntivirusViewModel) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val currentAppName by viewModel.currentAppBeingScanned.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()
    val totalAppsToScan by viewModel.totalAppsToScan.collectAsStateWithLifecycle()
    val scannedCount by viewModel.scannedCount.collectAsStateWithLifecycle()

    val isKvkkAccepted by viewModel.isKvkkAccepted.collectAsStateWithLifecycle()

    // Android 13+ bildirim izni: gerçek zamanlı korumanın ön plan bildirimi ve
    // tehdit uyarıları bu izne bağlı.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val aiUploadStatusMessage by viewModel.aiUploadStatusMessage.collectAsStateWithLifecycle()
    val aiModels by viewModel.aiModels.collectAsStateWithLifecycle()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkBatteryOptimization()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val batteryPct by viewModel.batteryPercentage.collectAsStateWithLifecycle()
    val isThrottled by viewModel.isThrottled.collectAsStateWithLifecycle()
    val memUsage by viewModel.memUsage.collectAsStateWithLifecycle()
    val netTraffic by viewModel.netTraffic.collectAsStateWithLifecycle()
    val isNetMeteringSupported by viewModel.isNetMeteringSupported.collectAsStateWithLifecycle()

    // Ayar durumları
    val isRealTimeActive by viewModel.isRealTimeActive.collectAsStateWithLifecycle()
    val isAutoScanNewApps by viewModel.isAutoScanNewApps.collectAsStateWithLifecycle()
    val isHeuristicsEnabled by viewModel.isHeuristicsEnabled.collectAsStateWithLifecycle()
    val isBackgroundAiLearning by viewModel.isBackgroundAiLearning.collectAsStateWithLifecycle()

    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isShizukuEnabled by viewModel.isShizukuEnabled.collectAsStateWithLifecycle()
    val scanScheduleMode by viewModel.scanScheduleMode.collectAsStateWithLifecycle()
    val scanTargetHour by viewModel.scanTargetHour.collectAsStateWithLifecycle()
    val scanTargetMinute by viewModel.scanTargetMinute.collectAsStateWithLifecycle()

    val isEnergyEfficiencyModeEnabled by viewModel.isEnergyEfficiencyModeEnabled.collectAsStateWithLifecycle()
    val energyOptimizationScheduleMode by viewModel.energyOptimizationScheduleMode.collectAsStateWithLifecycle()
    val healthScore by viewModel.securityHealthScore.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val activeThreats by viewModel.activeThreats.collectAsStateWithLifecycle()

    val recentEventsText = recentEvents.take(5).joinToString("\n") {
        "> [${it.severity}] ${it.title}: ${it.description}"
    }

    if (!isKvkkAccepted) {
        com.example.ui.components.KvkkConsentDialog(
            onAccept = { enableBackgroundAi ->
                viewModel.acceptKvkk(enableBackgroundAi)
            }
        )
    }

    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        com.example.ui.screens.SplashScreen { showSplash = false }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val background = MaterialTheme.colorScheme.background
    val navContainer = MaterialTheme.aae.surfaceLow
    val navBorder = MaterialTheme.aae.border

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        // Arka plan alanı: tarama sırasında bir tık belirginleşir — hareketin
        // kendisi bir durum göstergesi, süs değil.
        AmbientField(intensity = if (isScanning) 1.6f else 1f)

        Scaffold(
            // Saydam: ortam alanının üstünde durur, kendi zeminini boyamaz.
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            bottomBar = {
                NavigationBar(
                    containerColor = navContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.drawBehind {
                        // Gölge yerine saç teli üst kenar: çubuğun içerikten
                        // ayrıldığı yeri parlatmadan bildirir.
                        drawLine(
                            color = navBorder.copy(alpha = 0.6f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }
                ) {
                    NavTabs.forEachIndexed { index, tab ->
                        val selected = selectedTab == index
                        // Seçili ikon hafifçe büyür ve yukarı kalkar. 1.12 ve 2 dp:
                        // fark edilir ama satır yüksekliğini değiştirmez.
                        val lift by animateFloatAsState(
                            targetValue = if (selected) 1f else 0f,
                            animationSpec = Motion.settle(),
                            label = "navLift$index"
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = index },
                            alwaysShowLabel = selected,
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = 1f + 0.12f * lift
                                        scaleY = 1f + 0.12f * lift
                                        translationY = -2.dp.toPx() * lift
                                    }
                                )
                            },
                            label = {
                                Text(tab.label, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.aae.surfaceHigh,
                                unselectedIconColor = MaterialTheme.aae.textTertiary,
                                unselectedTextColor = MaterialTheme.aae.textTertiary
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    // Yön farkındalığı: sağdaki sekmeye geçerken içerik sağdan
                    // gelir, soldakine geçerken soldan. Önceki sürüm her geçişte
                    // aynı "ölçeklenerek belirme" hareketini yapıyordu, bu yüzden
                    // sekmelerin uzamsal düzeni hissedilmiyordu.
                    val forward = targetState > initialState
                    val enterShift: (Int) -> Int = { full -> if (forward) full / 5 else -full / 5 }
                    val exitShift: (Int) -> Int = { full -> if (forward) -full / 8 else full / 8 }

                    (slideInHorizontally(Motion.enter(), enterShift) + fadeIn(Motion.enter()))
                        .togetherWith(
                            slideOutHorizontally(Motion.exit(), exitShift) + fadeOut(Motion.exit())
                        )
                        // clip = false: geçiş sırasında kaydırılan içerik kırpılmaz.
                        .using(SizeTransform(clip = false))
                },
                label = "tab_transition",
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        isScanning = isScanning,
                        scanProgress = scanProgress,
                        currentAppName = currentAppName,
                        totalAppsToScan = totalAppsToScan,
                        scannedCount = scannedCount,
                        recentEventsText = recentEventsText,
                        aiStatusMessage = aiUploadStatusMessage,
                        healthScore = healthScore,
                        activeThreats = activeThreats,
                        memUsagePercent = memUsage,
                        netTrafficKbps = netTraffic,
                        isNetMeteringSupported = isNetMeteringSupported,
                        batteryPercent = batteryPct,
                        isThrottled = isThrottled,
                        isRealTimeActive = isRealTimeActive,
                        onStartQuickScan = { viewModel.startScan(scanType = "QUICK") },
                        onStartDeepScan = { viewModel.startScan(scanType = "DEEP") },
                        onStartFullScan = { viewModel.startScan(scanType = "FULL") },
                        onResolveThreat = { id -> viewModel.resolveThreat(id) },
                        onWhitelistThreat = { id -> viewModel.whitelistThreat(id) },
                        onQuarantineThreat = { id -> viewModel.quarantineThreat(id) },
                        onUninstallApp = { pkg -> viewModel.uninstallApp(context, pkg) },
                        onFreezeApp = viewModel::freezeApp,
                        onForceStopApp = viewModel::forceStopApp
                    )

                    1 -> ToolsScreen(viewModel = viewModel)

                    2 -> ReportsScreen(
                        scanHistory = scanHistory,
                        activeThreats = activeThreats,
                        onUninstallApp = { pkg -> viewModel.uninstallApp(context, pkg) },
                        onWhitelistThreat = { id -> viewModel.whitelistThreat(id) },
                        onQuarantineThreat = { id -> viewModel.quarantineThreat(id) }
                    )

                    3 -> NetworkScreen()

                    else -> SettingsScreen(
                        isRealTimeActive = isRealTimeActive,
                        onToggleRealTime = viewModel::toggleRealTimeProtection,
                        isAutoScanNewApps = isAutoScanNewApps,
                        onToggleAutoScan = viewModel::toggleAutoScanNewApps,
                        isHeuristicsEnabled = isHeuristicsEnabled,
                        onToggleHeuristics = viewModel::toggleHeuristics,
                        isBackgroundAiLearning = isBackgroundAiLearning,
                        onToggleBackgroundAi = viewModel::toggleBackgroundAiLearning,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = viewModel::toggleDarkTheme,
                        isShizukuEnabled = isShizukuEnabled,
                        onToggleShizuku = viewModel::toggleShizuku,
                        scanScheduleMode = scanScheduleMode,
                        onScheduleModeChange = viewModel::setScanScheduleMode,
                        scanTargetHour = scanTargetHour,
                        scanTargetMinute = scanTargetMinute,
                        onScanTargetTimeChange = viewModel::setScanTargetTime,
                        isEnergyEfficiencyModeEnabled = isEnergyEfficiencyModeEnabled,
                        onToggleEnergyEfficiencyMode = viewModel::toggleEnergyEfficiencyMode,
                        energyOptimizationScheduleMode = energyOptimizationScheduleMode,
                        onEnergyScheduleModeChange = viewModel::setEnergyOptimizationScheduleMode,
                        aiModels = aiModels,
                        onUploadAiWeights = viewModel::importAiWeights,
                        onLoadModelUri = viewModel::loadModelFromUri,
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored
                    )
                }
            }
        }
    }
}
