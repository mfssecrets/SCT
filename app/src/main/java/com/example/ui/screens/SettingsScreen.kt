package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CleanShieldAlertDialog
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDivider
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldOrange
import com.example.ui.theme.CleanShieldSurface
import com.example.ui.theme.CleanShieldSurfaceCard
import com.example.ui.theme.CleanShieldSurfaceBorder
import com.example.ui.theme.CleanShieldTextHint
import com.example.ui.theme.CleanShieldTextPrimary
import com.example.ui.theme.CleanShieldTextSecondary

// ═══════════════════════════════════════════════
// Settings Screen
// ═══════════════════════════════════════════════

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ═══════════════════════════════════════════════
    // Settings state (local toggles)
    // ═══════════════════════════════════════════════
    var biometricEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var typingIndicatorEnabled by remember { mutableStateOf(true) }
    var onlineStatusEnabled by remember { mutableStateOf(true) }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var messagePreviewEnabled by remember { mutableStateOf(true) }
    var autoDeleteEnabled by remember { mutableStateOf(false) }
    var autoDeleteDays by remember { mutableStateOf("30") }
    var darkModeEnabled by remember { mutableStateOf(false) }

    // Dialog states
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }

    // Auto-delete dialog
    if (showAutoDeleteDialog) {
        CleanShieldAlertDialog(
            title = "Auto-Delete Messages",
            message = "Messages will be automatically deleted after $autoDeleteDays days. This applies to all conversations and cannot be undone.",
            icon = Icons.Default.CleaningServices,
            onConfirm = { autoDeleteEnabled = true },
            onDismiss = { showAutoDeleteDialog = false }
        )
    }

    // Delete account dialog
    if (showDeleteAccountDialog) {
        CleanShieldAlertDialog(
            title = "Delete Account",
            message = "This action is permanent and cannot be undone. All your data, messages, and vault files will be permanently deleted.",
            confirmText = "Delete",
            confirmColor = CleanShieldError,
            icon = Icons.Default.DeleteForever,
            onConfirm = { /* Handle account deletion */ },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Settings",
                showBackButton = true,
                onBackClicked = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CleanShieldSurface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════════════
            // Section 1: Account
            // ═══════════════════════════════════════════════
            SettingsSectionHeader(title = "Account")

            SettingsCard {
                SettingsNavigationItem(
                    icon = Icons.Default.Password,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Change Password",
                    subtitle = "Update your account password"
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.MarkEmailRead,
                    iconBackgroundColor = CleanShieldCyan,
                    title = "Change Email",
                    subtitle = "Update your email address"
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.Block,
                    iconBackgroundColor = CleanShieldOrange,
                    title = "Blocked Users",
                    subtitle = "Manage blocked accounts",
                    onClick = { showBlockedUsersDialog = true }
                )
            }

            // ═══════════════════════════════════════════════
            // Section 2: Privacy & Security
            // ═══════════════════════════════════════════════
            SettingsSectionHeader(title = "Privacy & Security")

            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Biometric Lock",
                    subtitle = "Require fingerprint/face to open app",
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it }
                )
                SettingsDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Shield,
                    iconBackgroundColor = CleanShieldGreen,
                    title = "Two-Factor Authentication",
                    subtitle = "Extra layer of account security",
                    checked = twoFactorEnabled,
                    onCheckedChange = { twoFactorEnabled = it }
                )
                SettingsDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Visibility,
                    iconBackgroundColor = CleanShieldCyan,
                    title = "Read Receipts",
                    subtitle = "Let others know you've read messages",
                    checked = readReceiptsEnabled,
                    onCheckedChange = { readReceiptsEnabled = it }
                )
                SettingsDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Smartphone,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Typing Indicator",
                    subtitle = "Show when you're typing a message",
                    checked = typingIndicatorEnabled,
                    onCheckedChange = { typingIndicatorEnabled = it }
                )
                SettingsDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Info,
                    iconBackgroundColor = CleanShieldGreen,
                    title = "Online Status",
                    subtitle = "Show when you're active",
                    checked = onlineStatusEnabled,
                    onCheckedChange = { onlineStatusEnabled = it }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.Security,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "End-to-End Encryption",
                    subtitle = "All messages are encrypted (always on)",
                    trailingContent = {
                        Text(
                            text = "ON",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanShieldGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CleanShieldGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.CleaningServices,
                    iconBackgroundColor = CleanShieldOrange,
                    title = "Auto-Delete Messages",
                    subtitle = if (autoDeleteEnabled) "After $autoDeleteDays days" else "Off",
                    onClick = {
                        if (autoDeleteEnabled) {
                            autoDeleteEnabled = false
                        } else {
                            showAutoDeleteDialog = true
                        }
                    },
                    trailingContent = {
                        SettingsToggleDot(isEnabled = autoDeleteEnabled)
                    }
                )
            }

            // ═══════════════════════════════════════════════
            // Section 3: Notifications
            // ═══════════════════════════════════════════════
            SettingsSectionHeader(title = "Notifications")

            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Push Notifications",
                    subtitle = "Receive notifications for new messages",
                    checked = pushNotificationsEnabled,
                    onCheckedChange = { pushNotificationsEnabled = it }
                )
                SettingsDivider()
                SettingsToggleItem(
                    icon = Icons.Default.Visibility,
                    iconBackgroundColor = CleanShieldCyan,
                    title = "Message Preview",
                    subtitle = "Show message content in notifications",
                    checked = messagePreviewEnabled,
                    onCheckedChange = { messagePreviewEnabled = it }
                )
            }

            // ═══════════════════════════════════════════════
            // Section 4: Appearance
            // ═══════════════════════════════════════════════
            SettingsSectionHeader(title = "Appearance")

            SettingsCard {
                SettingsToggleItem(
                    icon = Icons.Default.BrightnessMedium,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Dark Mode",
                    subtitle = "Switch to dark theme",
                    checked = darkModeEnabled,
                    onCheckedChange = { darkModeEnabled = it }
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.Palette,
                    iconBackgroundColor = CleanShieldCyan,
                    title = "Chat Theme",
                    subtitle = "Customize chat appearance"
                )
            }

            // ═══════════════════════════════════════════════
            // Section 5: Storage & Data
            // ═══════════════════════════════════════════════
            SettingsSectionHeader(title = "Storage & Data")

            SettingsCard {
                SettingsNavigationItem(
                    icon = Icons.Default.Storage,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Storage Usage",
                    subtitle = "Manage app storage and cache"
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.CleaningServices,
                    iconBackgroundColor = CleanShieldOrange,
                    title = "Clear Cache",
                    subtitle = "Free up storage space"
                )
            }

            // ═══════════════════════════════════════════════
            // Section 6: About
            // ═══════════════════════════════════════════════
            SettingsSectionHeader(title = "About")

            SettingsCard {
                SettingsInfoItem(
                    icon = Icons.Default.Info,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Version",
                    value = "1.0.0"
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.Policy,
                    iconBackgroundColor = CleanShieldGreen,
                    title = "Privacy Policy"
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.Default.Description,
                    iconBackgroundColor = CleanShieldBlue,
                    title = "Terms of Service"
                )
                SettingsDivider()
                SettingsNavigationItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconBackgroundColor = CleanShieldCyan,
                    title = "Help & Support"
                )
            }

            // ═══════════════════════════════════════════════
            // Danger Zone
            // ═══════════════════════════════════════════════
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard(
                borderColor = CleanShieldError.copy(alpha = 0.3f)
            ) {
                SettingsNavigationItem(
                    icon = Icons.Default.DeleteForever,
                    iconBackgroundColor = CleanShieldError,
                    title = "Delete Account",
                    subtitle = "Permanently delete your account and data",
                    titleColor = CleanShieldError,
                    subtitleColor = CleanShieldError.copy(alpha = 0.7f),
                    onClick = { showDeleteAccountDialog = true }
                )
            }

            // ═══════════════════════════════════════════════
            // Footer branding
            // ═══════════════════════════════════════════════
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CleanShieldCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Clean Shield",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanShieldTextHint
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "·",
                    fontSize = 12.sp,
                    color = CleanShieldTextHint
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Secure by Design",
                    fontSize = 12.sp,
                    color = CleanShieldTextHint
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Settings Section Header
// ═══════════════════════════════════════════════

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = CleanShieldTextHint,
        letterSpacing = 0.5.sp,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp)
    )
}

