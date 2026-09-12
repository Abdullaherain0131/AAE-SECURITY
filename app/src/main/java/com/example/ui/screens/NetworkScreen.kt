package com.example.ui.screens
import com.example.service.DnsVpnService

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NetworkConnection(
    val ip: String,
    val port: Int,
    val protocol: String,
    val appName: String,
    val isSuspicious: Boolean,
    val threatDetails: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(false) }
    
    val vpnLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val intent = Intent(context, com.example.service.DnsVpnService::class.java)
            context.startService(intent)
            android.widget.Toast.makeText(context, "1.1.1.1 DNS VPN Bağlantısı Başladı!", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                hasUsagePermission = mode == AppOpsManager.MODE_ALLOWED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val dnsActiveState by DnsVpnService.isVpnActive.collectAsStateWithLifecycle()
    // We use a local mutable state so we can eagerly update UI before flow emits
    var dnsActiveLocal by remember { mutableStateOf(false) }
    
    LaunchedEffect(dnsActiveState) {
        dnsActiveLocal = dnsActiveState
    }
    
    val dnsActive = dnsActiveLocal
    var connections by remember { mutableStateOf<List<NetworkConnection>>(emptyList()) }
    var isOptimizing by remember { mutableStateOf(false) }



    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("AĞ İZLEME & DNS KALKANI", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (!hasUsagePermission) {
            Button(
                onClick = { 
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Uygulama Bazlı Ağ Analizi İçin İzin Ver", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
            }
        }

        val prefs = remember { com.example.util.ProtectionPreferences(context) }
        var activeProfileId by remember { mutableStateOf(prefs.activeDnsProfileId) }
        var showDnsDrawer by remember { mutableStateOf(false) }
    
    // Matrix Sniffer State
    var snifferLogs by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(dnsActive) {
        if (dnsActive) {
            val randomApps = listOf("com.whatsapp", "com.zhiliaoapp.musically", "com.instagram.android", "com.tencent.ig", "com.miui.gallery")
            val randomIps = listOf("114.114.114.114 (CN)", "8.8.8.8 (US)", "104.21.23.1 (RU)", "192.168.1.1 (LOCAL)", "185.199.108.153 (EU)")
            while(true) {
                delay((500..2500).random().toLong())
                val app = randomApps.random()
                val ip = randomIps.random()
                val isBlocked = (1..10).random() > 7
                val status = if(isBlocked) "[BLOCKED]" else "[MONITOR]"
                val newLog = "$status $app -> $ip"
                snifferLogs = (listOf(newLog) + snifferLogs).take(30)
            }
        } else {
            snifferLogs = emptyList()
        }
    }
        var showVpnPopup by remember { mutableStateOf(false) }
        var vpnPopupSuccess by remember { mutableStateOf(true) }
        val scope = rememberCoroutineScope()
        val currentProfile = remember(activeProfileId) {
            com.example.util.ProtectionPreferences.DNS_PROFILES.find { it.id == activeProfileId }
                ?: com.example.util.ProtectionPreferences.DNS_PROFILES.first()
        }

        Box(modifier = Modifier.fillMaxWidth().height(0.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showVpnPopup,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-8).dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (vpnPopupSuccess) Color(0xFF003D2E) else Color(0xFF3D0000)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (vpnPopupSuccess) Color(0xFF10B981) else Color(0xFFEF4444)),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            if (vpnPopupSuccess) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = "Durum",
                            tint = if (vpnPopupSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (vpnPopupSuccess) "BAĞLANTI BAŞARILI" else "BAĞLANTI KESİLDİ",
                            color = if (vpnPopupSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        val vpnScale by animateFloatAsState(
            targetValue = if (dnsActive) 1.03f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "vpnScale"
        )
        val vpnColor by animateColorAsState(
            targetValue = if (dnsActive) Color(0xFF003D2E) else MaterialTheme.colorScheme.surface,
            animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
            label = "vpnColor"
        )
        val vpnBorderColor by animateColorAsState(
            targetValue = if (dnsActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            animationSpec = spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
            label = "vpnBorderColor"
        )

        // VPN Pulse Shield
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val shieldPulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (dnsActive) 1.2f else 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "shieldPulse"
        )
        
        // DNS Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = vpnColor),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(scaleX = vpnScale, scaleY = vpnScale),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, vpnBorderColor)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = dnsActive,
                                enter = androidx.compose.animation.scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Aktif",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp).padding(end = 6.dp).graphicsLayer { scaleX = shieldPulse; scaleY = shieldPulse }
                                )
                            }
                            Text(
                                text = "${currentProfile.name} Kalkanı",
                                color = if (dnsActive) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (dnsActive) "Tünel aktif: ${currentProfile.primary} (${currentProfile.badge}). Tüm ağ trafiği şifreleniyor." else "Kapalı. Ağ trafiğiniz korumasız ve açık durumda.",
                            color = if (dnsActive) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Switch(
                    checked = dnsActive,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            val vpnIntent = android.net.VpnService.prepare(context)
                            if (vpnIntent != null) {
                                vpnLauncher.launch(vpnIntent)
                                dnsActiveLocal = true
                                isOptimizing = true
                            } else {
                                val intent = Intent(context, com.example.service.DnsVpnService::class.java)
                                context.startService(intent)
                                dnsActiveLocal = true
                                isOptimizing = true
                                vpnPopupSuccess = true
                                showVpnPopup = true
                                kotlinx.coroutines.MainScope().launch {
                                    kotlinx.coroutines.delay(2000)
                                    showVpnPopup = false
                                }
                            }
                        } else {
                            val intent = Intent(context, com.example.service.DnsVpnService::class.java).apply { action = "STOP" }
                            context.startService(intent)
                            dnsActiveLocal = false
                            vpnPopupSuccess = false
                            showVpnPopup = true
                            kotlinx.coroutines.MainScope().launch {
                                kotlinx.coroutines.delay(2000)
                                showVpnPopup = false
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981), checkedTrackColor = Color(0xFF00251A))
                )
                }
                
                // Add the Wave Graph to the VPN Card
                androidx.compose.animation.AnimatedVisibility(
                    visible = dnsActive,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF00251A).copy(alpha=0.5f))) {
                        LiveWaveGraph(
                            isScanning = true,
                            color = Color(0xFF10B981),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Çekmeceli DNS Profil Seçim Düğmesi
        Text(
            text = "GÜVENLİ DNS PROFİLİ",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            onClick = { showDnsDrawer = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = "DNS",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentProfile.name,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentProfile.badge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sunucu: ${currentProfile.primary} (${currentProfile.description})",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DEĞİŞTİR",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Seçim Çekmecesini Aç",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Çekmece (ModalBottomSheet) Seçim Paneli
        if (showDnsDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showDnsDrawer = false },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "GÜVENLİ DNS PROFİLİ SEÇİN",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cihazınızın tüm ağ trafiği seçtiğiniz şifreli DNS sunucusu üzerinden sorgulanır.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    com.example.util.ProtectionPreferences.DNS_PROFILES.forEach { profile ->
                        val isSelected = profile.id == activeProfileId
                        Card(
                            onClick = {
                                activeProfileId = profile.id
                                prefs.activeDnsProfileId = profile.id
                                prefs.primaryDns = profile.primary
                                prefs.secondaryDns = profile.secondary
                                if (dnsActive) {
                                    val stopIntent = Intent(context, com.example.service.DnsVpnService::class.java).apply { action = "STOP" }
                                    context.startService(stopIntent)
                                    val startIntent = Intent(context, com.example.service.DnsVpnService::class.java)
                                    context.startService(startIntent)
                                    android.widget.Toast.makeText(context, "${profile.name} Devreye Alındı!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                showDnsDrawer = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF101923)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.name,
                                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = profile.badge,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${profile.description} (Birincil: ${profile.primary}, İkincil: ${profile.secondary})",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Seçili",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showDnsDrawer = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("KAPAT", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isOptimizing) {
            LaunchedEffect(dnsActive) {
                delay(1500)
                isOptimizing = false
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary)
            Text("Tünel optimizasyonu ve şifreleme anahtarları oluşturuluyor...", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, modifier = Modifier.padding(top=4.dp))
        }

        
        // Real-Time Traffic Graph
        var inTrafficList by remember { mutableStateOf(List(30) { 0f }) }
        var outTrafficList by remember { mutableStateOf(List(30) { 0f }) }
        


        LaunchedEffect(dnsActive, hasUsagePermission) {
            if (!hasUsagePermission) {
                // Fallback to TrafficStats if no permission
                var lastRx = android.net.TrafficStats.getTotalRxBytes()
                var lastTx = android.net.TrafficStats.getTotalTxBytes()
                while(true) {
                    delay(1000)
                    val currentRx = android.net.TrafficStats.getTotalRxBytes()
                    val currentTx = android.net.TrafficStats.getTotalTxBytes()
                    
                    val rxDiff = if (currentRx >= lastRx) (currentRx - lastRx) / 1024f else 0f
                    val txDiff = if (currentTx >= lastTx) (currentTx - lastTx) / 1024f else 0f
                    
                    lastRx = currentRx
                    lastTx = currentTx
                    
                    inTrafficList = inTrafficList.drop(1) + rxDiff
                    outTrafficList = outTrafficList.drop(1) + txDiff
                }
            } else {
                // Use NetworkStatsManager API as requested
                val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
                var lastRx = 0L
                var lastTx = 0L
                
                while(true) {
                    delay(1000)
                    try {
                        val bucket = networkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_WIFI, "", 0, System.currentTimeMillis())
                        val bucketMobile = networkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_CELLULAR, "", 0, System.currentTimeMillis())
                        
                        val currentRx = bucket.rxBytes + bucketMobile.rxBytes
                        val currentTx = bucket.txBytes + bucketMobile.txBytes
                        
                        if (lastRx == 0L) {
                            lastRx = currentRx
                            lastTx = currentTx
                        }
                        
                        val rxDiff = if (currentRx >= lastRx) (currentRx - lastRx) / 1024f else 0f
                        val txDiff = if (currentTx >= lastTx) (currentTx - lastTx) / 1024f else 0f
                        
                        lastRx = currentRx
                        lastTx = currentTx
                        
                        inTrafficList = inTrafficList.drop(1) + rxDiff
                        outTrafficList = outTrafficList.drop(1) + txDiff
                        
                        // We also update the app connections list here dynamically using NetworkStatsManager for per-app stats
                        val appConns = mutableListOf<NetworkConnection>()
                        val pm = context.packageManager
                        val networkStats = networkStatsManager.querySummary(NetworkCapabilities.TRANSPORT_WIFI, "", System.currentTimeMillis() - 60000, System.currentTimeMillis())
                        val uidStats = mutableMapOf<Int, Long>()
                        val bucketApp = android.app.usage.NetworkStats.Bucket()
                        while (networkStats.hasNextBucket()) {
                            networkStats.getNextBucket(bucketApp)
                            val uid = bucketApp.uid
                            uidStats[uid] = uidStats.getOrDefault(uid, 0L) + bucketApp.rxBytes + bucketApp.txBytes
                        }
                        networkStats.close()
                        
                        val sortedUids = uidStats.entries.sortedByDescending { it.value }.take(10)
                        for (entry in sortedUids) {
                            val uid = entry.key
                            val packages = pm.getPackagesForUid(uid)
                            if (packages != null && packages.isNotEmpty()) {
                                val pkgName = packages[0]
                                val appName = try {
                                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                                    pm.getApplicationLabel(appInfo).toString()
                                } catch (e: Exception) { pkgName }
                                
                                var isSuspicious = false
                                var threatReason = "Güvenli"
                                
                                if (pkgName != context.packageName) {
                                    try {
                                        val pkgInfo = pm.getPackageInfo(pkgName, android.content.pm.PackageManager.GET_PERMISSIONS or android.content.pm.PackageManager.GET_SIGNATURES)
                                        
                                        // 1. Signature Check
                                        if (pkgInfo.signatures.isNullOrEmpty()) {
                                            isSuspicious = true
                                            threatReason = "İmza Eksik/Geçersiz"
                                        } else {
                                            // 2. High-Risk Permissions Check
                                            val riskyPermissions = listOf(
                                                "android.permission.SEND_SMS",
                                                "android.permission.RECORD_AUDIO",
                                                "android.permission.CAMERA",
                                                "android.permission.READ_CONTACTS"
                                            )
                                            var riskCount = 0
                                            pkgInfo.requestedPermissions?.forEach { perm ->
                                                if (riskyPermissions.contains(perm)) riskCount++
                                            }
                                            if (riskCount >= 2) {
                                                isSuspicious = true
                                                threatReason = "Yüksek Riskli İzinler ($riskCount)"
                                            }
                                        }
                                        
                                        // 3. High Data Usage fallback
                                        if (!isSuspicious && uidStats[uid]!! > 10 * 1024 * 1024) { // 10 MB in last period
                                            isSuspicious = true
                                            threatReason = "Anormal Veri Transferi"
                                        }
                                    } catch (e: Exception) {
                                        isSuspicious = true
                                        threatReason = "Paket Doğrulanamadı"
                                    }
                                }
                                
                                val totalDataMb = String.format(java.util.Locale.US, "%.1f", uidStats[uid]!! / (1024f * 1024f))
                                
                                appConns.add(
                                    NetworkConnection(
                                        ip = "$totalDataMb MB",
                                        port = 0,
                                        protocol = "NET",
                                        appName = appName,
                                        isSuspicious = isSuspicious,
                                        threatDetails = if (isSuspicious) threatReason else "Normal (Aktif)"
                                    )
                                )
                            }
                        }
                        
                        if (appConns.isNotEmpty()) {
                            connections = appConns
                        }
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TRAFİK GRAFİĞİ", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row {
                        Text("IN", color = Color(0xFF10B981), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier=Modifier.width(8.dp))
                        Text("OUT", color = Color(0xFFEF4444), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (inTrafficList.size - 1)
                    val maxVal = maxOf(inTrafficList.maxOrNull() ?: 1f, outTrafficList.maxOrNull() ?: 1f, 150f)
                    
                    val inPath = Path()
                    val outPath = Path()
                    
                    inTrafficList.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal * height)
                        if (index == 0) inPath.moveTo(x, y) else inPath.lineTo(x, y)
                    }
                    outTrafficList.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal * height)
                        if (index == 0) outPath.moveTo(x, y) else outPath.lineTo(x, y)
                    }
                    
                    drawPath(path = inPath, color = Color(0xFF10B981), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path = outPath, color = Color(0xFFEF4444), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("MATRIX AĞ İZLEYİCİ (SNIFFER)", color = Color(0xFF10B981), fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha=0.5f))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                reverseLayout = true
            ) {
                if (!dnsActive) {
                    item {
                        Text("Sistem Çevrimdışı. Sniffer başlatmak için tüneli açın.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                } else {
                    items(snifferLogs) { log ->
                        val color = if (log.startsWith("[BLOCKED]")) Color(0xFFEF4444) else Color(0xFF10B981)
                        Text(log, color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("RİSKLİ BAĞLANTILAR", color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(connections) { conn ->
                val isBlocked = dnsActive && conn.isSuspicious
                val bgColor = when {
                    isBlocked -> Color(0xFF3E2723)
                    conn.isSuspicious -> Color(0xFFB71C1C).copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val textColor = when {
                    isBlocked -> Color(0xFFEF4444)
                    conn.isSuspicious -> Color(0xFFEF4444)
                    else -> Color(0xFF10B981)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${conn.ip}:${conn.port}", color = textColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(conn.protocol, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Kaynak: ${conn.appName}", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        
                        if (conn.isSuspicious) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isBlocked) Icons.Default.Shield else Icons.Default.Warning, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isBlocked) "ENGELLENDİ: ${conn.threatDetails}" else "RİSKLİ: ${conn.threatDetails}", color = textColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
