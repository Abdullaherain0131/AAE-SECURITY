package com.example.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ShizukuUtils
import rikka.shizuku.Shizuku
import kotlinx.coroutines.delay

@Composable
fun ShizukuWizardDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isShizukuRunning by remember { mutableStateOf(ShizukuUtils.isAvailable()) }
    var hasPermission by remember { mutableStateOf(ShizukuUtils.hasPermission()) }
    
    val listener = remember {
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == 1001) {
                hasPermission = grantResult == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    onSuccess()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }

    LaunchedEffect(isShizukuRunning, hasPermission) {
        while (true) {
            delay(1000)
            val currentlyRunning = ShizukuUtils.isAvailable()
            val currentlyGranted = ShizukuUtils.hasPermission()
            if (isShizukuRunning != currentlyRunning) isShizukuRunning = currentlyRunning
            if (hasPermission != currentlyGranted) {
                hasPermission = currentlyGranted
                if (currentlyGranted) onSuccess()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Shizuku Bağlantı Sihirbazı",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!isShizukuRunning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Shizuku servisi çalışmıyor.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sistem yetkilerine erişmek için Shizuku uygulamasını açıp servisi başlatmanız gerekmektedir (Örn. Kablosuz Hata Ayıklama üzerinden).",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Shizuku Uygulamasını Aç")
                    }
                } else if (!hasPermission) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Servis Çalışıyor - İzin Gerekli",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Antivirüs'ün Shizuku komutlarını kullanabilmesi için izin vermelisiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            try {
                                ShizukuUtils.requestPermission(1001)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("İzin İste")
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bağlantı Başarılı!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onSuccess,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tamam")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
            }
        }
    }
}
