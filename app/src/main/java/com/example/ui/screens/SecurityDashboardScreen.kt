package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.ui.components.CategoryIconBox
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCardInner
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldDeepBlue
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDashboardScreen(
    initialCategory: OptimizationCategory?,
    onRescanRequested: () -> Unit,
    onLockRequested: () -> Unit,
    onSignOutRequested: () -> Unit,
    onOpenProfileRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }

    val currentSession by repository.currentSession.collectAsState()
    val username = currentSession?.username ?: "alex"

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        com.example.ui.components.LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirmDialog = false
                onSignOutRequested()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }

    // Phone Optimizer state
    var storageCleaned by remember { mutableStateOf(false) }
    var ramBoosted by remember { mutableStateOf(false) }
    var virusChecked by remember { mutableStateOf(false) }
    var currentRamUsedPercent by remember { mutableIntStateOf(54) }

    val cyanTealGradient = remember {
        Brush.horizontalGradient(listOf(CleanShieldCyanBright, CleanShieldBlue))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("security_dashboard_screen"),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyanTealGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clean Shield",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active Protection",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenProfileRequested,
                            modifier = Modifier.testTag("dashboard_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onLockRequested,
                            modifier = Modifier.testTag("dashboard_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showLogoutConfirmDialog = true },
                            modifier = Modifier.testTag("dashboard_signout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Social Hub Quick Navigation Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenProfileRequested() },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = CleanShieldBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Welcome, @$username",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldDarkNavy
                            )
                            Text(
                                text = "Tap to open Profile, Friends & Vault",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Button(
                        onClick = onOpenProfileRequested,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Open Hub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // System Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                color = Color.Gray
                            )
                            Text(
                                text = "100% Protected",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldGreen
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
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }

            // Action 1: Storage Cleaner
            OptimizerActionRow(
                icon = Icons.Default.CleaningServices,
                title = "Storage Cleaner",
                subtitle = if (storageCleaned) "Cleaned — 0 B residual cache" else "1.4 GB residual cache files ready to clean",
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFFDCFCE7) else Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isCompleted) CleanShieldGreen else CleanShieldBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CleanShieldDarkNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAction,
                enabled = !isCompleted,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CleanShieldBlue,
                    disabledContainerColor = Color(0xFFDCFCE7),
                    disabledContentColor = CleanShieldGreen
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
