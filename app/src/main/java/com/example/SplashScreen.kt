package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldPink
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.max

/**
 * Production-ready native Android splash screen UI for Clean Shield.
 * Matches the reference design with a 50% 50% centered radial gradient
 * (#5DE0E6 to #0078A6) and a centered pink rounded square logo placeholder.
 */
@Composable
fun CleanShieldSplashScreen(
    modifier: Modifier = Modifier,
    enableAnimation: Boolean = true,
    onComplete: () -> Unit = {}
) {
    var startAnimation by remember { mutableStateOf(!enableAnimation) }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1500L)
        onComplete()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "logo_scale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .drawBehind {
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.5f)
                val gradientRadius = max(size.width, size.height) * 0.72f
                val radialBrush = Brush.radialGradient(
                    colors = listOf(
                        CleanShieldCyan, // #5DE0E6 at center
                        CleanShieldBlue  // #0078A6 at edges
                    ),
                    center = centerOffset,
                    radius = gradientRadius
                )
                drawRect(brush = radialBrush)
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo size dynamically calibrated for mobile and tablet screens
                val logoSize = min(112.dp, maxWidth * 0.28f)
                val cornerRadius = logoSize * 0.25f

                Box(
                    modifier = Modifier
                        .testTag("logo_placeholder")
                        .size(logoSize)
                        .scale(scaleAnim)
                        .alpha(alphaAnim)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(CleanShieldPink)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Clean Shield",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "Clean Shield Splash - Phone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun CleanShieldSplashScreenPreview() {
    MyApplicationTheme {
        CleanShieldSplashScreen(enableAnimation = false)
    }
}

@Preview(name = "Clean Shield Splash - Tablet", showBackground = true, widthDp = 800, heightDp = 1280)
@Composable
fun CleanShieldSplashScreenTabletPreview() {
    MyApplicationTheme {
        CleanShieldSplashScreen(enableAnimation = false)
    }
}
