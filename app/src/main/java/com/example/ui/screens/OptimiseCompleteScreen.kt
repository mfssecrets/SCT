package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ripple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryIconBox
import com.example.ui.components.GlowingScoreBadge
import com.example.ui.components.SecurityShieldIcon
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldDarkButton
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldDeepBlue
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import com.example.ui.theme.MyApplicationTheme

enum class OptimizationCategory {
    STORAGE,
    SYSTEM_BOOST,
    VIRUSES
}

@Composable
fun OptimiseCompleteScreen(
    onNavigateBack: () -> Unit,
    onOpenSecurityAccess: (OptimizationCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("optimise_complete_screen")
            .background(CleanShieldDeepBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar with back button, Title, and Shield
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CleanShieldTextWhite
                    )
                }

                Text(
                    text = "Optimise",
                    color = CleanShieldTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                SecurityShieldIcon(modifier = Modifier.padding(end = 12.dp))
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Score & Optimal Header Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Optimal",
                            color = CleanShieldTextWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Real-time protection",
                            color = CleanShieldTextDim,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CleanShieldCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Your device is in perfect condition",
                                color = CleanShieldCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "All optimisations completed",
                            color = CleanShieldTextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 100 pts Glowing Badge
                    GlowingScoreBadge(
                        score = 100,
                        unit = "pts"
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Recommended Optimisations Section
                Text(
                    text = "Recommended optimisations",
                    color = CleanShieldTextDim,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card Container for Recommended Optimisations
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CleanShieldCardBg)
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        // 1. Storage cleanup
                        OptimizationItemRow(
                            icon = Icons.Default.CleaningServices,
                            iconBg = CleanShieldGreen,
                            title = "Storage cleanup",
                            subtitle = "Clear junk files to free up space.",
                            buttonText = "Go",
                            onButtonClick = { onOpenSecurityAccess(OptimizationCategory.STORAGE) },
                            testTag = "storage_cleanup_go"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = CleanShieldCardBorder.copy(alpha = 0.5f)
                        )

                        // 2. System boost
                        OptimizationItemRow(
                            icon = Icons.Default.RocketLaunch,
                            iconBg = Color(0xFF1976D2),
                            title = "System boost",
                            subtitle = "Close background apps to make your device run faster.",
                            buttonText = "Go",
                            onButtonClick = { onOpenSecurityAccess(OptimizationCategory.SYSTEM_BOOST) },
                            testTag = "system_boost_go"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = CleanShieldCardBorder.copy(alpha = 0.5f)
                        )

                        // 3. Viruses & risks
                        OptimizationItemRow(
                            icon = Icons.Default.Warning,
                            iconBg = CleanShieldAmber,
                            title = "Viruses & risks",
                            subtitle = "Detect and remove viruses to protect your device.",
                            buttonText = "Go",
                            onButtonClick = { onOpenSecurityAccess(OptimizationCategory.VIRUSES) },
                            testTag = "viruses_risks_go"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Manual Optimisations Section
                Text(
                    text = "Manual optimisations",
                    color = CleanShieldTextDim,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card Container for Manual Optimisations
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CleanShieldCardBg)
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconBox(
                            icon = Icons.Default.Home,
                            backgroundColor = CleanShieldDeepBlue,
                            contentDescription = "Home shortcut"
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Add the “System boost”\nshortcut to Home screen",
                                color = CleanShieldTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Easily access the feature directly from the Home screen.",
                                color = CleanShieldTextDim,
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // "Add" button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(CleanShieldDarkButton)
                                .border(1.dp, CleanShieldCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = CleanShieldCyan)
                                ) {
                                    Toast.makeText(context, "System boost shortcut added to Home screen", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add",
                                color = CleanShieldCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Full-width Cyan "Done" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                Button(
                    onClick = {
                        (context as? Activity)?.finishAffinity()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("done_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanShieldCyan,
                        contentColor = CleanShieldDarkNavy
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizationItemRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                color = CleanShieldTextDim,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // "Go" Cyan Pill Button
        Button(
            onClick = onButtonClick,
            modifier = Modifier
                .width(68.dp)
                .height(34.dp)
                .testTag(testTag),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CleanShieldCyan,
                contentColor = CleanShieldDarkNavy
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(name = "Optimise Complete Screen", showBackground = true)
@Composable
fun OptimiseCompleteScreenPreview() {
    MyApplicationTheme {
        OptimiseCompleteScreen(
            onNavigateBack = {},
            onOpenSecurityAccess = {}
        )
    }
}
