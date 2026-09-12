package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AntivirusViewModel
import com.example.ui.viewmodel.SystemAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMonitorScreen(
    viewModel: AntivirusViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val systemApps by viewModel.systemApps.collectAsStateWithLifecycle()
    val isShizukuEnabled by viewModel.isShizukuEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadSystemApps(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sistem Süreç Yöneticisi", fontFamily = FontFamily.Monospace, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isShizukuEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Shizuku API Kapalı", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sistem uygulamalarını dondurmak veya durdurmak için Ayarlar'dan Shizuku'yu aktifleştirin.", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(systemApps, key = { _, app -> app.packageName }) { index, app ->
                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(app.packageName) {
                        kotlinx.coroutines.delay(index * 70L)
                        itemVisible = true
                    }
                    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = itemVisible,
                        enter = androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { 200 },
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow))
                    ) {
                        SystemAppItem(
                            app = app,
                            isShizukuEnabled = isShizukuEnabled,
                            onFreezeToggle = { enabled ->
                                viewModel.toggleSystemApp(app.packageName, enabled, context)
                            },
                            onForceStop = {
                                viewModel.forceStopSystemApp(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemAppItem(
    app: SystemAppInfo,
    isShizukuEnabled: Boolean,
    onFreezeToggle: (Boolean) -> Unit,
    onForceStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = if (app.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bellek Tüketimi: ~${app.memoryUsageKb / 1024} MB",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (app.memoryUsageKb > 150 * 1024) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onForceStop,
                        enabled = isShizukuEnabled && app.isEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Durdur", fontSize = 10.sp)
                    }

                    Button(
                        onClick = { onFreezeToggle(!app.isEnabled) },
                        enabled = isShizukuEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isEnabled) Color(0xFF00796B) else Color(0xFFE64A19)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (app.isEnabled) "Dondur" else "Uyandır", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
