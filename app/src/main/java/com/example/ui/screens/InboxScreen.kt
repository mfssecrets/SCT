package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.ChatRepository
import com.example.data.InboxConversation
import com.example.ui.components.CleanShieldEmptyState
import com.example.ui.components.CleanShieldErrorView
import com.example.ui.components.CleanShieldSkeletonList
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldSurface
import com.example.ui.theme.CleanShieldTextHint
import com.example.ui.theme.CleanShieldTextPrimary
import com.example.ui.theme.CleanShieldTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository.getInstance(context) }
    val chatRepo = remember { ChatRepository.getInstance(context) }

    val currentSession by authRepo.currentSession.collectAsState()
    val currentUsername = currentSession?.username ?: ""

    val inboxList by chatRepo.getInboxFlow(currentUsername).collectAsState(initial = null)
    var hasError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("inbox_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Messenger",
                showBackButton = true,
                onBackClicked = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CleanShieldSurface)
        ) {
            if (inboxList == null) {
                // Skeleton loading state
                CleanShieldSkeletonList(itemCount = 5)
            } else if (hasError) {
                CleanShieldErrorView(
                    message = "Failed to load conversations",
                    onRetry = { hasError = false }
                )
            } else if (inboxList!!.isEmpty()) {
                // Empty state – shared component
                CleanShieldEmptyState(
                    icon = Icons.Default.QuestionAnswer,
                    title = "No Conversations Yet",
                    subtitle = "No conversations yet. Search for users and start chatting!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(inboxList!!, key = { _, conv -> conv.friend.id }) { index, conv ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                                animationSpec = tween(
                                    durationMillis = 350,
                                    delayMillis = index * 60,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = index * 60,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        ) {
                            InboxRowItem(
                                conversation = conv,
                                currentUsername = currentUsername,
                                onClick = { onNavigateToChat(conv.friend.username) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InboxRowItem(
    conversation: InboxConversation,
    currentUsername: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val friend = conversation.friend
    val lastMsg = conversation.lastMessage
    val hasUnread = conversation.unreadCount > 0

    val timeFormatted = remember(lastMsg?.timestamp) {
        if (lastMsg == null) ""
        else {
            val now = System.currentTimeMillis()
            val diff = now - lastMsg.timestamp
            val minutes = diff / (60 * 1000)
            val hours = diff / (60 * 60 * 1000)
            val date = Date(lastMsg.timestamp)
            when {
                diff < 60 * 1000 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .drawBehind {
                // Subtle left border accent for unread conversations
                if (hasUnread) {
                    drawRect(
                        color = CleanShieldCyan,
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                    )
                }
            }
            .padding(start = if (hasUnread) 0.dp else 3.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("inbox_item_${friend.username}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Friend Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CleanShieldBlue.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (!friend.profilePhotoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(friend.profilePhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = friend.name.take(1).ifBlank { friend.username.take(1) }.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = CleanShieldBlue
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = friend.name.ifBlank { "@${friend.username}" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CleanShieldTextPrimary
                )

                if (timeFormatted.isNotEmpty()) {
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = if (hasUnread) CleanShieldBlue else CleanShieldTextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sent indicator if sent by me
                    if (lastMsg != null && lastMsg.sender_id.equals(currentUsername, ignoreCase = true)) {
                        Icon(
                            imageVector = if (lastMsg.status == "SEEN") Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (lastMsg.status == "SEEN") CleanShieldBlue else CleanShieldTextHint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Message text / media preview
                    val previewText = when {
                        lastMsg == null -> "Tap to start conversation"
                        lastMsg.message_type.startsWith("ONE_SHOT") -> "🔒 One-shot media"
                        lastMsg.message_type == "IMAGE" -> "📷 Photo"
                        lastMsg.message_type == "VIDEO" -> "🎥 Video"
                        else -> lastMsg.content
                    }

                    Text(
                        text = previewText,
                        fontSize = 13.sp,
                        color = if (hasUnread) CleanShieldTextPrimary else CleanShieldTextSecondary,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unread Badge – gradient pill (CleanShieldCyan → CleanShieldBlue)
                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CleanShieldCyan, CleanShieldBlue)
                                )
                            )
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "${conversation.unreadCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}