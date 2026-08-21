package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // Dialog & viewer states
    var menuExpanded by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var viewingOneShotMessage by remember { mutableStateOf<SupabaseMessage?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
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
                }
            }
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

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messagesList, key = { it.id }) { msg ->
                    val isMine = msg.sender_id.equals(currentUsername, ignoreCase = true)
                    ChatMessageBubble(
                        message = msg,
                        isMine = isMine,
                        onOneShotClicked = {
                            if (!msg.one_shot_opened) {
                                viewingOneShotMessage = msg
                            }
                        }
                    )
                }
            }

            // Composer Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
            ) {
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
                                textMessage = ""
                                scope.launch {
                                    val (success, error) = chatRepo.sendMessage(
                                        sender = currentUsername,
                                        receiver = partnerUsername,
                                        content = sendingText,
                                        mediaUri = null,
                                        mediaType = "TEXT"
                                    )
                                    isSending = false
                                    if (!success) {
                                        Toast.makeText(context, error ?: "Send failed", Toast.LENGTH_SHORT).show()
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

@Composable
fun ChatMessageBubble(
    message: SupabaseMessage,
    isMine: Boolean,
    onOneShotClicked: () -> Unit
) {
    val context = LocalContext.current
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val isOneShot = message.message_type.startsWith("ONE_SHOT")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isMine) CleanShieldBlue else Color.White
                )
                .border(
                    width = if (isMine) 0.dp else 1.dp,
                    color = if (isMine) Color.Transparent else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(10.dp)
        ) {
            if (isOneShot) {
                // One-Shot Media bubble
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
            } else if (!message.media_reference.isNullOrEmpty()) {
                // Regular Media Photo/Video
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.3f)
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
                        modifier = Modifier.fillMaxSize()
                    )

                    if (message.message_type == "VIDEO") {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                // Text Message
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = if (isMine) Color.White else CleanShieldDarkNavy
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
                    color = if (isMine) Color.White.copy(alpha = 0.75f) else Color.Gray
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
                    }
                }
            }
        }
    }
}
