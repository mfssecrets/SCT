package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.SocialRepository
import com.example.data.VaultRepository
import com.example.data.SupabaseVaultMedia
import com.example.ui.components.CleanShieldBottomNavBar
import com.example.ui.components.CleanShieldTab
import com.example.ui.components.CleanShieldTopHeader
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VaultSetupStep {
    CHECKING,
    CREATE_PIN,
    CONFIRM_PIN,
    ENTER_PIN,
    UNLOCKED
}

@Composable
fun PrivateVaultScreen(
    onNavigateToTab: (CleanShieldTab) -> Unit,
    onLogoutClicked: () -> Unit,
    onMessengerClicked: () -> Unit,
    onNotificationClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository.getInstance(context) }
    val vaultRepo = remember { VaultRepository.getInstance(context) }
    val socialRepo = remember { SocialRepository.getInstance(context) }

    val currentSession by authRepo.currentSession.collectAsState()
    val currentUsername = currentSession?.username ?: ""
    val unreadNotifsCount by socialRepo.getUnreadNotificationsCountFlow(currentUsername).collectAsState(initial = 0)

    val isUnlocked by vaultRepo.isVaultUnlocked.collectAsState()
    val vaultMediaList by vaultRepo.getVaultMediaFlow(currentUsername).collectAsState(initial = emptyList())

    // Pin UI State
    var setupStep by remember { mutableStateOf(VaultSetupStep.CHECKING) }
    var enteredPin by remember { mutableStateOf("") }
    var firstPinDraft by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isCheckingPin by remember { mutableStateOf(false) }

    // Upload & Media viewing state
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var viewingMedia by remember { mutableStateOf<SupabaseVaultMedia?>(null) }
    var mediaToDelete by remember { mutableStateOf<SupabaseVaultMedia?>(null) }

    val cyanTealGradient = remember {
        Brush.horizontalGradient(listOf(CleanShieldCyanBright, CleanShieldBlue))
    }

    // Auto-lock vault on disposal/screen exit
    DisposableEffect(Unit) {
        onDispose {
            vaultRepo.lockVault()
        }
    }

    // Determine initial PIN setup state
    LaunchedEffect(currentUsername, isUnlocked) {
        if (isUnlocked) {
            setupStep = VaultSetupStep.UNLOCKED
            return@LaunchedEffect
        }
        val hasPin = vaultRepo.hasVaultPin(currentUsername)
        setupStep = if (hasPin) VaultSetupStep.ENTER_PIN else VaultSetupStep.CREATE_PIN
    }

    // Activity result launcher for Photos & Videos
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            uploadProgress = 0.2f
            scope.launch {
                delay(300)
                uploadProgress = 0.6f
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val mediaType = if (mimeType.startsWith("video")) "VIDEO" else "IMAGE"
                delay(300)
                uploadProgress = 0.9f
                val (success, error) = vaultRepo.saveMediaToVault(currentUsername, uri, mediaType)
                uploadProgress = 1f
                delay(150)
                isUploading = false
                if (success) {
                    Toast.makeText(context, "Encrypted & stored in Vault", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, error ?: "Upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("private_vault_screen"),
        topBar = {
            CleanShieldTopHeader(
                title = "Private Vault",
                showBackButton = false,
                onLogoutClicked = onLogoutClicked,
                onMessengerClicked = onMessengerClicked,
                onNotificationClicked = onNotificationClicked,
                unreadNotificationsCount = unreadNotifsCount,
                extraActionContent = if (setupStep == VaultSetupStep.UNLOCKED) {
                    {
                        IconButton(
                            onClick = {
                                vaultRepo.lockVault()
                                enteredPin = ""
                                setupStep = VaultSetupStep.ENTER_PIN
                            },
                            modifier = Modifier.testTag("lock_vault_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = Color.White
                            )
                        }
                    }
                } else null
            )
        },
        bottomBar = {
            CleanShieldBottomNavBar(
                selectedTab = CleanShieldTab.PRIVATE_VAULT,
                onTabSelected = onNavigateToTab
            )
        },
        floatingActionButton = {
            if (setupStep == VaultSetupStep.UNLOCKED && !isUploading) {
                FloatingActionButton(
                    onClick = { mediaPickerLauncher.launch("*/*") },
                    containerColor = CleanShieldBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("vault_upload_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Media")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            when (setupStep) {
                VaultSetupStep.CHECKING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CleanShieldBlue)
                    }
                }

                VaultSetupStep.CREATE_PIN -> {
                    VaultPinKeypadView(
                        title = "Create 4-Digit Vault PIN",
                        subtitle = "First-time setup: Set a secure 4-digit PIN to encrypt your private photos and videos.",
                        enteredPin = enteredPin,
                        error = pinError,
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                pinError = null
                                if (enteredPin.length == 4) {
                                    firstPinDraft = enteredPin
                                    enteredPin = ""
                                    setupStep = VaultSetupStep.CONFIRM_PIN
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        }
                    )
                }

                VaultSetupStep.CONFIRM_PIN -> {
                    VaultPinKeypadView(
                        title = "Confirm Vault PIN",
                        subtitle = "Re-enter the same 4-digit PIN to finalize vault encryption.",
                        enteredPin = enteredPin,
                        error = pinError,
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                pinError = null
                                if (enteredPin.length == 4) {
                                    if (enteredPin == firstPinDraft) {
                                        scope.launch {
                                            isCheckingPin = true
                                            vaultRepo.createVaultPin(currentUsername, enteredPin)
                                            isCheckingPin = false
                                            setupStep = VaultSetupStep.UNLOCKED
                                            Toast.makeText(context, "Vault PIN created securely!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        pinError = "PINs do not match. Please try again."
                                        enteredPin = ""
                                        setupStep = VaultSetupStep.CREATE_PIN
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        }
                    )
                }

                VaultSetupStep.ENTER_PIN -> {
                    VaultPinKeypadView(
                        title = "Enter Vault PIN",
                        subtitle = "Enter your 4-digit security PIN to unlock encrypted media.",
                        enteredPin = enteredPin,
                        error = pinError,
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                pinError = null
                                if (enteredPin.length == 4) {
                                    scope.launch {
                                        isCheckingPin = true
                                        val valid = vaultRepo.verifyVaultPin(currentUsername, enteredPin)
                                        isCheckingPin = false
                                        if (valid) {
                                            setupStep = VaultSetupStep.UNLOCKED
                                        } else {
                                            pinError = "Incorrect PIN. Access denied."
                                            enteredPin = ""
                                        }
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        }
                    )
                }

                VaultSetupStep.UNLOCKED -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Upload progress banner
                        AnimatedVisibility(visible = isUploading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0FDF4))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Encrypting and uploading media...", fontSize = 12.sp, color = CleanShieldBlue, fontWeight = FontWeight.SemiBold)
                                    Text("${(uploadProgress * 100).toInt()}%", fontSize = 12.sp, color = CleanShieldBlue)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = CleanShieldBlue,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        if (vaultMediaList.isEmpty()) {
                            // Empty Vault State
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF0F7FA))
                                            .border(2.dp, cyanTealGradient, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = CleanShieldBlue,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Private Vault is Empty",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanShieldDarkNavy
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Store confidential photos and videos securely. Files are encrypted with your 4-digit PIN and hidden from the device gallery.",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { mediaPickerLauncher.launch("*/*") },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CleanShieldBlue),
                                        modifier = Modifier.testTag("empty_vault_upload_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload Photo or Video")
                                    }
                                }
                            }
                        } else {
                            // Grid of Encrypted Media
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .testTag("vault_media_grid"),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(vaultMediaList, key = { it.id }) { item ->
                                    VaultMediaGridItem(
                                        media = item,
                                        onClick = { viewingMedia = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Media Viewer Fullscreen Dialog
    if (viewingMedia != null) {
        val media = viewingMedia!!
        Dialog(
            onDismissRequest = { viewingMedia = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Media preview
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.media_reference)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Private Media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (media.message_type == "VIDEO") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play Video",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Top Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewingMedia = null },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    IconButton(
                        onClick = { mediaToDelete = media },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CleanShieldError)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (mediaToDelete != null) {
        val target = mediaToDelete!!
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Delete Vault Media?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this ${if (target.message_type == "VIDEO") "video" else "photo"} from your encrypted vault? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            vaultRepo.deleteVaultMedia(target.id, target.media_reference)
                            if (viewingMedia?.id == target.id) viewingMedia = null
                            mediaToDelete = null
                            Toast.makeText(context, "Media deleted from vault.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CleanShieldError)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { mediaToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VaultMediaGridItem(
    media: SupabaseVaultMedia,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .clickable { onClick() }
            .testTag("vault_item_${media.id}"),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.media_reference)
                .crossfade(true)
                .build(),
            contentDescription = "Encrypted Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (media.message_type == "VIDEO") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun VaultPinKeypadView(
    title: String,
    subtitle: String,
    enteredPin: String,
    error: String?,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CleanShieldBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CleanShieldDarkNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Pin Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) CleanShieldBlue else Color(0xFFE2E8F0)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) CleanShieldBlue else Color(0xFF94A3B8),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = error,
                    fontSize = 13.sp,
                    color = CleanShieldError,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Numeric Keypad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val digits = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫")
            )

            for (row in digits) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(68.dp))
                        } else if (key == "⌫") {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable { onBackspace() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⌫",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanShieldDarkNavy
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                    .clickable { onDigitClick(key) }
                                    .testTag("pin_digit_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanShieldDarkNavy
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
