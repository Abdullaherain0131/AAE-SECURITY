package com.example.ui
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.sp
import android.content.Context


import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import com.example.ui.screens.NetworkScreen
import androidx.compose.material.icons.filled.Build
import com.example.ui.screens.ToolsScreen
import androidx.compose.material.icons.automirrored.filled.List
import com.example.ui.screens.ReportsScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.AntivirusViewModel

@Composable
fun MainScreen(viewModel: AntivirusViewModel) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val currentAppName by viewModel.currentAppBeingScanned.collectAsStateWithLifecycle()
    val recentEvents by viewModel.recentEvents.collectAsStateWithLifecycle()
    val totalAppsToScan by viewModel.totalAppsToScan.collectAsStateWithLifecycle()
    val scannedCount by viewModel.scannedCount.collectAsStateWithLifecycle()
    var isGhostModeActive by remember { mutableStateOf(false) }
    var showPanicDialog by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    
    val isKvkkAccepted by viewModel.isKvkkAccepted.collectAsStateWithLifecycle()
    
    // Request notification permission for Android 13+
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

    
    // Settings States
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
        com.example.ui.screens.SplashScreen {
            showSplash = false
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition()
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 100f
            val strokeColor = Color(0xFFA0AAB4).copy(alpha = 0.08f)
            
            // Vertical lines
            var x = (gridOffset % step) - step
            while (x < size.width) {
                drawLine(strokeColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            
            // Horizontal lines
            var y = (gridOffset % step) - step
            while (y < size.height) {
                drawLine(strokeColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }, alwaysShowLabel = selectedTab == 0,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Ana Ekran") },
                    label = { Text("Ana Ekran", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background,
                        unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }, alwaysShowLabel = selectedTab == 1,
                    icon = { Icon(Icons.Default.Build, contentDescription = "Araçlar") },
                    label = { Text("Araçlar", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background,
                        unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }, alwaysShowLabel = selectedTab == 2,
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Raporlar") },
                    label = { Text("Raporlar", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background,
                        unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
                                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }, alwaysShowLabel = selectedTab == 3,
                    icon = { Icon(Icons.Default.Public, contentDescription = "Ağ İzleme") },
                    label = { Text("Ağ İzleme", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background,
                        unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 }, alwaysShowLabel = selectedTab == 4,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar") },
                    label = { Text("Ayarlar", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background,
                        unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }
    ) { paddingValues ->
        androidx.compose.animation.AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) + 
                 androidx.compose.animation.scaleIn(initialScale = 0.92f, animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.EaseOutBack)))
                .togetherWith(androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)))
            },
            label = "tab_transition",
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) { targetTab ->
            when (targetTab) {
                0 -> {
                    DashboardScreen(
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
                }
                1 -> {
                    ToolsScreen(viewModel = viewModel)
                }
                2 -> {
                    ReportsScreen(
                        scanHistory = scanHistory,
                        activeThreats = activeThreats,
                        onUninstallApp = { pkg -> viewModel.uninstallApp(context, pkg) },
                        onWhitelistThreat = { id -> viewModel.whitelistThreat(id) },
                        onQuarantineThreat = { id -> viewModel.quarantineThreat(id) }
                    )
                }
                3 -> {
                    NetworkScreen()
                }
                else -> {
                    SettingsScreen(
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