package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.ChatRepository
import com.example.data.SocialRepository
import com.example.data.SupabaseMessage
import com.example.data.SupabaseProfile
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldSurfaceBorder
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextPrimary
import com.example.ui.theme.CleanShieldTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Reply Content Parser ──
private fun parseReplyContent(content: String): Triple<String, String, String>? {
    if (!content.startsWith("↩ @")) return null
    val newlineIndex = content.indexOf('\n')
    if (newlineIndex == -1) return null
    val replyLine = content.substring(0, newlineIndex)
    val actualContent = content.substring(newlineIndex + 1)
    val match = Regex("^↩ @(\\S+): \"(.*)\"$").find(replyLine) ?: return null
    return Triple(match.groupValues[1], match.groupValues[2], actualContent)
}

// ── Search Highlight Helper ──
private fun highlightText(text: String, query: String, highlightColor: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val builder = AnnotatedString.Builder()
    var start = 0
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    while (true) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index == -1) {
            builder.append(text.substring(start))
            break
        }
        builder.append(text.substring(start, index))
        builder.pushStyle(SpanStyle(background = highlightColor))
        builder.append(text.substring(index, index + query.length))
        builder.pop()
        start = index + query.length
    }
    return builder.toAnnotatedString()
}

@Composable
fun ChatScreen(
    partnerUsername: String,
    onNavigateBack: () -> Unit,
    onStartAudioCall: (String) -> Unit,
    onStartVideoCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository.getInstance(context) }
    val chatRepo = remember { ChatRepository.getInstance(context) }
    val socialRepo = remember { SocialRepository.getInstance(context) }

    val currentSession by authRepo.currentSession.collectAsState()
    val currentUsername = currentSession?.username ?: ""

    // Partner details
    var partnerUser by remember { mutableStateOf<SupabaseProfile?>(null) }
    LaunchedEffect(partnerUsername) {
        val user = authRepo.database.userDao().getUserByUsername(partnerUsername.trim().lowercase())
        partnerUser = user
    }

    // Messages flow
    val messagesList by chatRepo.getConversationMessagesFlow(currentUsername, partnerUsername)
        .collectAsState(initial = emptyList())

    // Secure chat mode flow
    val isSecureChatEnabled by chatRepo.getSecureChatModeFlow(currentUsername, partnerUsername)
        .collectAsState(initial = false)

    // Mark as seen on entry or new messages
    LaunchedEffect(messagesList.size) {
        chatRepo.markConversationSeen(partnerUsername, currentUsername)
    }

    // Apply FLAG_SECURE to Activity Window when Secure My Chat is active
    DisposableEffect(isSecureChatEnabled) {
        val activity = context as? Activity
        if (activity != null) {
            if (isSecureChatEnabled) {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {
            (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Composer states
    var textMessage by remember { mutableStateOf("") }
    var isOneShotMode by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    // Typing indicator state
    var showTypingIndicator by remember { mutableStateOf(false) }

    // Dialog & viewer states
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var viewingOneShotMessage by remember { mutableStateOf<SupabaseMessage?>(null) }
    var longPressedMessage by remember { mutableStateOf<SupabaseMessage?>(null) }
    var replyingTo by remember { mutableStateOf<SupabaseMessage?>(null) }

    // ═══ Chat Search states ═══
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchHighlightColor = remember { CleanShieldCyan.copy(alpha = 0.3f) }
    val filteredMessages = remember(messagesList, searchQuery) {
        if (searchQuery.isBlank()) messagesList
        else messagesList.filter { msg ->
            msg.content?.contains(searchQuery, ignoreCase = true) == true
        }
    }
    val matchCount = remember(messagesList, searchQuery) {
        if (searchQuery.isBlank()) 0
        else messagesList.count { msg ->
            msg.content?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val listState = rememberLazyListState()

    // Auto-dismiss long-press action bar after 4 seconds
    LaunchedEffect(longPressedMessage) {
        if (longPressedMessage != null) {
            delay(4000L)
            longPressedMessage = null
        }
    }

    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty() && !isSearchMode) {
            listState.animateScrollToItem(messagesList.size - 1)
        }
    }

    val cyanTealGradient = remember {
        Brush.horizontalGradient(listOf(CleanShieldCyanBright, CleanShieldBlue))
    }

    // Media picker launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !isSending) {
            isSending = true
            uploadProgress = 0.3f
            scope.launch {
                delay(250)
                uploadProgress = 0.7f
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isVideo = mimeType.startsWith("video")
                val mediaType = when {
                    isOneShotMode && isVideo -> "ONE_SHOT_VIDEO"
                    isOneShotMode && !isVideo -> "ONE_SHOT_IMAGE"
                    isVideo -> "VIDEO"
                    else -> "IMAGE"
                }
                delay(200)
                uploadProgress = 1f
                val (success, error) = chatRepo.sendMessage(
                    sender = currentUsername,
                    receiver = partnerUsername,
                    content = if (isOneShotMode) "🔒 One-shot media" else if (isVideo) "🎥 Video" else "📷 Photo",
                    mediaUri = uri,
                    mediaType = mediaType
                )
                isSending = false
                if (!success) {
                    Toast.makeText(context, error ?: "Message failed", Toast.LENGTH_SHORT).show()
                } else {
                    triggerTypingIndicator()
                }
            }
        }
    }

    // Helper to trigger typing indicator after successful send
    fun triggerTypingIndicator() {
        showTypingIndicator = true
        scope.launch {
            delay(2000L)
            showTypingIndicator = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("chat_screen"),
        topBar = {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyanTealGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!partnerUser?.profilePhotoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(partnerUser!!.profilePhotoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (partnerUser?.name ?: partnerUsername).take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title & Status
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = partnerUser?.name?.ifBlank { "@$partnerUsername" } ?: "@$partnerUsername",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSecureChatEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CleanShieldCyanBright,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Secure Screen Active",
                                    color = CleanShieldCyanBright,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "End-to-End Encrypted",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Audio Call
                    IconButton(
                        onClick = { onStartAudioCall(partnerUsername) },
                        modifier = Modifier.testTag("chat_audio_call_button")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = Color.White)
                    }

                    // Video Call
                    IconButton(
                        onClick = { onStartVideoCall(partnerUsername) },
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                    }

                    // Search
                    IconButton(
                        onClick = {
                            isSearchMode = !isSearchMode
                            if (!isSearchMode) searchQuery = ""
                        },
                        modifier = Modifier.testTag("chat_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Messages", tint = Color.White)
                    }

                    // 3-dot Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("chat_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isSecureChatEnabled) "Disable Secure My Chat" else "Secure My Chat")
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (isSecureChatEnabled) CleanShieldBlue else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        chatRepo.setSecureChatMode(
                                            currentUsername,
                                            partnerUsername,
                                            !isSecureChatEnabled
                                        )
                                        Toast.makeText(
                                            context,
                                            if (!isSecureChatEnabled) "Secure My Chat Enabled (Screenshots blocked)" else "Secure My Chat Disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showClearChatDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Block User", color = CleanShieldError) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = CleanShieldError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showBlockDialog = true
                                }
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
        ) {
            // Upload Progress Bar
            AnimatedVisibility(visible = isSending && uploadProgress > 0f) {
                LinearProgressIndicator(
                    progress = { uploadProgress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = CleanShieldBlue,
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            // ═══ Search Bar (animated) ═══
            AnimatedVisibility(
                visible = isSearchMode,
                enter = slideInVertically(tween(250)) { -it } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Search input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("chat_search_input"),
                            placeholder = { Text("Search messages...", fontSize = 13.sp, color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = CleanShieldBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CleanShieldBlue,
                                unfocusedBorderColor = Color(0xFFD0D7DE),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Close search button
                        IconButton(
                            onClick = {
                                isSearchMode = false
                                searchQuery = ""
                            },
                            modifier = Modifier.testTag("chat_search_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Search",
                                tint = CleanShieldTextMuted
                            )
                        }
                    }

                    // Match count
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = if (matchCount > 0) "$matchCount message${if (matchCount != 1) "s" else ""} found" else "No messages found",
                            fontSize = 12.sp,
                            color = if (matchCount > 0) CleanShieldBlue else CleanShieldTextMuted,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    // Divider
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE2E8F0))
                    )
                }
            }

            // Messages LazyColumn
            val displayMessages = if (isSearchMode && searchQuery.isNotBlank()) filteredMessages else messagesList

            if (isSearchMode && searchQuery.isNotBlank() && filteredMessages.isEmpty()) {
                // No messages found in search
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CleanShieldTextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No messages found",
                            fontSize = 14.sp,
                            color = CleanShieldTextMuted
                        )
                    }
                }
            } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Date separator helper
                val dayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                displayMessages.forEachIndexed { index, msg ->
                    val currentDate = dayFormat.format(Date(msg.sentAtMillis))
                    val prevDate = if (index > 0) dayFormat.format(Date(displayMessages[index - 1].sentAtMillis)) else null
                    if (currentDate != prevDate) {
                        item(key = "date_${msg.id}_$index") {
                            ChatDateSeparator(dateText = currentDate)
                        }
                    }
                    item(key = msg.id) {
                        val isMine = msg.sender_id.equals(currentUsername, ignoreCase = true)
                        Box {
                            ChatMessageBubble(
                                message = msg,
                                isMine = isMine,
                                searchQuery = if (isSearchMode) searchQuery else "",
                                searchHighlightColor = searchHighlightColor,
                                onLongPress = { longPressedMessage = msg },
                                onOneShotClicked = {
                                    if (!msg.one_shot_opened) {
                                        viewingOneShotMessage = msg
                                    }
                                },
                                onRetry = {
                                    if (msg.status == "FAILED") {
                                        scope.launch {
                                            val retryContent = msg.content ?: ""
                                            val isMedia = !msg.media_reference.isNullOrEmpty()
                                            val (success, error) = chatRepo.sendMessage(
                                                sender = currentUsername,
                                                receiver = partnerUsername,
                                                content = retryContent,
                                                mediaUri = null,
                                                mediaType = if (isMedia) msg.message_type else "TEXT"
                                            )
                                            if (success) {
                                                // Delete the failed message locally
                                                triggerTypingIndicator()
                                            } else {
                                                Toast.makeText(context, error ?: "Retry failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                            // ═══ Long-press action bar overlay ═══
                            AnimatedVisibility(
                                visible = longPressedMessage?.id == msg.id,
                                enter = fadeIn(tween(150)),
                                exit = fadeOut(tween(100))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                        .shadow(4.dp, RoundedCornerShape(24.dp))
                                        .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reply button
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(CleanShieldBlue)
                                            .clickable {
                                                replyingTo = msg
                                                longPressedMessage = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Reply,
                                            contentDescription = "Reply",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    // Copy button
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(CleanShieldBlue)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("message", msg.content ?: ""))
                                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                                longPressedMessage = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Typing indicator at the bottom
                item {
                    AnimatedVisibility(visible = showTypingIndicator && !isSearchMode) {
                        TypingIndicator()
                    }
                }
            }
            } // end if/else for search empty state

            // Composer Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                // ═══ Reply preview bar ═══
                AnimatedVisibility(
                    visible = replyingTo != null,
                    enter = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(150)) { it } + fadeOut(tween(150))
                ) {
                    replyingTo?.let { replyMsg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF0F4FF), RoundedCornerShape(8.dp))
                                .border(1.dp, CleanShieldBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(32.dp)
                                    .background(CleanShieldBlue, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to @${replyMsg.sender_id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanShieldBlue
                                )
                                Text(
                                    text = (replyMsg.content ?: "").take(50),
                                    fontSize = 12.sp,
                                    color = CleanShieldTextMuted,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { replyingTo = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = CleanShieldTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // One-Shot indicator banner
                if (isOneShotMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("One-Shot mode active (Media can be viewed once)", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                        IconButton(onClick = { isOneShotMode = false }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Disable", tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Picker
                    IconButton(
                        onClick = { mediaPickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("chat_media_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Gallery",
                            tint = CleanShieldBlue
                        )
                    }

                    // One-Shot Toggle
                    IconButton(
                        onClick = { isOneShotMode = !isOneShotMode },
                        modifier = Modifier.testTag("chat_oneshot_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isOneShotMode) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "One-Shot Toggle",
                            tint = if (isOneShotMode) Color(0xFFD97706) else Color.Gray
                        )
                    }

                    // Text Input
                    OutlinedTextField(
                        value = textMessage,
                        onValueChange = { textMessage = it },
                        placeholder = { Text("Encrypted message...", fontSize = 13.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanShieldBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (textMessage.isNotBlank() && !isSending) {
                                isSending = true
                                val sendingText = textMessage.trim()
                                val replyPrefix = if (replyingTo != null) {
                                    val preview = (replyingTo!!.content ?: "").take(50)
                                    "↩ @${replyingTo!!.sender_id}: \"$preview\"\n"
                                } else ""
                                textMessage = ""
                                replyingTo = null
                                scope.launch {
                                    val (success, error) = chatRepo.sendMessage(
                                        sender = currentUsername,
                                        receiver = partnerUsername,
                                        content = replyPrefix + sendingText,
                                        mediaUri = null,
                                        mediaType = "TEXT"
                                    )
                                    isSending = false
                                    if (!success) {
                                        Toast.makeText(context, error ?: "Send failed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        triggerTypingIndicator()
                                    }
                                }
                            }
                        },
                        enabled = textMessage.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (textMessage.isNotBlank() && !isSending) CleanShieldBlue else Color(0xFFE2E8F0))
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textMessage.isNotBlank() && !isSending) Color.White else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // One-Shot Media Viewer Dialog
    if (viewingOneShotMessage != null) {
        val msg = viewingOneShotMessage!!
        var countdown by remember { mutableStateOf(10) }

        LaunchedEffect(msg.id) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            // Mark as opened/burned
            chatRepo.markOneShotOpened(msg.id)
            viewingOneShotMessage = null
            Toast.makeText(context, "One-shot media burned.", Toast.LENGTH_SHORT).show()
        }

        Dialog(
            onDismissRequest = {
                scope.launch {
                    chatRepo.markOneShotOpened(msg.id)
                    viewingOneShotMessage = null
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(msg.media_reference)
                        .crossfade(true)
                        .build(),
                    contentDescription = "One-Shot Media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Timer badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CleanShieldCyanBright, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Self-destruct in ${countdown}s", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Semi-transparent overlay with countdown
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = CleanShieldCyanBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This media was viewed once  •  Self-destructing in ${countdown}s",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Clear Chat Options Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat Messages", fontWeight = FontWeight.Bold) },
            text = { Text("Choose how you would like to clear this conversation:") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                chatRepo.clearChatForMe(currentUsername, partnerUsername)
                                showClearChatDialog = false
                                Toast.makeText(context, "Chat cleared for you.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue)
                    ) {
                        Text("Clear for Me")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                chatRepo.clearChatForBoth(currentUsername, partnerUsername)
                                showClearChatDialog = false
                                Toast.makeText(context, "Chat deleted for both sides.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError)
                    ) {
                        Text("Clear for Both Sides")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block Confirmation Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block @$partnerUsername?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to block @$partnerUsername? They will not be able to message or call you.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            socialRepo.blockUser(currentUsername, partnerUsername)
                            showBlockDialog = false
                            onNavigateBack()
                            Toast.makeText(context, "Blocked @$partnerUsername", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError)
                ) {
                    Text("Block User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: SupabaseMessage,
    isMine: Boolean,
    searchQuery: String = "",
    searchHighlightColor: Color = Color.Transparent,
    onLongPress: () -> Unit = {},
    onOneShotClicked: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val timeFormatted = remember(message.sentAtMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.sentAtMillis))
    }

    val isOneShot = message.message_type.startsWith("ONE_SHOT")
    val isFailed = message.status == "FAILED"

    // WhatsApp-style bubble shape: 12dp corners, with sharp corner on the tail side
    val bubbleShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = if (isMine) 12.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 12.dp
    )

    // Gradient for sent messages, solid for received
    val sentGradient = Brush.horizontalGradient(listOf(CleanShieldCyan, CleanShieldBlue))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongPress
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = bubbleShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(bubbleShape)
                .background(
                    if (isFailed) Color(0xFFFFEBEE) else if (isMine) sentGradient else Color.White
                )
                .border(
                    width = if (isMine) 0.dp else 1.dp,
                    color = if (isFailed) CleanShieldError.copy(alpha = 0.3f) else if (isMine) Color.Transparent else CleanShieldSurfaceBorder,
                    shape = bubbleShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ── One-Shot Badge ──
            if (isOneShot) {
                // Gradient badge above the one-shot content
                val badgeGradient = Brush.horizontalGradient(
                    listOf(Color(0xFFD97706), Color(0xFFB45309))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(badgeGradient, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "One Shot",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // One-Shot Media bubble content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !message.one_shot_opened) { onOneShotClicked() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isMine) Color.White.copy(alpha = 0.2f) else Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (message.one_shot_opened) Icons.Default.Visibility else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isMine) Color.White else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (message.one_shot_opened) "One-shot Viewed" else "One-Shot Media",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMine) Color.White else CleanShieldDarkNavy
                        )
                        Text(
                            text = if (message.one_shot_opened) "Media has expired" else "Tap to view (view once)",
                            fontSize = 11.sp,
                            color = if (isMine) Color.White.copy(alpha = 0.75f) else Color.Gray
                        )
                    }
                }

                // Semi-transparent overlay for already-opened one-shots
                if (message.one_shot_opened) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = CleanShieldTextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "This media was viewed once",
                                fontSize = 10.sp,
                                color = CleanShieldTextMuted,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            } else if (!message.media_reference.isNullOrEmpty()) {
                // ── Regular Media Photo/Video ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(message.media_reference)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    )

                    // Video play button overlay
                    if (message.message_type == "VIDEO") {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // "Sent as media" label
                Text(
                    text = if (message.message_type == "VIDEO") "Sent as video" else "Sent as media",
                    fontSize = 10.sp,
                    color = if (isMine) Color.White.copy(alpha = 0.7f) else CleanShieldTextMuted,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                // ── Text Message ──
                val replyInfo = parseReplyContent(message.content ?: "")

                // Reply header
                if (replyInfo != null) {
                    val (replySender, replyPreview, _) = replyInfo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .border(
                                width = 1.dp,
                                color = if (isMine) Color.White.copy(alpha = 0.4f) else CleanShieldBlue.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(20.dp)
                                .background(
                                    if (isMine) Color.White.copy(alpha = 0.6f) else CleanShieldBlue,
                                    RoundedCornerShape(1.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "@$replySender",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMine) Color.White.copy(alpha = 0.85f) else CleanShieldBlue
                            )
                            Text(
                                text = replyPreview,
                                fontSize = 10.sp,
                                color = if (isMine) Color.White.copy(alpha = 0.65f) else CleanShieldTextMuted,
                                fontStyle = FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                val actualText = replyInfo?.third ?: (message.content ?: "")
                val displayText = if (searchQuery.isNotBlank()) {
                    highlightText(actualText, searchQuery, searchHighlightColor)
                } else {
                    AnnotatedString(actualText)
                }
                Text(
                    text = displayText,
                    fontSize = 14.sp,
                    color = if (isFailed) CleanShieldError else if (isMine) Color.White else CleanShieldTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time & Status
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 10.sp,
                    color = if (isMine) Color.White.copy(alpha = 0.75f) else CleanShieldTextMuted
                )

                if (isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.status) {
                        "SENDING" -> {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        "SENT" -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        "SEEN" -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Seen",
                                tint = CleanShieldCyanBright,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        "FAILED" -> {
                            // Retry button for failed messages
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = CleanShieldError,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRetry() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Date Separator Composable ──
@Composable
private fun ChatDateSeparator(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    CleanShieldSurfaceBorder.copy(alpha = 0.5f),
                    RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = dateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = CleanShieldTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Typing Indicator Composable ──
@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0f at 0
                1f at 300
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0f at 100
                1f at 400
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0f at 200
                1f at 500
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, CleanShieldSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "typing",
                    fontSize = 12.sp,
                    color = CleanShieldTextMuted
                )
                // Animated dots
                Box(
                    modifier = Modifier.size(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp * (0.5f + dot1 * 0.5f))
                            .background(CleanShieldTextMuted, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier.size(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp * (0.5f + dot2 * 0.5f))
                            .background(CleanShieldTextMuted, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier.size(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp * (0.5f + dot3 * 0.5f))
                            .background(CleanShieldTextMuted, CircleShape)
                    )
                }
            }
        }
    }
}