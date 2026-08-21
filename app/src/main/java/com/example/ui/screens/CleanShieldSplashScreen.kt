package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanGlow
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldTextWhite
import kotlinx.coroutines.delay

@Composable
fun CleanShieldSplashScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    displayDurationMillis: Long = 2200L
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        delay(displayDurationMillis)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CleanShieldDeepBg)
            .drawSplashGlow(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha.value)
        ) {
            // Shield Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CleanShieldCyan.copy(alpha = 0.1f))
                    .drawSplashIconGlow(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Clean Shield",
                    tint = CleanShieldCyan,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Clean Shield",
                color = CleanShieldTextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Secure · Private · Optimised",
                color = CleanShieldCyan.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun Modifier.drawSplashGlow() = this.then(
    Modifier.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF003D5C).copy(alpha = 0.6f),
                    Color(0xFF001B2E).copy(alpha = 0.3f),
                    CleanShieldDeepBg
                ),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = size.width * 0.9f
            )
        )
    }
)

private fun Modifier.drawSplashIconGlow() = this.then(
    Modifier.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CleanShieldCyanGlow.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.width * 0.8f
            )
        )
    }
)
