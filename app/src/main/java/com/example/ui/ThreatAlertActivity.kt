package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri

class ThreatAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appName = intent.getStringExtra("APP_NAME") ?: "Bilinmeyen Uygulama"
        val pkgName = intent.getStringExtra("PKG_NAME") ?: ""
        val category = intent.getStringExtra("CATEGORY") ?: ""
        val reasons = intent.getStringExtra("REASONS") ?: ""
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = "Threat",
                            tint = Color.Red,
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "KRİTİK TEHDİT ENGELLENDİ!",
                            color = Color.Red,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha=0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Hedef: $appName", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Paket: $pkgName", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Kategori: $category", color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Teşhis: $reasons", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            "Bu uygulama otonom sistemler tarafından tehlikeli olarak sınıflandırıldı. Derhal kaldırılması önerilir.",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:$pkgName")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(intent)
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("HEMEN İMHÂ ET", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { finish() }) {
                            Text("Riski Kabul Edip Çık", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
