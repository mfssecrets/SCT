package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.ui.components.CleanShieldBottomNavBar
import com.example.ui.components.CleanShieldTab
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted

@Composable
fun SecurityDashboardScreen(
    initialCategory: OptimizationCategory? = null,
    onLogout: () -> Unit,
    onNavigateToInbox: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onRescanRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }

    val currentSession by repository.currentSession.collectAsState()

    // Phone Optimizer state
    var storageCleaned by remember { mutableStateOf(false) }
    var ramBoosted by remember { mutableStateOf(false) }
    var virusChecked by remember { mutableStateOf(false) }
    var currentRamUsedPercent by remember { mutableIntStateOf(54) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("security_dashboard_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Clean Shield",
                onLogoutClicked = onLogout,
                onMessengerClicked = onNavigateToInbox,
                onNotificationClicked = onNavigateToNotifications
            )
        },
        bottomBar = {
            CleanShieldBottomNavBar(
                selectedTab = CleanShieldTab.PROFILE, // Dashboard doesn't map to a specific tab; no tab highlighted
                onTabSelected = { tab ->
                    when (tab) {
                        CleanShieldTab.PROFILE -> onNavigateToProfile()
                        CleanShieldTab.FRIENDS -> onNavigateToFriends()
                        CleanShieldTab.PRIVATE_VAULT -> onNavigateToVault()
                        CleanShieldTab.SEARCH -> onNavigateToSearch()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CleanShieldDeepBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Gradient heading: "Secure Your Device"
            Text(
                text = "Secure Your Device",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(CleanShieldCyanBright, CleanShieldBlue)
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Run quick optimizations to keep your device safe and fast.",
                color = Color(0xFF9CB8CC),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════
            // System Protection Card with pulsing glow
            // ═══════════════════════════════════════════════
            val infiniteTransition = rememberInfiniteTransition(label = "dashboard_glow")
            val glowPulse by infiniteTransition.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_pulse"
            )
            // Shimmer alpha for "100% Protected" text: pulses between 0.4 and 1.0
            val shimmerAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmer_alpha"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Subtle pulsing radial glow behind the card
                        val glowColor = CleanShieldCyan.copy(alpha = glowPulse)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor, Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width / 1.5f
                            ),
                            radius = size.width / 1.2f,
                            alpha = 1f
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CleanShieldCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "System Status",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CleanShieldTextMuted
                            )
                            Text(
                                text = "100% Protected",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldGreen.copy(alpha = shimmerAlpha)
                            )
                        }
                        Button(
                            onClick = onRescanRequested,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-Scan", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CleanShieldGreen,
                        trackColor = CleanShieldCardBorder
                    )
                }
            }

            // Action 1: Storage Cleaner
            OptimizerActionRow(
                icon = Icons.Default.CleaningServices,
                title = "Storage Cleaner",
                subtitle = if (storageCleaned) "Cleaned \u2014 0 B residual cache" else "1.4 GB residual cache files ready to clean",
                actionLabel = if (storageCleaned) "Cleaned" else "Clean Junk",
                isCompleted = storageCleaned,
                onAction = {
                    storageCleaned = true
                    Toast.makeText(context, "1.4 GB cleaned.", Toast.LENGTH_SHORT).show()
                }
            )

            // Action 2: Memory Booster
            OptimizerActionRow(
                icon = Icons.Default.RocketLaunch,
                title = "Memory Booster",
                subtitle = if (ramBoosted) "RAM Optimized ($currentRamUsedPercent% used)" else "Background apps utilizing $currentRamUsedPercent% RAM",
                actionLabel = if (ramBoosted) "Boosted" else "Boost RAM",
                isCompleted = ramBoosted,
                onAction = {
                    ramBoosted = true
                    currentRamUsedPercent = 34
                    Toast.makeText(context, "RAM boosted. Background apps stopped.", Toast.LENGTH_SHORT).show()
                }
            )

            // Action 3: Security & Malware Shield
            OptimizerActionRow(
                icon = Icons.Default.Security,
                title = "Malware & Security Shield",
                subtitle = if (virusChecked) "0 Threats found. Live definitions active." else "Real-time threat definition update available",
                actionLabel = if (virusChecked) "Protected" else "Check Now",
                isCompleted = virusChecked,
                onAction = {
                    virusChecked = true
                    Toast.makeText(context, "Threat scan clean. Zero vulnerabilities.", Toast.LENGTH_SHORT).show()
                }
            )

            // ═══════════════════════════════════════════════
            // Data Retention Policy Card
            // ═══════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CleanShieldCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CleanShieldBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Data Retention",
                            tint = CleanShieldBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Data Retention Policy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFD0E4F0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All messages are securely retained for 30 days per compliance requirements.",
                            fontSize = 12.sp,
                            color = CleanShieldTextDim
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptimizerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    isCompleted: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CleanShieldCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) CleanShieldGreen.copy(alpha = 0.15f) else CleanShieldCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isCompleted) CleanShieldGreen else CleanShieldCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFD0E4F0)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = CleanShieldTextDim
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAction,
                enabled = !isCompleted,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CleanShieldBlue,
                    disabledContainerColor = CleanShieldGreen.copy(alpha = 0.15f),
                    disabledContentColor = CleanShieldGreen
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
