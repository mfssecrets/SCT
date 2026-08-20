package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldError

enum class CleanShieldTab {
    PROFILE,
    FRIENDS,
    PRIVATE_VAULT,
    SEARCH
}

@Composable
fun CleanShieldTopHeader(
    title: String,
    showBackButton: Boolean = false,
    onBackClicked: () -> Unit = {},
    onLogoutClicked: () -> Unit = {},
    onMessengerClicked: () -> Unit = {},
    onNotificationClicked: () -> Unit = {},
    unreadNotificationsCount: Int = 0,
    unreadMessagesCount: Int = 0,
    extraActionContent: @Composable (() -> Unit)? = null
) {
    val cyanTealGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(CleanShieldCyanBright, CleanShieldBlue)
        )
    }

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirmDialog = false
                onLogoutClicked()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cyanTealGradient)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Back or Logout
            if (showBackButton) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier.testTag("top_bar_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Logout",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Right Actions
            if (extraActionContent != null) {
                extraActionContent()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMessengerClicked,
                        modifier = Modifier.testTag("top_bar_messenger_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadMessagesCount > 0) {
                                    Badge(
                                        containerColor = CleanShieldError,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadMessagesCount", fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = "Messenger",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNotificationClicked,
                        modifier = Modifier.testTag("top_bar_notification_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = CleanShieldError,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadNotificationsCount", fontSize = 10.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanShieldBottomNavBar(
    selectedTab: CleanShieldTab,
    onTabSelected: (CleanShieldTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(8.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == CleanShieldTab.PROFILE,
            onClick = { onTabSelected(CleanShieldTab.PROFILE) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = {
                Text(
                    text = "Profile",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == CleanShieldTab.PROFILE) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CleanShieldBlue,
                selectedTextColor = CleanShieldBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = CleanShieldCyanBright.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("bottom_nav_profile")
        )

        NavigationBarItem(
            selected = selectedTab == CleanShieldTab.FRIENDS,
            onClick = { onTabSelected(CleanShieldTab.FRIENDS) },
            icon = { Icon(Icons.Default.Group, contentDescription = "Friends") },
            label = {
                Text(
                    text = "Friends",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == CleanShieldTab.FRIENDS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CleanShieldBlue,
                selectedTextColor = CleanShieldBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = CleanShieldCyanBright.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("bottom_nav_friends")
        )

        NavigationBarItem(
            selected = selectedTab == CleanShieldTab.PRIVATE_VAULT,
            onClick = { onTabSelected(CleanShieldTab.PRIVATE_VAULT) },
            icon = { Icon(Icons.Default.Lock, contentDescription = "Private Vault") },
            label = {
                Text(
                    text = "Private Vault",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == CleanShieldTab.PRIVATE_VAULT) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CleanShieldBlue,
                selectedTextColor = CleanShieldBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = CleanShieldCyanBright.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("bottom_nav_vault")
        )

        NavigationBarItem(
            selected = selectedTab == CleanShieldTab.SEARCH,
            onClick = { onTabSelected(CleanShieldTab.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = {
                Text(
                    text = "Search",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == CleanShieldTab.SEARCH) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CleanShieldBlue,
                selectedTextColor = CleanShieldBlue,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = CleanShieldCyanBright.copy(alpha = 0.25f)
            ),
            modifier = Modifier.testTag("bottom_nav_search")
        )
    }
}