// ═══════════════════════════════════════════════
// Settings Card Container
// ═══════════════════════════════════════════════

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CleanShieldSurfaceBorder,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CleanShieldSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════
// Settings Divider
// ═══════════════════════════════════════════════

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = CleanShieldDivider.copy(alpha = 0.6f),
        thickness = 0.5.dp
    )
}

// ═══════════════════════════════════════════════
// Settings Navigation Item (clickable row with chevron)
// ═══════════════════════════════════════════════

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    subtitle: String? = null,
    titleColor: Color = CleanShieldTextPrimary,
    subtitleColor: Color = CleanShieldTextHint,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackgroundColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBackgroundColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title + subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Trailing content or chevron
        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = CleanShieldTextHint.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════
// Settings Toggle Item (switch row)
// ═══════════════════════════════════════════════

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackgroundColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBackgroundColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title + subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = CleanShieldTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = CleanShieldTextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Toggle switch with brand colors
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CleanShieldBlue,
                uncheckedThumbColor = CleanShieldTextHint,
                uncheckedTrackColor = CleanShieldDivider
            )
        )
    }
}

// ═══════════════════════════════════════════════
// Settings Info Item (non-clickable, shows value)
// ═══════════════════════════════════════════════

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackgroundColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBackgroundColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = CleanShieldTextPrimary,
            modifier = Modifier.weight(1f)
        )

        // Value text
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = CleanShieldTextHint
        )
    }
}

// ═══════════════════════════════════════════════
// Toggle Status Dot
// ═══════════════════════════════════════════════

@Composable
private fun SettingsToggleDot(
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = if (isEnabled) CleanShieldGreen else CleanShieldDivider,
        label = "toggle_dot_color"
    )

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(dotColor)
            .border(1.5.dp, dotColor.copy(alpha = 0.3f), CircleShape)
    )
}
