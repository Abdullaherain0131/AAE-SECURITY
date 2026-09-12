package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun KvkkConsentDialog(
    onAccept: (enableBackgroundAi: Boolean) -> Unit
) {
    var isTermsAccepted by remember { mutableStateOf(false) }
    var isBackgroundAiConsent by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { /* Zorunlu onay dialogu */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f), RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Kullanıcı Sözleşmesi & KVKK",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Telif Hakları: Abdullah Asım Ersin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Legal Text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. TELİF HAKLARI & MÜLKİYET",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bu antivirüs ve yapay zeka koruma yazılımının tüm telif, fikri ve sınai mülkiyet hakları Abdullah Asım Ersin'e aittir. İzinsiz kopyalanamaz, çoğaltılamaz ve ticari amaçla izinsiz dağıtılamaz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
                            fontSize = 11.sp
                        )

                        Text(
                            text = "2. KİŞİSEL VERİLERİN KORUNMASI (KVKK)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uygulama, 6698 sayılı KVKK kapsamında cihazınızdaki kişisel dosyaları, fotoğrafları veya mesajları asla dış sunuculara aktarmaz. Tüm sezgisel ve yapay zeka taramaları çevrimdışı (cihaz içinde) gerçekleştirilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
                            fontSize = 11.sp
                        )

                        Text(
                            text = "3. GERÇEK ZAMANLI KALKAN",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cihazın zararlı APK ve SMS dolandırıcılığına karşı korunması amacıyla arka plan servisinin çalışmasına izin vermiş sayılırsınız.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Checkbox 1: Terms acceptance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isTermsAccepted,
                        onCheckedChange = { isTermsAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.secondary,
                            checkmarkColor = Color.Black,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)
                        ),
                        modifier = Modifier.testTag("kvkk_terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KVKK Aydınlatma Metnini ve Telif Hakları Sözleşmesini okudum, kabul ediyorum.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }

                // Checkbox 2: AI background self-evolution
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBackgroundAiConsent) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isBackgroundAiConsent,
                        onCheckedChange = { isBackgroundAiConsent = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = Color.Black,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f)
                        ),
                        modifier = Modifier.testTag("kvkk_ai_learning_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Yapay Zekası arkada kendi kendine gelişsin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "AAE Security sinir ağı boştayken cihazı yormadan kendi kendini eğitir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Accept Button
                Button(
                    onClick = {
                        if (isTermsAccepted) {
                            onAccept(isBackgroundAiConsent)
                        }
                    },
                    enabled = isTermsAccepted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("kvkk_accept_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = if (isTermsAccepted) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kabul Et ve Başla",
                        color = if (isTermsAccepted) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
