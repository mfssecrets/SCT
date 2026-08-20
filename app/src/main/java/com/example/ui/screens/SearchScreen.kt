package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.data.UserRelationshipStatus
import com.example.data.UserSearchResult
import com.example.ui.components.CleanShieldBottomNavBar
import com.example.ui.components.CleanShieldTab
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onNavigateToTab: (CleanShieldTab) -> Unit,
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
    val unreadNotifsCount by socialRepo.getUnreadNotificationsCountFlow(currentUsername).collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<UserSearchResult?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSendingRequest by remember { mutableStateOf(false) }

    var userToBlock by remember { mutableStateOf<UserSearchResult?>(null) }

    // Search by username ONLY with debounce
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim().lowercase().removePrefix("@")
        if (q.isBlank()) {
            searchResult = null
            hasSearched = false
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(350)
        val result = socialRepo.searchByExactUsername(currentUsername, q)
        searchResult = result
        hasSearched = true
        isSearching = false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Search",
                showBackButton = false,
                onLogoutClicked = onLogoutClicked,
                onMessengerClicked = onMessengerClicked,
                onNotificationClicked = onNotificationClicked,
                unreadNotificationsCount = unreadNotifsCount
            )
        },
        bottomBar = {
            CleanShieldBottomNavBar(
                selectedTab = CleanShieldTab.SEARCH,
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
            // Search Input Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("username_search_input"),
                    placeholder = { Text("Search by username only...", fontSize = 13.sp, color = Color.Gray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CleanShieldBlue,
                            modifier = Modifier.size(20.dp)
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

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Clean Shield allows user lookup by exact username only.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Search Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isSearching) {
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
                            Text("Searching username...", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else if (hasSearched && searchResult == null) {
                    // Empty result
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No user found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldDarkNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No account found matching \"$searchQuery\". Check username spelling.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (searchResult != null) {
                    val result = searchResult!!
                    val targetUser = result.user

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                                .testTag("search_result_card"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Profile Image
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!targetUser.profilePhotoUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(targetUser.profilePhotoUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "User Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = targetUser.name.take(1).ifBlank { targetUser.username.take(1) }.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = CleanShieldBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Name & Username
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = targetUser.name.ifBlank { targetUser.username },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanShieldDarkNavy
                                )
                                Text(
                                    text = "@${targetUser.username}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                                if (targetUser.bio.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = targetUser.bio,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Request & Block
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Friend Request Button with state
                            when (result.relationshipStatus) {
                                UserRelationshipStatus.SELF -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("This is You")
                                    }
                                }
                                UserRelationshipStatus.NOT_FRIENDS -> {
                                    Button(
                                        onClick = {
                                            if (!isSendingRequest) {
                                                isSendingRequest = true
                                                scope.launch {
                                                    val (success, msg) = socialRepo.sendFriendRequest(currentUsername, targetUser.username)
                                                    isSendingRequest = false
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    if (success) {
                                                        // Refresh relationship status
                                                        val updatedStatus = socialRepo.getRelationshipStatus(currentUsername, targetUser.username)
                                                        searchResult = searchResult?.copy(relationshipStatus = updatedStatus)
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isSendingRequest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("send_request_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue)
                                    ) {
                                        if (isSendingRequest) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Request", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                UserRelationshipStatus.REQUEST_SENT -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1f).height(44.dp).testTag("request_sent_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFFE2E8F0),
                                            disabledContentColor = Color(0xFF475569)
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Request Sent")
                                    }
                                }
                                UserRelationshipStatus.REQUEST_RECEIVED -> {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                socialRepo.acceptFriendRequest(targetUser.username, currentUsername)
                                                val updatedStatus = socialRepo.getRelationshipStatus(currentUsername, targetUser.username)
                                                searchResult = searchResult?.copy(relationshipStatus = updatedStatus)
                                                Toast.makeText(context, "Accepted friend request!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp).testTag("accept_received_request_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue)
                                    ) {
                                        Text("Accept Request", fontWeight = FontWeight.Bold)
                                    }
                                }
                                UserRelationshipStatus.FRIENDS -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1f).height(44.dp).testTag("already_friends_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFFD1FAE5),
                                            disabledContentColor = Color(0xFF065F46)
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Already Friends", fontWeight = FontWeight.Bold)
                                    }
                                }
                                UserRelationshipStatus.BLOCKED_BY_ME -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFFFEE2E2),
                                            disabledContentColor = CleanShieldError
                                        )
                                    ) {
                                        Text("Blocked")
                                    }
                                }
                                UserRelationshipStatus.BLOCKED_BY_THEM -> {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Unavailable")
                                    }
                                }
                            }

                            // Block Button (if not self and not already blocked)
                            if (result.relationshipStatus != UserRelationshipStatus.SELF &&
                                result.relationshipStatus != UserRelationshipStatus.BLOCKED_BY_ME
                            ) {
                                OutlinedButton(
                                    onClick = { userToBlock = result },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("search_block_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = CleanShieldError
                                    )
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Block", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Initial prompt state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Enter an exact username to find users and send friend requests.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Block Confirmation Dialog
    if (userToBlock != null) {
        val target = userToBlock!!.user
        AlertDialog(
            onDismissRequest = { userToBlock = null },
            title = { Text("Block @${target.username}?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to block ${target.name.ifBlank { "@" + target.username }}? They will not be able to interact with you.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            socialRepo.blockUser(currentUsername, target.username)
                            userToBlock = null
                            val updatedStatus = socialRepo.getRelationshipStatus(currentUsername, target.username)
                            searchResult = searchResult?.copy(relationshipStatus = updatedStatus)
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
}
