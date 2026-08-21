package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanGlow
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldTextHint
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextPrimary
import com.example.ui.theme.CleanShieldTextSecondary

@Composable
fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Log Out of Clean Shield?",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = CleanShieldDarkNavy
            )
        },
        text = {
            Text(
                text = "Are you sure you want to log out? You will need your access PIN and credentials to sign back in.",
                fontSize = 14.sp,
                color = Color(0xFF475569)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SecurityShieldIcon(
    modifier: Modifier = Modifier,
    tint: Color = CleanShieldCyan
) {
    Icon(
        imageVector = Icons.Outlined.Shield,
        contentDescription = "Security Shield",
        tint = tint,
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun CategoryIconBox(
    icon: ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(13.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun ScanStatusIndicator(
    isCompleted: Boolean,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicator_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    when {
        isCompleted -> {
            Box(
                modifier = modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(CleanShieldCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color(0xFF00223A),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        isScanning -> {
            Canvas(
                modifier = modifier
                    .size(26.dp)
                    .rotate(rotation)
            ) {
                // Background faint ring
                drawCircle(
                    color = Color(0x3300D4EC),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                // Active spinning arc
                drawArc(
                    color = CleanShieldCyan,
                    startAngle = 0f,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(2.dp, CleanShieldCardBorder.copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}

@Composable
fun GlowingScoreBadge(
    score: Int = 100,
    unit: String = "pts",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(92.dp)
            .drawBehind {
                // Radiant outer cyan bloom glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CleanShieldCyan.copy(alpha = 0.65f),
                            CleanShieldCyanGlow.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width * 0.75f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner crisp badge
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, CleanShieldCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = score.toString(),
                    color = Color(0xFF0B2B43),
                    fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    lineHeight = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                androidx.compose.material3.Text(
                    text = unit,
                    color = Color(0xFF78909C),
                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Shared Loading, Error, Empty States
// ═══════════════════════════════════════════════

@Composable
fun CleanShieldSkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE2E8F0))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.height(14.dp).width(160.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0)))
            Box(modifier = Modifier.height(10.dp).width(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF1F5F9)))
        }
    }
}

@Composable
fun CleanShieldSkeletonList(itemCount: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        repeat(itemCount) {
            CleanShieldSkeletonRow()
        }
    }
}

@Composable
fun CleanShieldErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = CleanShieldAmber,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = CleanShieldTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onRetry,
            border = BorderStroke(1.dp, CleanShieldBlue),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CleanShieldBlue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CleanShieldEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CleanShieldTextMuted,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = CleanShieldTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = CleanShieldTextHint,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
