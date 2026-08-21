package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldSurface
import com.example.ui.theme.CleanShieldSurfaceBorder
import com.example.ui.theme.CleanShieldTextPrimary
import com.example.ui.theme.CleanShieldTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow

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

    // Determine grid columns: 3 on phones, 4 on tablets
    val configuration = LocalConfiguration.current
    val isTablet = remember(configuration) { configuration.screenWidthDp >= 600 }
    val gridColumns = if (isTablet) GridCells.Fixed(4) else GridCells.Fixed(3)

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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CleanShieldSurface)
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
                                    trackColor = CleanShieldSurfaceBorder
                                )
                            }
                        }

                        if (vaultMediaList.isEmpty()) {
                            // Empty Vault State with improved upload drop target
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
                                            .background(CleanShieldCyan.copy(alpha = 0.12f))
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
                                        color = CleanShieldTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Store confidential photos and videos securely. Files are encrypted with your 4-digit PIN and hidden from the device gallery.",
                                        fontSize = 13.sp,
                                        color = CleanShieldTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    // Gradient dashed border drop target button
                                    VaultDropTargetButton(onClick = { mediaPickerLauncher.launch("*/*") })
                                }
                            }
                        } else {
                            // Media count badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${vaultMediaList.size} item${if (vaultMediaList.size != 1) "s" else ""}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CleanShieldTextPrimary
                                )
                                Text(
                                    text = "Encrypted & Secure",
                                    fontSize = 12.sp,
                                    color = CleanShieldTextSecondary
                                )
                            }

                            // Grid of Encrypted Media with responsive columns
                            LazyVerticalGrid(
                                columns = gridColumns,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp)
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

                                // Add Media drop target as the last grid item
                                item(span = { GridItemSpan(if (isTablet) 4 else 3) }) {
                                    VaultDropTargetButton(
                                        onClick = { mediaPickerLauncher.launch("*/*") },
                                        modifier = Modifier.padding(vertical = 8.dp)
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
                        .data(media.secure_storage_reference)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Private Media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (media.media_type == "VIDEO") {
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
            text = { Text("Are you sure you want to permanently delete this ${if (target.media_type == "VIDEO") "video" else "photo"} from your encrypted vault? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            vaultRepo.deleteVaultMedia(target.id, currentUsername, target.secure_storage_reference ?: "")
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

/**
 * Gradient dashed-border drop target button for adding media to the vault.
 */
@Composable
private fun VaultDropTargetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyanTealGradient = remember {
        Brush.horizontalGradient(listOf(CleanShieldCyan, CleanShieldBlue))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CleanShieldCyan.copy(alpha = 0.06f))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CleanShieldCyan.copy(alpha = 0.6f),
                        CleanShieldBlue.copy(alpha = 0.6f),
                        CleanShieldCyan.copy(alpha = 0.6f),
                        CleanShieldBlue.copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(16.dp),
                dashArray = floatArrayOf(12f, 8f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cyanTealGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Media",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Add Media",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CleanShieldBlue
            )
        }
    }
}

/**
 * A single media thumbnail in the vault grid, with gradient overlay and file size label.
 */
@Composable
fun VaultMediaGridItem(
    media: SupabaseVaultMedia,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val fileSizeLabel = remember(media.file_size_bytes) {
        formatFileSize(media.file_size_bytes)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(CleanShieldDarkNavy)
            .clickable { onClick() }
            .testTag("vault_item_${media.id}"),
        contentAlignment = Alignment.Center
    ) {
        // Thumbnail image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.secure_storage_reference)
                .crossfade(true)
                .build(),
            contentDescription = "Encrypted Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video play icon overlay
        if (media.media_type == "VIDEO") {
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

        // Dark gradient overlay from bottom + file size label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            if (fileSizeLabel != null) {
                Text(
                    text = fileSizeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

/**
 * PIN keypad view with pulsing glow shield icon.
 */
@Composable
fun VaultPinKeypadView(
    title: String,
    subtitle: String,
    enteredPin: String,
    error: String?,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    // Pulsing glow animation for shield icon
    val infiniteTransition = rememberInfiniteTransition(label = "shield_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_glow_scale"
    )

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

            // Shield icon with pulsing glow
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer glow circle
                Box(
                    modifier = Modifier
                        .size((84 * glowScale).dp)
                        .clip(CircleShape)
                        .background(
                            CleanShieldCyan.copy(alpha = glowAlpha * 0.18f)
                        )
                )
                // Inner glow circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            CleanShieldCyan.copy(alpha = glowAlpha * 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Solid icon background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                    CleanShieldCyanBright.copy(alpha = 0.3f),
                                    CleanShieldBlue.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CleanShieldBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CleanShieldTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = CleanShieldTextSecondary,
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
                                if (isFilled) CleanShieldBlue else CleanShieldSurfaceBorder
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) CleanShieldBlue else CleanShieldTextSecondary.copy(alpha = 0.4f),
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
                    textAlign = Alignment.Center
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
                                    color = CleanShieldTextPrimary
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(CleanShieldSurface)
                                    .border(1.dp, CleanShieldSurfaceBorder, CircleShape)
                                    .clickable { onDigitClick(key) }
                                    .testTag("pin_digit_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanShieldTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats a byte count into a human-readable string (e.g. "2.4 MB", "156 KB").
 */
private fun formatFileSize(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> {
            val kb = bytes / 1024.0
            if (kb < 100) "%.1f KB".format(kb) else "${kb.toInt()} KB"
        }
        bytes < 1024 * 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024.0)
            "%.1f MB".format(mb)
        }
        else -> {
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            "%.1f GB".format(gb)
        }
    }
}
