package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.ui.components.CleanShieldBottomNavBar
import com.example.ui.components.CleanShieldTab
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.UsernameValidationResult
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Navigation tabs now use shared CleanShieldTab from components

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutClicked: () -> Unit,
    onMessengerClicked: () -> Unit = {},
    onNotificationClicked: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val currentSession by repository.currentSession.collectAsState()
    val userId = currentSession?.id ?: 0L

    // selectedNavTab removed — CleanShieldBottomNavBar handles selection internally
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        com.example.ui.components.LogoutConfirmationDialog(
            onConfirm = {
                showLogoutConfirmDialog = false
                onLogoutClicked()
            },
            onDismiss = { showLogoutConfirmDialog = false }
        )
    }

    // Form states initialized with current session data
    var usernameInput by remember(currentSession?.username) {
        mutableStateOf(currentSession?.username ?: "")
    }
    var nameInput by remember(currentSession?.name) {
        mutableStateOf(currentSession?.name ?: "")
    }
    var bioInput by remember(currentSession?.bio) {
        mutableStateOf(currentSession?.bio ?: "")
    }
    var photoUriString by remember(currentSession?.profilePhotoUri) {
        mutableStateOf(currentSession?.profilePhotoUri)
    }

    // Validation & Loading states
    var usernameStatusText by remember { mutableStateOf<String?>(null) }
    var isUsernameValid by remember { mutableStateOf(true) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var validationJob by remember { mutableStateOf<Job?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var photoUploadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Real gradient colors
    val cyanTealGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(CleanShieldCyanBright, CleanShieldBlue)
        )
    }
    val verticalGradient = remember {
        Brush.verticalGradient(
            colors = listOf(CleanShieldCyanBright, CleanShieldBlue)
        )
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
        }
    }

    // Real-time username availability validation with debounce
    LaunchedEffect(usernameInput) {
        val trimmed = usernameInput.trim().lowercase()
        if (trimmed.isEmpty()) {
            usernameStatusText = "Username cannot be empty"
            isUsernameValid = false
            isCheckingUsername = false
            return@LaunchedEffect
        }

        // If username is unchanged from current user's, it is valid immediately
        if (currentSession != null && trimmed == currentSession?.username?.lowercase()) {
            usernameStatusText = "Current username"
            isUsernameValid = true
            isCheckingUsername = false
            return@LaunchedEffect
        }

        isCheckingUsername = true
        validationJob?.cancel()
        validationJob = scope.launch {
            delay(350) // Debounce typing
            val result = repository.validateUsernameForProfile(trimmed, userId)
            isCheckingUsername = false
            when (result) {
                is UsernameValidationResult.Valid -> {
                    usernameStatusText = "Username available"
                    isUsernameValid = true
                }
                is UsernameValidationResult.Invalid -> {
                    usernameStatusText = result.reason
                    isUsernameValid = false
                }
            }
        }
    }

    fun handleSaveProfile() {
        focusManager.clearFocus()
        errorMessage = null

        val cleanUsername = usernameInput.trim().lowercase()
        val cleanName = nameInput.trim()
        val cleanBio = bioInput.trim()

        if (cleanName.isEmpty()) {
            errorMessage = "Please enter your name."
            return
        }

        if (!isUsernameValid) {
            errorMessage = usernameStatusText ?: "Please provide a valid username."
            return
        }

        isSaving = true

        // Show upload progress if a new photo (content:// URI) was selected
        val isNewPhoto = !photoUriString.isNullOrEmpty() &&
            photoUriString.startsWith("content://") &&
            photoUriString != currentSession?.profilePhotoUri
        if (isNewPhoto) {
            isUploadingPhoto = true
            photoUploadProgress = 0.5f
        }

        scope.launch {
            val (success, error) = repository.updateUserProfile(
                userId = userId,
                username = cleanUsername,
                name = cleanName,
                bio = cleanBio,
                photoUri = photoUriString
            )
            isSaving = false
            isUploadingPhoto = false
            if (success) {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                snackbarHostState.showSnackbar("Profile saved successfully")
            } else {
                errorMessage = error ?: "Failed to save profile changes."
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Fixed Top Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cyanTealGradient)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logout Icon on Left
                    IconButton(
                        onClick = { showLogoutConfirmDialog = true },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Messenger + Notification Icons on Right
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMessengerClicked,
                            modifier = Modifier.testTag("messenger_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = "Messenger",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = onNotificationClicked,
                            modifier = Modifier.testTag("notification_button")
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
        },
        bottomBar = {
            CleanShieldBottomNavBar(
                selectedTab = CleanShieldTab.PROFILE,
                onTabSelected = { tab ->
                    when (tab) {
                        CleanShieldTab.PROFILE -> { /* already on profile */ }
                        CleanShieldTab.FRIENDS -> onNavigateToFriends()
                        CleanShieldTab.PRIVATE_VAULT -> onNavigateToVault()
                        CleanShieldTab.SEARCH -> onNavigateToSearch()
                    }
                }
            )
        }
    ) { paddingValues ->
        // Clean White Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Profile Photo: Round (300x300 px target / 140dp round) editable/uploadable
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F7FA))
                        .border(3.dp, cyanTealGradient, CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .testTag("profile_photo_container"),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUriString.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUriString)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        // Fallback initials or default avatar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (nameInput.isNotBlank()) nameInput.take(1).uppercase()
                                else if (usernameInput.isNotBlank()) usernameInput.take(1).uppercase()
                                else "C",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldBlue
                            )
                        }
                    }

                    // Edit / Camera Badge Overlay at bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EDIT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Upload progress overlay
                    if (isUploadingPhoto) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    progress = { photoUploadProgress },
                                    color = CleanShieldCyan,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${(photoUploadProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isUploadingPhoto) "Uploading photo..." else "Tap photo to upload or change",
                    color = if (isUploadingPhoto) CleanShieldCyan else Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Banner if present
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CleanShieldError.copy(alpha = 0.1f))
                            .border(1.dp, CleanShieldError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = CleanShieldError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = CleanShieldError,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 1. Username Field (Instagram-style format with real-time validation)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Username",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanShieldDarkNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { input ->
                            // Enforce lowercase and instagram format characters immediately
                            val filtered = input.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }
                            usernameInput = filtered
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        placeholder = { Text("username (e.g. alex_shield)", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = {
                            Text(
                                text = "@",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanShieldBlue,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            )
                        },
                        trailingIcon = {
                            if (isCheckingUsername) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CleanShieldBlue
                                )
                            } else if (usernameInput.isNotBlank()) {
                                if (isUsernameValid) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid Username",
                                        tint = CleanShieldGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Invalid Username",
                                        tint = CleanShieldError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isUsernameValid) CleanShieldBlue else CleanShieldError,
                            unfocusedBorderColor = if (isUsernameValid) Color(0xFFD0D7DE) else CleanShieldError,
                            focusedTextColor = CleanShieldDarkNavy,
                            unfocusedTextColor = CleanShieldDarkNavy,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Real-time username status indicator
                    if (!usernameStatusText.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.padding(start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = usernameStatusText ?: "",
                                fontSize = 12.sp,
                                color = if (isUsernameValid) CleanShieldGreen else CleanShieldError,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Name Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CleanShieldDarkNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        placeholder = { Text("Your full name or display name", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = CleanShieldBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanShieldBlue,
                            unfocusedBorderColor = Color(0xFFD0D7DE),
                            focusedTextColor = CleanShieldDarkNavy,
                            unfocusedTextColor = CleanShieldDarkNavy,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Bio Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CleanShieldDarkNavy
                        )
                        Text(
                            text = "${bioInput.length} / 150",
                            fontSize = 12.sp,
                            color = if (bioInput.length <= 150) Color.Gray else CleanShieldError
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { if (it.length <= 150) bioInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("bio_input"),
                        placeholder = { Text("Write a short bio about yourself...", color = Color.Gray, fontSize = 14.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CleanShieldBlue,
                            unfocusedBorderColor = Color(0xFFD0D7DE),
                            focusedTextColor = CleanShieldDarkNavy,
                            unfocusedTextColor = CleanShieldDarkNavy,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Save Button with Cyan/Teal Gradient
                Button(
                    onClick = { handleSaveProfile() },
                    enabled = !isSaving && isUsernameValid && nameInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color(0xFFB0BEC5)
                    ),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .testTag("save_profile_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (!isSaving && isUsernameValid && nameInput.isNotBlank()) cyanTealGradient
                                else Brush.horizontalGradient(listOf(Color(0xFFB0BEC5), Color(0xFF90A4AE)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
