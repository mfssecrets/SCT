package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.NotificationItemWithUser
import com.example.data.SocialRepository
import com.example.data.local.NotificationEntity
import com.example.data.local.UserEntity
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository.getInstance(context) }
    val socialRepo = remember { SocialRepository.getInstance(context) }

    val currentSession by authRepo.currentSession.collectAsState()
    val currentUsername = currentSession?.username ?: ""

    // Real-time unread count
    val unreadCount by socialRepo.getUnreadNotificationsCountFlow(currentUsername).collectAsState(initial = 0)

    // Flow of notifications with full sender profile metadata
    val notificationsFlow = remember(currentUsername) {
        socialRepo.getNotificationsWithSenderFlow(currentUsername)
    }
    val notificationsList by notificationsFlow.collectAsState(initial = null)

    // UI Loading / Refreshing states
    var isRefreshing by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // In-flight action trackers to prevent duplicate taps
    val processingActionIds = remember { mutableStateListOf<Long>() }

    // Dialog state for confirmation
    var pendingAcceptTarget by remember { mutableStateOf<NotificationItemWithUser?>(null) }
    var pendingRejectTarget by remember { mutableStateOf<NotificationItemWithUser?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val cyanTealGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(CleanShieldCyanBright, CleanShieldBlue)
        )
    }

    // Confirmation Dialog for Accept
    if (pendingAcceptTarget != null) {
        val target = pendingAcceptTarget!!
        val senderName = target.senderUser?.name?.ifBlank { null } ?: target.notification.senderUsername
        val senderHandle = target.notification.senderUsername

        AlertDialog(
            onDismissRequest = { pendingAcceptTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = CleanShieldBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accept Friend Request?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = CleanShieldDarkNavy
                    )
                }
            },
            text = {
                Text(
                    text = "Accept friend request from $senderName (@$senderHandle)? Both of you will be added to each other's friends list and can exchange end-to-end encrypted messages.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notif = target.notification
                        pendingAcceptTarget = null
                        scope.launch {
                            processingActionIds.add(notif.id)
                            try {
                                val success = socialRepo.acceptFriendRequest(
                                    requesterUsername = notif.senderUsername,
                                    recipientUsername = currentUsername
                                )
                                if (success) {
                                    socialRepo.markNotificationAsRead(notif.id)
                                    Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Could not accept request.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                processingActionIds.remove(notif.id)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingAcceptTarget = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation Dialog for Reject
    if (pendingRejectTarget != null) {
        val target = pendingRejectTarget!!
        val senderName = target.senderUser?.name?.ifBlank { null } ?: target.notification.senderUsername
        val senderHandle = target.notification.senderUsername

        AlertDialog(
            onDismissRequest = { pendingRejectTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = CleanShieldError,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reject Friend Request?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = CleanShieldDarkNavy
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to reject the friend request from $senderName (@$senderHandle)?",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notif = target.notification
                        pendingRejectTarget = null
                        scope.launch {
                            processingActionIds.add(notif.id)
                            try {
                                val success = socialRepo.declineFriendRequest(
                                    requesterUsername = notif.senderUsername,
                                    recipientUsername = currentUsername
                                )
                                if (success) {
                                    socialRepo.markNotificationAsRead(notif.id)
                                    Toast.makeText(context, "Friend request rejected.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Could not reject request.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                processingActionIds.remove(notif.id)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingRejectTarget = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text(
                    text = "Clear All Notifications?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CleanShieldDarkNavy
                )
            },
            text = {
                Text(
                    text = "This will clear all notifications from your notification history.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirm = false
                        scope.launch {
                            socialRepo.clearAllNotifications(currentUsername)
                            Toast.makeText(context, "Notifications cleared.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllConfirm = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("notifications_screen"),
        topBar = {
            // Fixed gradient header (#5DE0E6 -> #0078A6)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyanTealGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back icon
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Title with unread badge count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Notifications",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    color = CleanShieldBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Header Right Actions (Mark all read & Clear all)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (unreadCount > 0) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        socialRepo.markAllNotificationsAsRead(currentUsername)
                                        Toast.makeText(context, "All marked as read", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("mark_all_read_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Mark All As Read",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (notificationsList?.isNotEmpty() == true) {
                                    showClearAllConfirm = true
                                }
                            },
                            modifier = Modifier.testTag("clear_all_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Clear All",
                                tint = Color.White.copy(alpha = if (notificationsList?.isNotEmpty() == true) 1f else 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
        ) {
            when {
                // Skeleton / Loading State
                notificationsList == null || isRefreshing -> {
                    NotificationSkeletonList()
                }

                // Error State
                hasError -> {
                    NotificationErrorState(
                        onRetry = {
                            hasError = false
                            isRefreshing = true
                            scope.launch {
                                delay(600)
                                isRefreshing = false
                            }
                        }
                    )
                }

                // Empty State
                notificationsList!!.isEmpty() -> {
                    NotificationEmptyState(
                        onRefresh = {
                            isRefreshing = true
                            scope.launch {
                                delay(500)
                                isRefreshing = false
                            }
                        }
                    )
                }

                // Populated State
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 28.dp)
                    ) {
                        items(
                            items = notificationsList!!,
                            key = { it.notification.id }
                        ) { item ->
                            val isProcessing = processingActionIds.contains(item.notification.id)

                            NotificationCardRow(
                                item = item,
                                isProcessing = isProcessing,
                                onAcceptClick = {
                                    pendingAcceptTarget = item
                                },
                                onRejectClick = {
                                    pendingRejectTarget = item
                                },
                                onCardClick = {
                                    if (!item.notification.isRead) {
                                        scope.launch {
                                            socialRepo.markNotificationAsRead(item.notification.id)
                                        }
                                    }
                                },
                                onStartChatClick = {
                                    onNavigateToChat(item.notification.senderUsername)
                                },
                                onDeleteClick = {
                                    scope.launch {
                                        socialRepo.deleteNotification(item.notification.id)
                                        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardRow(
    item: NotificationItemWithUser,
    isProcessing: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
    onCardClick: () -> Unit,
    onStartChatClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val notif = item.notification
    val sender = item.senderUser
    val isFriendRequest = notif.type == "FRIEND_REQUEST" && notif.status == "PENDING"
    val isAccepted = notif.type == "REQUEST_ACCEPTED" || notif.status == "ACCEPTED"
    val isRejected = notif.type == "REQUEST_REJECTED" || notif.status == "REJECTED"
    val isSecurityAlert = notif.type == "SECURITY_ALERT"

    val senderDisplayName = sender?.name?.ifBlank { null } ?: notif.senderUsername
    val senderHandle = notif.senderUsername
    val timeAgo = remember(notif.timestamp) { formatRelativeTime(notif.timestamp) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
            .testTag("notification_card_${notif.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (notif.isRead) Color.White else Color(0xFFF0F9FF)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!notif.isRead) CleanShieldCyanBright.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notif.isRead) 1.dp else 2.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Avatar, Name/Timestamp, Read Indicator / Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CleanShieldCyanBright.copy(alpha = 0.2f), CleanShieldBlue.copy(alpha = 0.2f))
                            )
                        )
                        .border(1.5.dp, if (!notif.isRead) CleanShieldBlue else Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (sender?.profilePhotoUri != null && sender.profilePhotoUri.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Uri.parse(sender.profilePhotoUri))
                                .crossfade(true)
                                .build(),
                            contentDescription = senderDisplayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initial = senderDisplayName.firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            color = CleanShieldBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // User details & notification message
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = senderDisplayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldDarkNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@$senderHandle",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = timeAgo,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            if (!notif.isRead) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CleanShieldBlue)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notif.message,
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp
                    )
                }
            }

            // Action / Status Area
            when {
                // Pending Friend Request: Accept and Reject buttons
                isFriendRequest -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Accept Button
                        Button(
                            onClick = onAcceptClick,
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CleanShieldBlue,
                                disabledContainerColor = CleanShieldBlue.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("accept_friend_btn_${notif.id}")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Reject Button
                        OutlinedButton(
                            onClick = onRejectClick,
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CleanShieldError
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CleanShieldError.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("reject_friend_btn_${notif.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = CleanShieldError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reject", fontSize = 13.sp, color = CleanShieldError, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Accepted State
                isAccepted -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = CleanShieldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Friends Connected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }

                        TextButton(
                            onClick = onStartChatClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = CleanShieldBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Start Chat",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldBlue
                            )
                        }
                    }
                }

                // Rejected State
                isRejected -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = CleanShieldError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Friend Request Declined",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSkeletonList() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCBD5E1).copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFCBD5E1).copy(alpha = alpha))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE2E8F0).copy(alpha = alpha))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0).copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFCBD5E1).copy(alpha = alpha))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE2E8F0).copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationEmptyState(
    onRefresh: () -> Unit
) {
    val cyanTealGradient = remember {
        Brush.linearGradient(
            colors = listOf(CleanShieldCyanBright, CleanShieldBlue)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(cyanTealGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No Notifications Yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CleanShieldDarkNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "When other users send you friend requests or accept your connection, you'll find real-time updates right here.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier.testTag("empty_notifications_refresh_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun NotificationErrorState(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = CleanShieldError,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Failed to load notifications",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CleanShieldDarkNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "An error occurred while loading your notification feed. Please try again.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 45 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
