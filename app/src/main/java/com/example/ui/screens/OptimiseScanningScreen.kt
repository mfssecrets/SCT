package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.ScanStatusIndicator
import com.example.ui.components.SecurityShieldIcon
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanGlow
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldPurple
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

@Composable
fun OptimiseScanningScreen(
    onScanComplete: () -> Unit,
    modifier: Modifier = Modifier,
    scanDurationMillis: Long = 3200L
) {
    var rawProgress by remember { mutableFloatStateOf(0f) }
    var currentPathIndex by remember { mutableStateOf(0) }

    // Generic scan location labels – no real filesystem paths (spec §6)
    val scanLocations = remember {
        listOf(
            "Download cache files",
            "App cache data",
            "System telemetry data",
            "Temporary files",
            "Thumbnail cache",
            "Usage statistics cache",
            "Log files",
            "System integrity data"
        )
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed.toFloat() / scanDurationMillis).coerceIn(0f, 1f)
            rawProgress = fraction * 100f
            currentPathIndex = ((elapsed / 380L) % scanLocations.size).toInt()
            if (fraction >= 1f) {
                delay(300)
                onScanComplete()
                break
            }
            delay(32)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 50, easing = FastOutSlowInEasing),
        label = "scan_progress"
    )

    val currentIntProgress = animatedProgress.toInt().coerceIn(0, 100)

    val currentScanPhase = when {
        currentIntProgress < 30 -> "Scanning for junk files"
        currentIntProgress < 60 -> "Analyzing privacy risks"
        currentIntProgress < 85 -> "Inspecting system security threats"
        else -> "Finalizing general system optimization"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("optimise_scanning_screen")
            .background(CleanShieldDeepBg)
            .drawBehind {
                // Subtle radial glow centered at upper-mid screen
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF003D5C),
                            Color(0xFF001B2E),
                            CleanShieldDeepBg
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.32f),
                        radius = size.width * 0.85f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Optimise",
                        color = CleanShieldTextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scanning your device...",
                        color = CleanShieldTextDim,
                        fontSize = 13.sp
                    )
                }

                SecurityShieldIcon()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Circular Progress Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(210.dp)
                        .testTag("scanning_gauge")
                ) {
                    val strokeWidth = 11.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Track Ring
                    drawArc(
                        color = Color(0xFF06334D),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Glowing Progress Arc
                    val sweep = (animatedProgress / 100f) * 360f
                    if (sweep > 0f) {
                        drawArc(
                            color = CleanShieldCyanGlow,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = CleanShieldCyan,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$currentIntProgress",
                            color = CleanShieldTextWhite,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 48.sp
                        )
                        Text(
                            text = " %",
                            color = CleanShieldTextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scanning...",
                        color = CleanShieldCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sub-status & Dynamic Location Readout (generic labels only)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentScanPhase,
                    color = CleanShieldTextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scanLocations.getOrElse(currentPathIndex) { "Temporary files" },
                    color = CleanShieldTextDim,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scan Records Card Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CleanShieldCardBg)
                    .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Scan records",
                        color = CleanShieldTextDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Record 1: Cleanup
                    ScanRecordRow(
                        icon = Icons.Default.Delete,
                        iconBg = Color(0xFF0288D1),
                        title = "Cleanup",
                        subtitle = if (currentIntProgress >= 30) "Cleaned 1.4 GB junk" else "Scanning junk files",
                        isCompleted = currentIntProgress >= 30,
                        isScanning = currentIntProgress < 30
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Record 2: Privacy
                    ScanRecordRow(
                        icon = Icons.Default.Lock,
                        iconBg = CleanShieldGreen,
                        title = "Privacy",
                        subtitle = if (currentIntProgress >= 60) "0 privacy risks found" else "Scanning privacy risks",
                        isCompleted = currentIntProgress >= 60,
                        isScanning = currentIntProgress in 30..59
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Record 3: Security
                    ScanRecordRow(
                        icon = Icons.Default.Security,
                        iconBg = CleanShieldPurple,
                        title = "Security",
                        subtitle = if (currentIntProgress >= 85) "System protected & secure" else "Scanning for threats",
                        isCompleted = currentIntProgress >= 85,
                        isScanning = currentIntProgress in 60..84
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Record 4: General
                    ScanRecordRow(
                        icon = Icons.Default.Settings,
                        iconBg = CleanShieldAmber,
                        title = "General",
                        subtitle = if (currentIntProgress >= 100) "All system items optimized" else "Scanning system items",
                        isCompleted = currentIntProgress >= 100,
                        isScanning = currentIntProgress in 85..99
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ScanRecordRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    // Shimmer effect for currently scanning categories
    val infiniteTransition = rememberInfiniteTransition(label = "scan_shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_shimmer_alpha"
    )

    val shimmerModifier = if (isScanning) {
        modifier.drawBehind {
            // Subtle left-to-right shimmer highlight
            val shimmerWidth = size.width * 0.4f
            val xOffset = shimmerAlpha * (size.width + shimmerWidth) - shimmerWidth
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        CleanShieldCyan.copy(alpha = 0.06f),
                        CleanShieldCyan.copy(alpha = 0.12f),
                        CleanShieldCyan.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    startX = xOffset,
                    endX = xOffset + shimmerWidth
                ),
                size = size
            )
        }
    } else {
        modifier
    }

    Row(
        modifier = shimmerModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBox(
            icon = icon,
            backgroundColor = iconBg,
            contentDescription = title
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = CleanShieldTextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = CleanShieldTextMuted,
                fontSize = 13.sp
            )
        }

        ScanStatusIndicator(
            isCompleted = isCompleted,
            isScanning = isScanning
        )
    }
}

@Preview(name = "Optimise Scanning Screen", showBackground = true)
@Composable
fun OptimiseScanningScreenPreview() {
    MyApplicationTheme {
        OptimiseScanningScreen(onScanComplete = {})
    }
}
