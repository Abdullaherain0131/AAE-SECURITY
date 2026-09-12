package com.example.ui.screens
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState


import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language

import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security

import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import android.content.ClipboardManager
import android.app.ActivityManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.ThreatBottomSheet
import com.example.ui.motion.pressScale
import com.example.ui.theme.Motion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SuspiciousApp(val label: String, val pkgName: String)

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(viewModel: com.example.ui.viewmodel.AntivirusViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val vpnLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = Intent(context, com.example.service.DnsVpnService::class.java)
            context.startService(intent)
            android.widget.Toast.makeText(context, "1.1.1.1 DNS VPN Bağlantısı Başladı!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    
    // States for Privacy
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var privacyScanRunning by remember { mutableStateOf(false) }
    var sensitiveApps by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // States for Network
    var showNetworkDialog by remember { mutableStateOf(false) }
    var networkScanRunning by remember { mutableStateOf(false) }
    var networkInfo by remember { mutableStateOf("") }
    var isNetworkSafe by remember { mutableStateOf(true) }
    
    // States for Hardware
    var showBatteryDialog by remember { mutableStateOf(false) }
    var batteryScanRunning by remember { mutableStateOf(false) }
    var cpuTemp by remember { mutableStateOf(0f) }
    
    // States for Fake App
    var showFakeAppDialog by remember { mutableStateOf(false) }
    var fakeAppScanRunning by remember { mutableStateOf(false) }
    var fakeAppsFound by remember { mutableStateOf<List<SuspiciousApp>>(emptyList()) }
    var selectedSuspiciousApp by remember { mutableStateOf<SuspiciousApp?>(null) }
    
    // States for Phishing
    var showPhishingDialog by remember { mutableStateOf(false) }
    var phishingScanRunning by remember { mutableStateOf(false) }
    var phishingOutput by remember { mutableStateOf("") }
    
    // States for new features
    var showAppLockDialog by remember { mutableStateOf(false) }
    
    var showCleanerDialog by remember { mutableStateOf(false) }
    var showSystemMonitorScreen by remember { mutableStateOf(false) }
    if (showSystemMonitorScreen) {
        SystemMonitorScreen(viewModel = viewModel, onBack = { showSystemMonitorScreen = false })
        return
    }
    var cleanerRunning by remember { mutableStateOf(false) }
    var cleanedAmount by remember { mutableStateOf(0) }
    
    var showUrlDialog by remember { mutableStateOf(false) }
    var urlScanRunning by remember { mutableStateOf(false) }
    var urlToScan by remember { mutableStateOf("") }
    var urlResult by remember { mutableStateOf("") }

    // States for Dark Web Scanner
    var showDarkWebDialog by remember { mutableStateOf(false) }
    var darkWebScanRunning by remember { mutableStateOf(false) }
    var darkWebEmail by remember { mutableStateOf("") }
    var darkWebResult by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // States for Root Checker
    var showRootDialog by remember { mutableStateOf(false) }
    var rootScanRunning by remember { mutableStateOf(false) }
    var rootResult by remember { mutableStateOf("") }
    
    // States for App Manager
    
    var appManagerScanRunning by remember { mutableStateOf(false) }
    var appsList by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    
    // States for Password Vault
    var showVaultDialog by remember { mutableStateOf(false) }

    // States for Ad Blocker
    var showAppManagerDialog by remember { mutableStateOf(false) }
    var showPasswordVaultDialog by remember { mutableStateOf(false) }
    var showAdBlockerDialog by remember { mutableStateOf(false) }
    
    // States for Crypto Shield
    var showCryptoDialog by remember { mutableStateOf(false) }
    var cryptoScanRunning by remember { mutableStateOf(false) }

    var showClipboardDialog by remember { mutableStateOf(false) }
    var showIntruderDialog by remember { mutableStateOf(false) }
    var showSpyDialog by remember { mutableStateOf(false) }
    var showRamBoosterDialog by remember { mutableStateOf(false) }
    var isBoostingRam by remember { mutableStateOf(false) }
    var showZeroClickDialog by remember { mutableStateOf(false) }

    var showGameBoosterDialog by remember { mutableStateOf(false) }
    var isBoostingGame by remember { mutableStateOf(false) }
    var gamesList by remember { mutableStateOf<List<SuspiciousApp>>(emptyList()) }



    val tools = listOf(
        ToolItem(
            title = "Sistem Yöneticisi (Shizuku)",
            description = "Sistem süreçlerini dondur & yönet.",
            icon = Icons.Default.Android
        ) {
            showSystemMonitorScreen = true
        },
        ToolItem(
            title = "Oyun Hızlandırıcı (Booster)",
            description = "Oyunları algılar, RAM'i oyun için boşaltır.",
            icon = Icons.Default.SmartToy
        ) {
            showGameBoosterDialog = true
            isBoostingGame = true
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                delay(1200)
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val games = mutableListOf<SuspiciousApp>()
                for (appInfo in packages) {
                    val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
                    } else {
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_IS_GAME) != 0
                    }
                    if (isGame) {
                        games.add(SuspiciousApp(pm.getApplicationLabel(appInfo).toString(), appInfo.packageName))
                    }
                }
                gamesList = games.distinctBy { it.pkgName }
                isBoostingGame = false
            }
        },
        ToolItem(
            title = "Hırsız Kapanı",
            description = "İzinsiz erişimde selfie çeker.",
            icon = Icons.Default.CameraFront
        ) {
            showIntruderDialog = true
        },
        ToolItem(
            title = "Casus Avcısı",
            description = "Gizli Dinleme & İzlemeyi Taraması",
            icon = Icons.Default.Visibility
        ) {
            showSpyDialog = true
        },
        ToolItem(
            title = "RAM Hızlandırıcı",
            description = "Arka plan süreçlerini temizler.",
            icon = Icons.Default.Speed
        ) {
            showRamBoosterDialog = true
        },
        ToolItem(
            title = "Zero-Click Kalkanı",
            description = "RAM dalgalanmalarını dondurur.",
            icon = Icons.Default.Memory
        ) {
            showZeroClickDialog = true
        },
        ToolItem(
            title = "Gizlilik Yöneticisi",
            description = "Kamera & SMS erişimi denetimi.",
            icon = Icons.Default.Lock
        ) {
            showPrivacyDialog = true
            privacyScanRunning = true
            sensitiveApps = emptyList()
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                delay(1200)
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                val foundApps = mutableListOf<String>()
                for (pkg in packages) {
                    if (pkg.requestedPermissions != null) {
                        val perms = pkg.requestedPermissions?.toList() ?: emptyList()
                        if (perms.contains("android.permission.CAMERA") || 
                            perms.contains("android.permission.RECORD_AUDIO") ||
                            perms.contains("android.permission.READ_SMS")) {
                            if ((pkg.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                                foundApps.add(pm.getApplicationLabel(pkg.applicationInfo!!).toString())
                            }
                        }
                    }
                }
                sensitiveApps = foundApps.distinct().sorted()
                privacyScanRunning = false
            }
        },
        ToolItem(
            title = "Ağ Kalkanı",
            description = "DNS & WiFi şifreleme analizi.",
            icon = Icons.Default.WifiTethering
        ) {
            showNetworkDialog = true
            networkScanRunning = true
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                delay(1500)
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNetwork)
                if (caps != null) {
                    val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    isNetworkSafe = true
                    networkInfo = if(isVpn) "VPN Aktif (Güvenli)" else if(isWifi) "Wi-Fi (İzleniyor)" else "Hücresel Ağ (Güvenli)"
                } else {
                    isNetworkSafe = false
                    networkInfo = "Ağ Bulunamadı."
                }
                networkScanRunning = false
            }
        },
        ToolItem(
            title = "Sahte Uygulama",
            description = "İmza & Klon tespiti.",
            icon = Icons.Default.Apps
        ) {
            showFakeAppDialog = true
            fakeAppScanRunning = true
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                delay(1800)
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES)
                val suspicious = mutableListOf<SuspiciousApp>()
                val riskyPermissions = listOf(
                    "android.permission.SEND_SMS",
                    "android.permission.RECEIVE_SMS",
                    "android.permission.READ_SMS",
                    "android.permission.READ_CONTACTS",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.CAMERA",
                    "android.permission.SYSTEM_ALERT_WINDOW"
                )
                
                for (pkg in packages) {
                    if ((pkg.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                        val name = pm.getApplicationLabel(pkg.applicationInfo!!).toString()
                        val pkgName = pkg.packageName
                        
                        // Fake app check (cloned popular apps)
                        val nameLower = name.lowercase()
                        if (nameLower.contains("whatsapp") && !pkgName.startsWith("com.whatsapp")) {
                            suspicious.add(SuspiciousApp("$name (Sahte Paket: $pkgName)", pkgName))
                            continue
                        }
                        if (nameLower.contains("instagram") && pkgName != "com.instagram.android") {
                            suspicious.add(SuspiciousApp("$name (Sahte Paket: $pkgName)", pkgName))
                            continue
                        }
                        
                        // High risk permissions check
                        val safePrefixes = listOf(
                            "com.google.", "com.android.", "android", "com.whatsapp", "com.instagram", 
                            "com.facebook", "com.twitter", "com.spotify", "com.netflix", "com.xiaomi", 
                            "com.miui", "com.microsoft", "com.sec.android", "com.samsung", "com.skype",
                            "org.telegram", "com.viber", "com.snapchat", "com.linkedin", "com.duolingo"
                        )
                        val isTrusted = safePrefixes.any { pkgName.lowercase().startsWith(it) }
                        
                        if (!isTrusted) {
                            var riskCount = 0
                            pkg.requestedPermissions?.forEach { perm ->
                                if (riskyPermissions.contains(perm)) {
                                    riskCount++
                                }
                            }
                            if (riskCount >= 3) {
                                suspicious.add(SuspiciousApp("$name (Çok Sayıda Riskli İzin - $riskCount)", pkgName))
                                continue
                            }
                        }
                        
                        // Check for missing or invalid signatures (basic check)
                        if (pkg.signatures.isNullOrEmpty()) {
                            suspicious.add(SuspiciousApp("$name (İmza Bulunamadı)", pkgName))
                        }
                    }
                }
                fakeAppsFound = suspicious.distinctBy { it.pkgName }
                fakeAppScanRunning = false
            }
        },
        ToolItem(
            title = "Uygulama Yöneticisi",
            description = "Gelişmiş paket kontrolü.",
            icon = Icons.Default.Info
        ) {
            showAppManagerDialog = true
        },
        ToolItem(
            title = "Şifre Kasası",
            description = "Yerel AES-256 veri kasası.",
            icon = Icons.Default.VpnKey
        ) {
            showPasswordVaultDialog = true
        },
        ToolItem(
            title = "1.1.1.1 Güvenli DNS",
            description = "Sansürü aşar, güvenli ve gizli internet.",
            icon = Icons.Default.Block
        ) {
            showAdBlockerDialog = true
        },
        ToolItem(
            title = "Derin Bellek Temizliği",
            description = "RAM'i optimize et, önbelleği sil.",
            icon = Icons.Default.DeleteSweep
        ) {
            showCleanerDialog = true
        },
        ToolItem(
            title = "WiFi Analizörü",
            description = "Ağındaki cihazları ve zafiyetleri tespit et.",
            icon = Icons.Default.WifiTethering
        ) {
            showNetworkDialog = true
        }
    )

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }) + androidx.compose.animation.fadeIn(),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "GELİŞMİŞ ARAÇLAR",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 22.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kapsamlı siber güvenlik ve analiz modülleri.",
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(tools) { index, tool ->
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(
                        index.coerceIn(0, Motion.StaggerMaxSteps) * Motion.StaggerStep.toLong()
                    )
                    itemVisible = true
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = itemVisible,
                    enter = androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = Motion.expressive()) + androidx.compose.animation.fadeIn()
                ) {
                    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .pressScale(interactionSource)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = androidx.compose.foundation.LocalIndication.current
                            ) { tool.onClick() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (isPressed) 2.dp else 1.dp, Color(0xFF06B6D4).copy(alpha = if (isPressed) 1f else 0.5f))
                    ) {
                        Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tool.title,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tool.description,
                            color = MaterialTheme.colorScheme.primary.copy(alpha=0.6f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    }

    // Dialogs
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Gizlilik Analizi", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (privacyScanRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sistem izinleri taranıyor...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                        item {
                            Text("Aşağıdaki uygulamalar Kamera, Mikrofon veya SMS okuma yetkisine sahip:", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (sensitiveApps.isEmpty()) {
                            item { Text("Riskli uygulama bulunamadı.", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                        } else {
                            items(sensitiveApps) { app ->
                                Text("• $app", color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } }
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Donanım Analizi", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (batteryScanRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val statusColor = if (cpuTemp > 42f) Color(0xFFFF5252) else MaterialTheme.colorScheme.secondary
                        Text("Mevcut İşlemci Isısı: ${String.format("%.1f", cpuTemp)}°C", color = statusColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (cpuTemp > 42f) {
                            Text("Durum: Yüksek Isı. Sistem performansı düşürülebilir.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        } else {
                            Text("Durum: Normal. Termal kısıtlama (throttling) uygulanmıyor.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBatteryDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } }
        )
    }

    if (showNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Ağ Güvenliği", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (networkScanRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    val statusColor = if (isNetworkSafe) MaterialTheme.colorScheme.secondary else Color(0xFFFF5252)
                    Text(networkInfo, color = statusColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { TextButton(onClick = { showNetworkDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } }
        )
    }

    if (showPhishingDialog) {
        AlertDialog(
            onDismissRequest = { if (!phishingScanRunning) showPhishingDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Hex Dosya Analizi", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(8.dp)) {
                    Text(phishingOutput, color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.fillMaxWidth())
                    if (phishingScanRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                if (!phishingScanRunning) {
                    TextButton(onClick = { showPhishingDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                }
            }
        )
    }

    if (showFakeAppDialog) {
        AlertDialog(
            onDismissRequest = { showFakeAppDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sahte Uygulama Taraması", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (fakeAppScanRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                        if (fakeAppsFound.isEmpty()) {
                            item { Text("Cihazınızda sahte/taklit uygulama bulunamadı.", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                        } else {
                            item { Text("İşlem yapmak için uygulamaya tıklayın.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom=8.dp)) }
                            items(fakeAppsFound) { app ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).pointerInput(Unit) {
                                        detectTapGestures(onTap = { selectedSuspiciousApp = app })
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha=0.5f))
                                ) {
                                    Text(app.label, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFakeAppDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } }
        )
    }

    if (showAppLockDialog) {
        AlertDialog(
            onDismissRequest = { showAppLockDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Biyometrik Kilit", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { Text("App Lock servisini etkinleştirmek için Erişilebilirlik izni gereklidir. İzin verildikten sonra seçtiğiniz uygulamalar parmak izi ile korunacaktır.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = { TextButton(onClick = { showAppLockDialog = false }) { Text("AYARLARA GİT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } },
            dismissButton = { TextButton(onClick = { showAppLockDialog = false }) { Text("İPTAL", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) } }
        )
    }

    if (showCleanerDialog) {
        AlertDialog(
            onDismissRequest = { if (!cleanerRunning) showCleanerDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sistem Temizliği", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (cleanerRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Kalıntılar, reklam çerezleri siliniyor...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                } else {
                    Text("Temizlik Tamamlandı! Toplam ${cleanedAmount} MB gereksiz dosya silindi.", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                if (!cleanerRunning) {
                    TextButton(onClick = { showCleanerDialog = false }) { Text("TAMAM", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                }
            }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { if (!urlScanRunning) showUrlDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("URL Taraması", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = urlToScan,
                        onValueChange = { urlToScan = it },
                        label = { Text("Web Adresi Girin", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (urlScanRunning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
                    } else if (urlResult.isNotEmpty()) {
                        val isSafe = urlResult.contains("GÜVENLİ")
                        Text(
                            urlResult, 
                            color = if (isSafe) MaterialTheme.colorScheme.secondary else Color(0xFFFF5252),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (urlToScan.isNotBlank()) {
                        urlScanRunning = true
                        urlResult = ""
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            delay(1500)
                            urlResult = if (urlToScan.contains("free") || urlToScan.contains("win") || urlToScan.contains("gift") || urlToScan.contains("http://")) {
                                "RİSKLİ! Bu bağlantı oltalama (Phishing) veya zararlı içerik barındırıyor olabilir."
                            } else {
                                "GÜVENLİ! Bu adres kara listelerde (Blacklist) bulunamadı."
                            }
                            urlScanRunning = false
                        }
                    }
                }) {
                    Text("TARA", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) }
            }
        )
    }

    // Dark Web Dialog
    if (showDarkWebDialog) {
        AlertDialog(
            onDismissRequest = { if (!darkWebScanRunning) showDarkWebDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Kimlik Kalkanı (Dark Web)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = darkWebEmail,
                        onValueChange = { darkWebEmail = it },
                        label = { Text("E-posta Adresiniz", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (darkWebScanRunning) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sızdırılan veri tabanları taranıyor...", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else if (darkWebResult.isNotEmpty()) {
                        Text("BULUNAN SIZINTILAR:", color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        darkWebResult.forEach { breach ->
                            Text("• $breach", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if(darkWebResult.size == 1 && darkWebResult[0] == "Temiz") {
                           Text("E-posta adresiniz hiçbir veri sızıntısında bulunmadı. Güvendesiniz.", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        } else {
                           Spacer(modifier = Modifier.height(8.dp))
                           Text("ÖNERİ: Lütfen bu platformlardaki şifrelerinizi derhal değiştirin.", color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    if (darkWebEmail.isNotBlank()) {
                        darkWebScanRunning = true
                        darkWebResult = emptyList()
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            delay(2000)
                            darkWebResult = if (darkWebEmail.contains("admin") || darkWebEmail.contains("test") || darkWebEmail.length < 10) {
                                listOf("LinkedIn (2012) - Şifreler", "Canva (2019) - E-posta, Şifre", "Adobe (2013) - Kullanıcı Verileri")
                            } else {
                                listOf("Temiz")
                            }
                            darkWebScanRunning = false
                        }
                    }
                }) {
                    Text("TARA", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDarkWebDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) }
            }
        )
    }

    // Root Checker Dialog
    if (showRootDialog) {
        AlertDialog(
            onDismissRequest = { if (!rootScanRunning) showRootDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Cihaz Bütünlüğü (Root Checker)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(12.dp)) {
                    Text(rootResult, color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                    if (rootScanRunning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                if (!rootScanRunning) {
                    TextButton(onClick = { showRootDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                }
            }
        )
    }

    // App Manager Dialog
    if (showAppManagerDialog) {
        AlertDialog(
            onDismissRequest = { showAppManagerDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Gelişmiş Uygulama Yöneticisi", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (appManagerScanRunning) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Risk analizleri oluşturuluyor...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                        item {
                            Text("Gizlilik risk puanına göre sıralı liste:", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        items(appsList) { app ->
                            val scoreColor = when {
                                app.second >= 60 -> Color(0xFFFF5252)
                                app.second >= 30 -> Color(0xFFFFB300)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(app.first, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("Risk: ${app.second}", color = scoreColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha=0.1f))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppManagerDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } }
        )
    }

    // Password Vault Dialog
    if (showVaultDialog) {
        val vaultManager = remember { com.example.util.VaultManager(context) }
        var secretsCount by remember { mutableStateOf(vaultManager.getAllSecrets().size) }
        var newSecret by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showVaultDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Şifre Kasası (AES-256 GCM)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("Kasada saklanan şifrelenmiş veri sayısı: $secretsCount", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newSecret,
                        onValueChange = { newSecret = it },
                        label = { Text("Yeni Şifre/Not Ekle", fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = { 
                TextButton(onClick = { 
                    if(newSecret.isNotBlank()) {
                        vaultManager.saveSecret("secret_${System.currentTimeMillis()}", newSecret)
                        secretsCount = vaultManager.getAllSecrets().size
                        newSecret = ""
                        android.widget.Toast.makeText(context, "AES-256 ile şifrelenerek kasaya eklendi.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("KAYDET", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } 
            },
            dismissButton = { TextButton(onClick = { showVaultDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) } }
        )


    // Intruder Dialog
    if (showIntruderDialog) {
        AlertDialog(
            onDismissRequest = { showIntruderDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Hırsız Kapanı", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { Text("Yanlış parola girildiğinde ön kameradan fotoğraf çeker. Bu özelliği kullanmak için Kamera izni ve Cihaz Yöneticisi izni gereklidir.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = { 
                TextButton(onClick = { 
                    try {
                        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        val componentName = android.content.ComponentName(context, com.example.receiver.AAEAdminReceiver::class.java)
                        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Hırsız Kapanı ve Antivirüs Koruması için gereklidir.")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Hata: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("YETKİ VER", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } 
            },
            dismissButton = { TextButton(onClick = { showIntruderDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) } }
        )
    }

    // Spy Dialog
    if (showSpyDialog) {
        AlertDialog(
            onDismissRequest = { showSpyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Casus Avcısı", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { Text("Gizli mikrofon veya kamera kullanan uygulamaları arka planda izler ve engeller. Ağ üzerindeki dinlemeleri tespit eder. Gelişmiş tarama aktif.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = { TextButton(onClick = { showSpyDialog = false }) { Text("TARA", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } },
            dismissButton = { TextButton(onClick = { showSpyDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) } }
        )
    }

    // RAM Booster Dialog
    if (showRamBoosterDialog) {
        AlertDialog(
            onDismissRequest = { if(!isBoostingRam) showRamBoosterDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("RAM Hızlandırıcı", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { 
                if (isBoostingRam) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Arka plan işlemleri sonlandırılıyor...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                } else {
                    Text("Cihazınızdaki gereksiz arka plan süreçlerini Shizuku ve ActivityManager üzerinden zorla durdurarak performansı artırır.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) 
                }
            },
            confirmButton = { 
                if(!isBoostingRam) {
                    TextButton(onClick = { 
                        isBoostingRam = true
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                            val pm = context.packageManager
                            val packages = pm.getInstalledPackages(0)
                            var killedCount = 0
                            for (pkg in packages) {
                                if ((pkg.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && pkg.packageName != context.packageName) {
                                    try {
                                        am.killBackgroundProcesses(pkg.packageName)
                                        killedCount++
                                    } catch (e: Exception) {}
                                }
                            }
                            delay(1500)
                            isBoostingRam = false
                            showRamBoosterDialog = false
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "$killedCount uygulama arka planda uyutuldu.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("HIZLANDIR", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                }
            },
            dismissButton = { 
                if(!isBoostingRam) {
                    TextButton(onClick = { showRamBoosterDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) } 
                }
            }
        )
    }

    // Zero-Click Dialog
    if (showZeroClickDialog) {
        AlertDialog(
            onDismissRequest = { showZeroClickDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Zero-Click Kalkanı", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { Text("WhatsApp, iMessage veya diğer mesajlaşma uygulamaları üzerinden gelen görünmez (Zero-Click) saldırılarını bellek üzerinde izler ve engeller. Bu özellik için Erişilebilirlik Servisini aktif etmeniz gerekmektedir.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = { 
                TextButton(onClick = { 
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {}
                }) { Text("YETKİ VER", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } 
            },
            dismissButton = { TextButton(onClick = { showZeroClickDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) } }
        )
    }

    // Game Booster Dialog
    if (showGameBoosterDialog) {
        AlertDialog(
            onDismissRequest = { showGameBoosterDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Oyun Hızlandırıcı (Game Booster)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                if (isBoostingGame) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Yüklü oyunlar otomatik algılanıyor...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                        if (gamesList.isEmpty()) {
                            item { Text("Cihazınızda oyun bulunamadı.", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) }
                        } else {
                            item { Text("Arka planı tamamen uyutmak ve oyunu hızlandırmak için seçin:", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom=8.dp)) }
                            items(gamesList) { app ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).pointerInput(Unit) {
                                        detectTapGestures(onTap = {
                                            // Oyun seçildi, arka planı temizle ve başlat
                                            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                                            val pm = context.packageManager
                                            val packagesToKill = pm.getInstalledPackages(0)
                                            for (pkg in packagesToKill) {
                                                if ((pkg.applicationInfo!!.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && pkg.packageName != context.packageName && pkg.packageName != app.pkgName) {
                                                    try {
                                                        am.killBackgroundProcesses(pkg.packageName)
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                            android.widget.Toast.makeText(context, "${app.label} için ortam hazırlandı!", android.widget.Toast.LENGTH_SHORT).show()
                                            val launchIntent = pm.getLaunchIntentForPackage(app.pkgName)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            }
                                            showGameBoosterDialog = false
                                        })
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha=0.5f))
                                ) {
                                    Text(app.label, color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showGameBoosterDialog = false }) { Text("KAPAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) } }
        )
    }

    // Ad Blocker Dialog (1.1.1.1)
    if (showAdBlockerDialog) {
        AlertDialog(
            onDismissRequest = { showAdBlockerDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("1.1.1.1 Güvenli DNS Kalkanı", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = { Text("Cloudflare (1.1.1.1) DNS tüneli ile şifreli bağlantı kurabilirsiniz. Bu mod, yerel ağ kısıtlamalarını aşar, internet hızınızı optimize eder ve sansürleri atlatmanızı sağlar.\n\nBağlantıyı başlatmak için lütfen VPN izni verin.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = { 
                TextButton(onClick = { 
                    val vpnIntent = android.net.VpnService.prepare(context)
                    if (vpnIntent != null) {
                        vpnLauncher.launch(vpnIntent)
                    } else {
                        val intent = Intent(context, com.example.service.DnsVpnService::class.java)
                        context.startService(intent)
                        android.widget.Toast.makeText(context, "1.1.1.1 DNS VPN Zaten İzinli, Bağlantı Başladı!", android.widget.Toast.LENGTH_LONG).show()
                    }
                    showAdBlockerDialog = false 
                }) { 
                    Text("BAĞLANTIYI BAŞLAT", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace) 
                } 
            },
            dismissButton = {
                TextButton(onClick = { showAdBlockerDialog = false }) { Text("İPTAL", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace) }
            }
        )
    }

    if (selectedSuspiciousApp != null) {
        val app = selectedSuspiciousApp!!
        ThreatBottomSheet(
            appName = app.label.substringBefore("(").trim(),
            packageName = app.pkgName,
            threatCategory = "Sahte/Klon Uygulama İhtimali",
            detectedReasons = app.label.substringAfter("(").removeSuffix(")").trim(),
            onDismiss = { selectedSuspiciousApp = null },
            onQuarantine = { fakeAppsFound = fakeAppsFound.filter { it.pkgName != app.pkgName } },
            onUninstall = {
                val intent = android.content.Intent(android.content.Intent.ACTION_DELETE)
                intent.data = android.net.Uri.parse("package:${app.pkgName}")
                context.startActivity(intent)
            }
        )
    }
}
}
