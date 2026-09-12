package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.motion.enterStaggered
import com.example.ui.theme.AaeText
import com.example.ui.theme.Elevation
import com.example.ui.theme.aae
import com.example.ui.theme.aaeSurface

/**
 * Tehdit ayrıntı sayfası.
 *
 * ## Değişenler
 *
 * - Kod içine gömülü `#FF5252` / `#F59E0B` / `Color.Black` / `Color.White`
 *   değerleri anlamsal belirteçlerle değiştirildi.
 * - Gelişmiş eylemler (dondur, zorla durdur, çözüldü olarak işaretle) isteğe bağlı
 *   olarak eklendi. Kart üzerinde dört-beş düğme sıkıştırmak yerine ikincil
 *   eylemler buraya taşındı; kart yalnızca en sık kullanılan ikisini gösterir.
 * - İçerik kaydırılabilir: uzun bir "tespit gerekçesi" listesi küçük ekranlarda
 *   düğmeleri ekranın dışına itiyordu.
 *
 * Boş bırakılan (`null`) eylem hiç çizilmez; çağıran ekran neyi destekliyorsa
 * onu geçer.
 */
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
    onWhitelist: (() -> Unit)? = null,
    /** Shizuku etkinken uygulamayı dondurur. */
    onFreeze: (() -> Unit)? = null,
    /** Uygulamanın çalışan süreçlerini sonlandırır. */
    onForceStop: (() -> Unit)? = null,
    /** Kaydı "çözüldü" olarak işaretler; uygulamaya dokunmaz. */
    onResolve: (() -> Unit)? = null
) {
    val danger = MaterialTheme.aae.danger
    val warning = MaterialTheme.aae.warning
    val safe = MaterialTheme.aae.safe

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.aae.surfaceHighest,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "ŞÜPHELİ UYGULAMA TESPİTİ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.enterStaggered(0)
            )

            Spacer(modifier = Modifier.height(20.dp))

            DetailBlock(label = "Hedef", index = 1) {
                Text(appName, style = AaeText.metricSmall, color = danger)
                Text(
                    packageName,
                    style = AaeText.technical,
                    color = MaterialTheme.aae.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailBlock(label = "Tespit Türü", index = 2) {
                Text(threatCategory, style = MaterialTheme.typography.titleMedium, color = danger)
            }

            if (detectedReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                DetailBlock(label = "Saldırı / Sızıntı Vektörü", index = 3) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aaeSurface(Elevation.Sunken, MaterialTheme.shapes.small)
                            .padding(12.dp)
                    ) {
                        // Virgülle ayrılmış gerekçeler tek satırda okunmuyordu;
                        // her biri kendi madde işaretine ayrılıyor.
                        detectedReasons.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { reason ->
                                Text(
                                    "• $reason",
                                    style = AaeText.console,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth().enterStaggered(4),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onQuarantine(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.aae.warningMuted,
                        contentColor = warning
                    ),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "KARANTİNA",
                        style = AaeText.badge,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onUninstall(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = danger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "SİSTEMDEN SİL",
                        style = AaeText.badge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (onFreeze != null || onForceStop != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().enterStaggered(5),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onForceStop != null) {
                        Button(
                            onClick = { onForceStop(); onDismiss() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.aae.surfaceHigh,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("ZORLA DURDUR", style = AaeText.badge, textAlign = TextAlign.Center)
                        }
                    }
                    if (onFreeze != null && onForceStop != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (onFreeze != null) {
                        Button(
                            onClick = { onFreeze(); onDismiss() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.aae.surfaceHigh,
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("DONDUR", style = AaeText.badge, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            if (onWhitelist != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onWhitelist(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = safe
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, safe.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().enterStaggered(6),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("GÜVENLİ OLARAK İŞARETLE", style = AaeText.badge)
                }
            }

            if (onResolve != null) {
                TextButton(
                    onClick = { onResolve(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Kaydı çözüldü olarak kapat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.aae.textTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Etiket + içerik ikilisi; etiket her yerde aynı ton ve ölçekte kalsın diye. */
@Composable
private fun DetailBlock(
    label: String,
    index: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().enterStaggered(index)) {
        Text(
            label.uppercase(),
            style = AaeText.sectionLabel,
            color = MaterialTheme.aae.textTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}
