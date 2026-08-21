package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
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
                .background(Color.White)
        ) {
            if (inboxList == null) {
                // Skeleton loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CleanShieldBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading conversations...", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else if (inboxList!!.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F9FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = CleanShieldBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Conversations Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanShieldDarkNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add friends from the Search tab or start a conversation from your Friends list.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(inboxList!!, key = { it.friend.id }) { conv ->
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

@Composable
fun InboxRowItem(
    conversation: InboxConversation,
    currentUsername: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val friend = conversation.friend
    val lastMsg = conversation.lastMessage

    val timeFormatted = remember(lastMsg?.timestamp) {
        if (lastMsg == null) ""
        else {
            val date = Date(lastMsg.timestamp)
            val now = System.currentTimeMillis()
            val diff = now - lastMsg.timestamp
            if (diff < 24 * 60 * 60 * 1000) {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            } else {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("inbox_item_${friend.username}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Friend Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0F2FE)),
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
                    color = CleanShieldDarkNavy
                )

                if (timeFormatted.isNotEmpty()) {
                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = Color.Gray
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
                            tint = if (lastMsg.status == "SEEN") CleanShieldBlue else Color.Gray,
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
                        color = if (conversation.unreadCount > 0) CleanShieldDarkNavy else Color.Gray,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unread Badge
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(
                        containerColor = CleanShieldBlue,
                        contentColor = Color.White,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Text("${conversation.unreadCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
