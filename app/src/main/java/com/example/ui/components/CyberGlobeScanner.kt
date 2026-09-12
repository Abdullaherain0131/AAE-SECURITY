package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.theme.Motion
import com.example.ui.theme.reduceMotion
import kotlin.math.*

private data class GeoNode(val lat: Float, val lon: Float, val continent: Int)

@Composable
fun TypewriterText(text: String, modifier: Modifier = Modifier, color: Color, fontSize: androidx.compose.ui.unit.TextUnit, fontFamily: FontFamily = FontFamily.Monospace) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        // Hareket azaltma açıksa daktilo etkisi atlanır: metin tek seferde yazılır.
        if (reduceMotion) {
            displayedText = text
            return@LaunchedEffect
        }
        displayedText = ""
        for (i in text.indices) {
            displayedText += text[i]
            delay((10L..30L).random())
        }
    }
    
    Text(
        text = displayedText + if (displayedText.length < text.length || displayedText.isEmpty()) "█" else "",
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        modifier = modifier
    )
}

@Composable
fun CyberGlobeScanner(
    isScanning: Boolean,
    scanProgress: Float,
    currentAppName: String,
    primaryColor: Color,
    secondaryColor: Color,
    surfaceColor: Color,
    activeThreatsCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduce = reduceMotion
    val infiniteTransition = rememberInfiniteTransition(label = "GlobeTransitions")

    // Hareket azaltma açıksa hiçbir döngü başlamaz; her faz son karesinde donar.
    // Döngü süreleri birer geçiş değil, etkinin kendi ritmidir (bkz. Motion.loop).

    // Rotation speed: smooth & stately when idle, fast and energetic when scanning
    val rotationDuration = if (isScanning) 4000 else 16000
    val globeRotation by if (reduce) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = Motion.loop(rotationDuration),
            label = "GlobeRotation"
        )
    }

    // Radar sweep rotation
    val radarAngle by if (reduce) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = Motion.loop(if (isScanning) 2200 else 7000),
            label = "RadarAngle"
        )
    }

    // Vertical scanning laser slice
    val scanSlicePhase by if (reduce) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = -1.15f,
            targetValue = 1.15f,
            animationSpec = Motion.pulse(4500),
            label = "ScanSlicePhase"
        )
    }

    // Atmospheric pulse
    val atmospherePulse by if (reduce) {
        remember { mutableFloatStateOf(1f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = Motion.pulse(2000),
            label = "AtmospherePulse"
        )
    }

    // Smooth scan fade
    val scanAlpha by animateFloatAsState(
        targetValue = if (isScanning) 1f else 0f,
        animationSpec = if (reduce) snap() else tween(Motion.Emphasized, easing = Motion.Standard_Easing),
        label = "ScanAlpha"
    )

    // Alert pulse for threats
    val alertPulse by if (reduce) {
        remember { mutableFloatStateOf(0.7f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = Motion.pulse(800),
            label = "AlertPulse"
        )
    }

    // Shield formation phase for safe state
    val shieldPhase by animateFloatAsState(
        targetValue = if (!isScanning && activeThreatsCount == 0) 1f else 0f,
        animationSpec = if (reduce) snap() else tween(Motion.Slow, easing = Motion.Enter),
        label = "ShieldPhase"
    )

    // Base colors adjusted for threat state
    val actualPrimary = if (activeThreatsCount > 0) Color(0xFFFF3333) else primaryColor
    val actualSecondary = if (activeThreatsCount > 0) Color(0xFFFF8888) else secondaryColor

    // Predefined geographic clusters for major continents
    val continentNodes = remember {
        listOf(
            // Europe
            GeoNode(52f, 10f, 1), GeoNode(48f, 2f, 1), GeoNode(40f, -3f, 1),
            GeoNode(42f, 12f, 1), GeoNode(55f, 37f, 1), GeoNode(60f, 25f, 1),
            // Asia
            GeoNode(35f, 105f, 2), GeoNode(28f, 77f, 2), GeoNode(35f, 139f, 2),
            GeoNode(55f, 82f, 2), GeoNode(15f, 100f, 2), GeoNode(25f, 121f, 2),
            GeoNode(39f, 116f, 2), GeoNode(45f, 65f, 2), GeoNode(30f, 60f, 2),
            // Africa
            GeoNode(0f, 20f, 3), GeoNode(10f, 0f, 3), GeoNode(30f, 31f, 3),
            GeoNode(-25f, 28f, 3), GeoNode(5f, 38f, 3), GeoNode(-10f, 35f, 3),
            // North America
            GeoNode(40f, -100f, 4), GeoNode(50f, -95f, 4), GeoNode(34f, -118f, 4),
            GeoNode(40f, -74f, 4), GeoNode(25f, -80f, 4), GeoNode(20f, -100f, 4),
            GeoNode(60f, -120f, 4),
            // South America
            GeoNode(-15f, -50f, 5), GeoNode(-23f, -43f, 5), GeoNode(5f, -60f, 5),
            GeoNode(-34f, -58f, 5), GeoNode(-12f, -77f, 5),
            // Oceania
            GeoNode(-25f, 135f, 6), GeoNode(-33f, 151f, 6), GeoNode(-37f, 144f, 6),
            GeoNode(-41f, 174f, 6)
        )
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            com.example.util.ScannerSoundManager.startScanningSound()
        } else {
            com.example.util.ScannerSoundManager.stopScanningSound()
        }
    }

    Box(
        modifier = modifier
            .size(310.dp)
            .clip(CircleShape)
            .clickable {
                if (!isScanning) onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // --- 3D CYBER DÜNYA VE HOLOGRAFİK RADAR CANVAS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val globeRadius = size.minDimension / 2.3f

            // 1. Deep Cosmic Void Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0C0C0C), // Dark Grey
                        if (activeThreatsCount > 0) Color(0xFF220000) else Color(0xFF080808),
                        Color(0xFF000000)  // Pure Black
                    ),
                    center = center,
                    radius = globeRadius * 1.1f
                ),
                radius = globeRadius,
                center = center
            )

            // 2. Outer Atmospheric Holographic Glow Ring
            val atmoGlowRadius = globeRadius * (1.03f + 0.01f * atmospherePulse * (if (isScanning) 1f else 0f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        actualPrimary.copy(alpha = if (isScanning) 0.35f else 0.18f),
                        actualPrimary.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = atmoGlowRadius * 1.15f
                ),
                radius = atmoGlowRadius * 1.1f,
                center = center
            )
            drawCircle(
                color = actualPrimary.copy(alpha = if (isScanning) 0.7f else 0.35f),
                radius = globeRadius,
                center = center,
                style = Stroke(width = if (isScanning) 2.5.dp.toPx() else 1.5.dp.toPx())
            )
            
            // 2.5 Particles (Stars)
            val seed = 12345
            val rnd = java.util.Random(seed.toLong())
            for (i in 0 until (if(isScanning) 60 else 30)) { // Reduced star count
                val angle = rnd.nextFloat() * 2 * Math.PI
                val distance = globeRadius * (0.1f + 0.9f * rnd.nextFloat())
                
                // Add some rotation to particles based on globeRotation
                val pAngle = angle + (globeRotation * (if (i % 2 == 0) 1f else -0.5f) * Math.PI / 180f)
                val pX = center.x + cos(pAngle).toFloat() * distance
                val pY = center.y + sin(pAngle).toFloat() * distance
                
                // Twinkling effect
                val twinkle = (sin(globeRotation * rnd.nextFloat() * 0.1f) + 1f) / 2f
                
                // Draw 4-pointed star instead of a circle
                val starSize = (if (isScanning) 4f else 2.5f) * rnd.nextFloat()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pX, pY - starSize)
                    lineTo(pX + starSize * 0.2f, pY - starSize * 0.2f)
                    lineTo(pX + starSize, pY)
                    lineTo(pX + starSize * 0.2f, pY + starSize * 0.2f)
                    lineTo(pX, pY + starSize)
                    lineTo(pX - starSize * 0.2f, pY + starSize * 0.2f)
                    lineTo(pX - starSize, pY)
                    lineTo(pX - starSize * 0.2f, pY - starSize * 0.2f)
                    close()
                }
                
                drawPath(
                    path = path,
                    color = actualSecondary.copy(alpha = twinkle * (if (isScanning) 0.6f else 0.4f))
                )
            }

            // 3. Parallels (Enlem Çizgileri - Latitudes with 15-degree tilt)
            val tiltAngleRad = Math.toRadians(14.0)
            val cosTilt = cos(tiltAngleRad).toFloat()
            val sinTilt = sin(tiltAngleRad).toFloat()

            val latitudes = listOf(-60f, -30f, 0f, 30f, 60f)
            for (lat in latitudes) {
                val latRad = Math.toRadians(lat.toDouble())
                val rLat = globeRadius * cos(latRad).toFloat()
                val yBase = -globeRadius * sin(latRad).toFloat()

                // Project with 3D tilt
                val centerY = center.y + yBase * cosTilt
                val ellipseHeight = 2 * rLat * sinTilt
                val ellipseWidth = 2 * rLat

                val isEquator = lat == 0f
                val ringColor = if (activeThreatsCount > 0) Color(0xFF662222) else Color(0xFF333333)
                drawOval(
                    color = ringColor.copy(alpha = if (isEquator) 0.8f else 0.4f),
                    topLeft = Offset(center.x - rLat, centerY - ellipseHeight / 2f),
                    size = Size(ellipseWidth, max(2f, ellipseHeight)),
                    style = Stroke(width = if (isEquator) 1.5.dp.toPx() else 1.dp.toPx())
                )
            }

            // 4. Meridians (3D Dönen Boylam Çizgileri)
            val meridianCount = 6
            for (i in 0 until meridianCount) {
                val baseAngle = (globeRotation + i * (360f / meridianCount)) % 360f
                val rad = Math.toRadians(baseAngle.toDouble())
                val cosAngle = cos(rad).toFloat()
                val sinAngle = sin(rad).toFloat()

                val isFront = sinAngle >= 0
                val meridianWidth = max(3f, 2 * globeRadius * abs(cosAngle))

                val meridianAlpha = if (isFront) {
                    if (isScanning) 0.7f else 0.5f
                } else {
                    0.2f // Arka taraf hafif saydam
                }

                val meridianColor = if (activeThreatsCount > 0) Color(0xFF662222) else Color(0xFF333333)
                drawOval(
                    color = meridianColor.copy(alpha = meridianAlpha),
                    topLeft = Offset(center.x - meridianWidth / 2f, center.y - globeRadius),
                    size = Size(meridianWidth, globeRadius * 2),
                    style = Stroke(
                        width = if (isFront) 1.4.dp.toPx() else 0.8.dp.toPx()
                    )
                )
            }

            // 5. Continents & Cyber Landmass Nodes (3D Küre Üzerinde Dönen Kıtalar)
            val visibleNodes = mutableListOf<Triple<Offset, Int, Float>>()
            for (node in continentNodes) {
                val currentLon = (node.lon + globeRotation) % 360f
                val lonRad = Math.toRadians(currentLon.toDouble())
                val latRad = Math.toRadians(node.lat.toDouble())

                val x3D = globeRadius * cos(latRad) * sin(lonRad)
                val y3D = -globeRadius * sin(latRad)
                val z3D = globeRadius * cos(latRad) * cos(lonRad) // +z is towards viewer

                if (z3D > 0) { // Sadece bize bakan ön yarı kürede çiz
                    val projX = center.x + x3D.toFloat()
                    val projY = center.y + (y3D * cosTilt - z3D * sinTilt).toFloat()

                    val depthFactor = (z3D / globeRadius).toFloat().coerceIn(0.2f, 1f)
                    visibleNodes.add(Triple(Offset(projX, projY), node.continent, depthFactor))
                }
            }

            // Kıta içi bağlantı ağ çizgileri
            for (i in visibleNodes.indices) {
                for (j in i + 1 until visibleNodes.size) {
                    val n1 = visibleNodes[i]
                    val n2 = visibleNodes[j]
                    if (n1.second == n2.second) { // Aynı kıta düğümleri
                        val dist = (n1.first - n2.first).getDistance()
                        if (dist < globeRadius * 0.7f) {
                            val lineAlpha = min(n1.third, n2.third) * (if (isScanning) 0.45f else 0.25f)
                            drawLine(
                                color = actualSecondary.copy(alpha = lineAlpha),
                                start = n1.first,
                                end = n2.first,
                                strokeWidth = 1.2.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Kıta veri düğümleri (Glowing Cyber Dots)
            for (node in visibleNodes) {
                val pos = node.first
                val depth = node.third

                // Dış hale
                drawCircle(
                    color = actualPrimary.copy(alpha = depth * (if (isScanning) 0.6f else 0.35f)),
                    radius = (4.dp.toPx()) * depth,
                    center = pos
                )
                // Çekirdek ışık
                drawCircle(
                    color = Color.White.copy(alpha = depth * 0.9f),
                    radius = (2.dp.toPx()) * depth,
                    center = pos
                )
            }

            // 6. Holographic Radar Sweep (Removed per user request)


            // 7. Scanning Laser Slice Ring (CT/MRI Dilim Taraması)
            if (scanAlpha > 0f) {
                val sliceYOffset = globeRadius * 0.99f * scanSlicePhase
                val sliceY = center.y + sliceYOffset
                val radAtSlice = sqrt(max(0f, globeRadius * globeRadius - sliceYOffset * sliceYOffset))

                if (radAtSlice > 1f) {
                    val sliceHeight = radAtSlice * sinTilt * 2f
                    // Glowing laser ellipse
                    drawOval(
                        color = actualPrimary.copy(alpha = 0.8f * scanAlpha),
                        topLeft = Offset(center.x - radAtSlice, sliceY - sliceHeight / 2f),
                        size = Size(radAtSlice * 2, max(3f, sliceHeight)),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.9f * scanAlpha),
                        topLeft = Offset(center.x - radAtSlice * 0.8f, sliceY - sliceHeight / 2.5f),
                        size = Size(radAtSlice * 1.6f, max(2f, sliceHeight * 0.8f)),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Orbital Satellite Tracking Ring
                val orbitRadiusX = globeRadius * 1.18f
                val orbitRadiusY = globeRadius * 0.45f
                drawOval(
                    color = actualSecondary.copy(alpha = 0.3f * scanAlpha),
                    topLeft = Offset(center.x - orbitRadiusX, center.y - orbitRadiusY),
                    size = Size(orbitRadiusX * 2, orbitRadiusY * 2),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Moving Satellite on Orbit
                val satRad = Math.toRadians((radarAngle * 1.5f).toDouble())
                val satX = center.x + orbitRadiusX * cos(satRad).toFloat()
                val satY = center.y + orbitRadiusY * sin(satRad).toFloat()
                drawCircle(
                    color = actualSecondary.copy(alpha = scanAlpha),
                    radius = 4.dp.toPx(),
                    center = Offset(satX, satY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = scanAlpha),
                    radius = 2.dp.toPx(),
                    center = Offset(satX, satY)
                )
            }

            // 8. Outer Circular Progress Indicator on Globe Rim
            if (scanAlpha > 0f) {
                drawArc(
                    color = actualPrimary.copy(alpha = scanAlpha),
                    startAngle = -90f,
                    sweepAngle = (scanProgress * 360f).coerceIn(0f, 360f),
                    useCenter = false,
                    topLeft = Offset(center.x - globeRadius - 6.dp.toPx(), center.y - globeRadius - 6.dp.toPx()),
                    size = Size((globeRadius + 6.dp.toPx()) * 2, (globeRadius + 6.dp.toPx()) * 2),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // 9. Shield Assembly Animation (When Safe)
            if (shieldPhase > 0f) {
                val hexCount = 6
                for (i in 0 until hexCount) {
                    val angle = i * (360f / hexCount) + (1f - shieldPhase) * 180f
                    val angleRad = Math.toRadians(angle.toDouble())
                    val dist = globeRadius * 1.5f * (1f - shieldPhase)
                    
                    val px = center.x + cos(angleRad).toFloat() * dist
                    val py = center.y + sin(angleRad).toFloat() * dist
                    
                    drawCircle(
                        color = actualPrimary.copy(alpha = shieldPhase * 0.7f),
                        radius = 12.dp.toPx() * shieldPhase,
                        center = Offset(px, py),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                drawCircle(
                    color = actualSecondary.copy(alpha = shieldPhase * 0.4f),
                    radius = globeRadius * 0.8f * shieldPhase,
                    center = center
                )
            }
            
            // 10. Threat Red Alert Pulsing
            if (activeThreatsCount > 0 && !isScanning) {
                drawCircle(
                    color = actualPrimary.copy(alpha = alertPulse * 0.35f),
                    radius = globeRadius * 1.15f,
                    center = center
                )
                drawCircle(
                    color = Color.Red.copy(alpha = alertPulse * 0.15f),
                    radius = globeRadius * alertPulse,
                    center = center
                )
            }
        }

        // --- MERKEZİ HOLOGRAFİK HUD VE BİLGİ ALANI ---
        Column(
            modifier = Modifier
                .size(190.dp)
                .background(
                    color = surfaceColor.copy(alpha = if (isScanning) 0.82f else 0.75f),
                    shape = CircleShape
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isScanning) {
                Text(
                    text = "${(scanProgress * 100).toInt()}%",
                    color = primaryColor,
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KÜRESEL TEHDİT TARAMASI",
                    color = secondaryColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                TypewriterText(
                    text = currentAppName.takeLast(28),
                    color = primaryColor.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(actualPrimary.copy(alpha = 0.18f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (activeThreatsCount > 0) "RİSKLİ!" else "TARAMAYI BAŞLAT",
                        color = actualPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "DÜNYA KALKANI",
                    color = secondaryColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NÖRAL MERKEZ AKTİF",
                    color = primaryColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
