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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.CleanShieldErrorView
import com.example.ui.components.CleanShieldSkeletonList
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldSurfaceBorder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.components.CleanShieldAlertDialog
import com.example.ui.components.CleanShieldEmptyState
import kotlinx.coroutines.launch

@Composable
fun BlockedUsersScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository.getInstance(context) }
    val socialRepo = remember { SocialRepository.getInstance(context) }

    val currentSession by authRepo.currentSession.collectAsState()
    val currentUsername = currentSession?.username ?: ""

    val blockedUsers by socialRepo.getBlockedUsersFlow(currentUsername).collectAsState(initial = null)
    var hasError by remember { mutableStateOf(false) }
    var userToUnblock by remember { mutableStateOf<SupabaseProfile?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("blocked_users_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Blocked Users",
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
            if (blockedUsers == null) {
                CleanShieldSkeletonList(itemCount = 3)
            } else if (hasError) {
                CleanShieldErrorView(
                    message = "Failed to load blocked users",
                    onRetry = { hasError = false }
                )
            } else if (blockedUsers!!.isEmpty()) {
                CleanShieldEmptyState(
                    icon = Icons.Default.Block,
                    title = "No Blocked Accounts",
                    subtitle = "When you block someone, they will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(blockedUsers!!, key = { _, blocked -> blocked.id }) { index, blocked ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                                animationSpec = tween(
                                    durationMillis = 350,
                                    delayMillis = index * 50,
                                    easing = FastOutSlowInEasing
                                )
                            ) + fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = index * 50,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFCBD5E1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!blocked.profilePhotoUri.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(blocked.profilePhotoUri)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Profile Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = blocked.name.take(1).ifBlank { blocked.username.take(1) }.uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = blocked.name.ifBlank { blocked.username },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CleanShieldDarkNavy
                                        )
                                        Text(
                                            text = "@${blocked.username}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { userToUnblock = blocked },
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp).testTag("unblock_button_${blocked.username}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = CleanShieldBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Unblock", fontSize = 12.sp, color = CleanShieldBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (index < blockedUsers!!.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(CleanShieldSurfaceBorder)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Unblock Confirmation Dialog
    if (userToUnblock != null) {
        val target = userToUnblock!!
        CleanShieldAlertDialog(
            title = "Unblock @${target.username}?",
            message = "Are you sure you want to unblock ${target.name.ifBlank { "@" + target.username }}? They will be able to search for your profile and send you friend requests.",
            icon = Icons.Default.LockOpen,
            confirmColor = CleanShieldBlue,
            confirmText = "Unblock",
            onConfirm = {
                scope.launch {
                    socialRepo.unblockUser(currentUsername, target.username)
                    Toast.makeText(context, "Unblocked @${target.username}", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { userToUnblock = null }
        )
    }
}
