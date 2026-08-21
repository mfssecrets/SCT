package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.SocialRepository
import com.example.data.SupabaseProfile
import com.example.ui.components.CleanShieldBottomNavBar
import com.example.ui.components.CleanShieldTab
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen(
    onNavigateToTab: (CleanShieldTab) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onLogoutClicked: () -> Unit,
    onMessengerClicked: () -> Unit,
    onNotificationClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository.getInstance(context) }
    val socialRepo = remember { SocialRepository.getInstance(context) }

    val currentSession by authRepo.currentSession.collectAsState()
    val currentUsername = currentSession?.username ?: ""

    val friendsList by socialRepo.getFriendsListFlow(currentUsername).collectAsState(initial = null)
    val unreadNotifsCount by socialRepo.getUnreadNotificationsCountFlow(currentUsername).collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }

    // Dialog states
    var userToUnfriend by remember { mutableStateOf<SupabaseProfile?>(null) }
    var userToBlock by remember { mutableStateOf<SupabaseProfile?>(null) }
    var userToReport by remember { mutableStateOf<SupabaseProfile?>(null) }

    val filteredFriends = remember(friendsList, searchQuery) {
        val list = friendsList ?: emptyList()
        if (searchQuery.isBlank()) {
            list
        } else {
            val q = searchQuery.trim().lowercase()
            list.filter { it.username.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
        }
    }

    val cyanTealGradient = remember {
        Brush.horizontalGradient(listOf(CleanShieldCyanBright, CleanShieldBlue))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("friends_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Friends",
                showBackButton = false,
                onLogoutClicked = onLogoutClicked,
                onMessengerClicked = onMessengerClicked,
                onNotificationClicked = onNotificationClicked,
                unreadNotificationsCount = unreadNotifsCount
            )
        },
        bottomBar = {
            CleanShieldBottomNavBar(
                selectedTab = CleanShieldTab.FRIENDS,
                onTabSelected = onNavigateToTab
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Search Box and Blocked Users Header Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("friends_search_input"),
                        placeholder = { Text("Search friends...", fontSize = 13.sp, color = Color.Gray) },
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
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
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

                    // Blocked Users Button
                    OutlinedButton(
                        onClick = onNavigateToBlockedUsers,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("blocked_users_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CleanShieldDarkNavy
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = CleanShieldError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Blocked", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Friends List Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (friendsList == null) {
                    // Loading / Skeleton State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = CleanShieldBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading friends...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else if (filteredFriends.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F7FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = CleanShieldBlue,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No friends match '$searchQuery'" else "No friends yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldDarkNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Check spelling or search for another username." else "Use the Search tab to find users by username and send friend requests.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredFriends, key = { it.id }) { friend ->
                            FriendRowItem(
                                friend = friend,
                                onChatClicked = { onNavigateToChat(friend.username) },
                                onUnfriendClicked = { userToUnfriend = friend },
                                onBlockClicked = { userToBlock = friend },
                                onReportClicked = { userToReport = friend }
                            )
                        }
                    }
                }
            }
        }
    }

    // Unfriend Confirmation Dialog
    if (userToUnfriend != null) {
        val target = userToUnfriend!!
        AlertDialog(
            onDismissRequest = { userToUnfriend = null },
            title = { Text("Unfriend @${target.username}?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${target.name.ifBlank { "@" + target.username }} from your friends list? You will no longer be able to message each other until a new request is accepted.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            socialRepo.unfriend(currentUsername, target.username)
                            userToUnfriend = null
                            Toast.makeText(context, "Unfriended @${target.username}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError)
                ) {
                    Text("Unfriend")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToUnfriend = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block Confirmation Dialog
    if (userToBlock != null) {
        val target = userToBlock!!
        AlertDialog(
            onDismissRequest = { userToBlock = null },
            title = { Text("Block @${target.username}?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to block ${target.name.ifBlank { "@" + target.username }}? They will not be able to message you, call you, or send friend requests. They will also be removed from your friends.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            socialRepo.blockUser(currentUsername, target.username)
                            userToBlock = null
                            Toast.makeText(context, "Blocked @${target.username}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBlock = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Confirmation Dialog
    if (userToReport != null) {
        val target = userToReport!!
        AlertDialog(
            onDismissRequest = { userToReport = null },
            title = { Text("Report @${target.username}", fontWeight = FontWeight.Bold) },
            text = { Text("Submit a confidential security report for @${target.username} for investigation.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            socialRepo.reportUser(currentUsername, target.username, "Inappropriate activity")
                            userToReport = null
                            Toast.makeText(context, "Report submitted securely.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue)
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToReport = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FriendRowItem(
    friend: SupabaseProfile,
    onChatClicked: () -> Unit,
    onUnfriendClicked: () -> Unit,
    onBlockClicked: () -> Unit,
    onReportClicked: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("friend_row_${friend.username}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Image
        Box(
            modifier = Modifier
                .size(46.dp)
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
                    contentDescription = "Friend Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = friend.name.take(1).ifBlank { friend.username.take(1) }.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CleanShieldBlue
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Username / Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name.ifBlank { friend.username },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CleanShieldDarkNavy
            )
            Text(
                text = "@${friend.username}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Chat Button
        Button(
            onClick = onChatClicked,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CleanShieldBlue,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
                .height(34.dp)
                .testTag("chat_button_${friend.username}")
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 3-dot Menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("friend_menu_button_${friend.username}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Unfriend") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onUnfriendClicked()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Block", color = CleanShieldError) },
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
                        onBlockClicked()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Report") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onReportClicked()
                    }
                )
            }
        }
    }
}
