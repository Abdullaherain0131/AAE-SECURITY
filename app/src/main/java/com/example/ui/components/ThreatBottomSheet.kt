package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatBottomSheet(
    appName: String,
    packageName: String,
    threatCategory: String,
    detectedReasons: String,
    onDismiss: () -> Unit,
    onQuarantine: () -> Unit,
    onUninstall: () -> Unit,
    onWhitelist: (() -> Unit)? = null // Optional whitelist action for real threats
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = "ŞÜPHELİ UYGULAMA TESPİTİ", 
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 18.sp, 
                fontFamily = FontFamily.Monospace, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Hedef:", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(appName, color = Color(0xFFFF5252), fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(packageName, color = MaterialTheme.colorScheme.primary.copy(alpha=0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tespit Türü:", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(threatCategory, color = Color(0xFFFF5252), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            
            if (detectedReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Saldırı / Sızıntı Vektörü:", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(detectedReasons.replace(",", "\n•"), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        onQuarantine()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("KARANTİNA", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onUninstall()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SİSTEMDEN SİL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
            
            if (onWhitelist != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { 
                        onWhitelist()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GÜVENLİ (WHITELIST)", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
