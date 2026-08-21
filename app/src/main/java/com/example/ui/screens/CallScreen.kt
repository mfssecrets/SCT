package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AuthRepository
import com.example.data.SupabaseProfile
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldCyanBright
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldError
import kotlinx.coroutines.delay

enum class CallState {
    CALLING,
    RINGING,
    CONNECTED,
    ENDED,
    DECLINED,
    FAILED
}

@Composable
fun CallScreen(
    partnerUsername: String,
    isVideoCall: Boolean,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository.getInstance(context) }

    var partnerUser by remember { mutableStateOf<SupabaseProfile?>(null) }
    LaunchedEffect(partnerUsername) {
        partnerUser = authRepo.database.userDao().getUserByUsername(partnerUsername.trim().lowercase())
    }

    var callState by remember { mutableStateOf(CallState.CALLING) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(isVideoCall) }
    var isCameraEnabled by remember { mutableStateOf(isVideoCall) }
    var isFrontCamera by remember { mutableStateOf(true) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            if (isVideoCall) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (isVideoCall) {
            hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        }
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideoCall) needed.add(Manifest.permission.CAMERA)
        permissionLauncher.launch(needed.toTypedArray())
    }

    // Call state machine: CALLING (2s) -> RINGING (3s) -> CONNECTED
    LaunchedEffect(callState) {
        when (callState) {
            CallState.CALLING -> {
                delay(2000)
                callState = CallState.RINGING
            }
            CallState.RINGING -> {
                delay(3000)
                callState = CallState.CONNECTED
            }
            CallState.CONNECTED -> {
                while (true) {
                    delay(1000)
                    durationSeconds++
                }
            }
            CallState.ENDED, CallState.DECLINED, CallState.FAILED -> {
                delay(1200)
                onEndCall()
            }
        }
    }

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    // ── Animations ──
    val infiniteTransition = rememberInfiniteTransition()

    // Pulse scale for calling/ringing avatar
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Pulse ring alpha for calling/ringing state
    val pulseRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_alpha"
    )
    val pulseRingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_ring_scale"
    )

    // Rotating angle for connected state animated border
    val connectedBorderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connected_border_angle"
    )

    // End-call button glow pulse
    val endCallGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "endcall_glow_alpha"
    )

    val backgroundBrush = remember(isVideoCall) {
        if (isVideoCall) {
            Brush.verticalGradient(listOf(Color(0xFF0F172A), CleanShieldDeepBg))
        } else {
            Brush.verticalGradient(listOf(CleanShieldDarkNavy, CleanShieldDeepBg))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .testTag("call_screen")
    ) {
        // ── Coming Soon Badge (prominent, amber) ──
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = CleanShieldAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(1.5.dp, CleanShieldAmber, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🔔 ",
                    fontSize = 14.sp
                )
                Text(
                    text = "Coming Soon",
                    color = CleanShieldAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Coming Soon subtitle
        Text(
            text = "Audio & video calls will be available in the next update",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 62.dp, start = 40.dp, end = 40.dp)
        )

        // Video Preview Mock Stream (if Video Call and Camera Enabled)
        if (isVideoCall && isCameraEnabled && callState == CallState.CONNECTED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                // Partner simulated full screen stream
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(partnerUser?.profilePhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Partner Stream",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Self video preview picture-in-picture in top-right
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(width = 100.dp, height = 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF334155))
                        .border(2.dp, CleanShieldCyanBright, RoundedCornerShape(12.dp))
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFrontCamera) "Self (Front)" else "Self (Back)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top Info Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 92.dp, start = 24.dp, end = 24.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CleanShieldCyanBright,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encrypted ${if (isVideoCall) "Video" else "Audio"}",
                    color = CleanShieldCyanBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Partner Avatar (if audio call or connecting)
            if (!isVideoCall || callState != CallState.CONNECTED || !isCameraEnabled) {
                val isCallingOrRinging = callState == CallState.RINGING || callState == CallState.CALLING
                val isConnected = callState == CallState.CONNECTED

                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Pulse ring (calling/ringing state)
                    if (isCallingOrRinging) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(pulseRingScale)
                                .drawBehind {
                                    drawCircle(
                                        color = CleanShieldCyan.copy(alpha = pulseRingAlpha),
                                        radius = (size.minDimension / 2) - 3.dp.toPx(),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 2.5.dp.toPx()
                                        )
                                    )
                                }
                        )
                    }

                    // Main avatar
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(if (isCallingOrRinging) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(CleanShieldBlue.copy(alpha = 0.3f))
                            .then(
                                if (isConnected) {
                                    Modifier.drawBehind {
                                        // Animated rotating gradient border when connected
                                        val sweepAngle = connectedBorderAngle
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    CleanShieldCyan,
                                                    CleanShieldBlue,
                                                    CleanShieldCyanBright,
                                                    CleanShieldCyan
                                                ),
                                                center = Offset(size.width / 2, size.height / 2)
                                            ),
                                            startAngle = sweepAngle,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 3.dp.toPx()
                                            )
                                        )
                                    }
                                } else {
                                    Modifier.border(
                                        3.dp,
                                        Brush.horizontalGradient(
                                            listOf(CleanShieldCyanBright, CleanShieldBlue)
                                        ),
                                        CircleShape
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!partnerUser?.profilePhotoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(partnerUser!!.profilePhotoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = (partnerUser?.name ?: partnerUsername).take(1).uppercase(),
                                color = Color.White,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = partnerUser?.name?.ifBlank { "@$partnerUsername" } ?: "@$partnerUsername",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (callState) {
                    CallState.CALLING -> "Connecting..."
                    CallState.RINGING -> "Ringing..."
                    CallState.CONNECTED -> formattedDuration
                    CallState.ENDED -> "Call Ended"
                    CallState.DECLINED -> "Declined"
                    CallState.FAILED -> "Call Failed"
                },
                color = if (callState == CallState.CONNECTED) CleanShieldCyanBright else Color(0xFF94A3B8),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            // Call quality indicator when connected
            if (callState == CallState.CONNECTED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = CleanShieldCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Encrypted • Secure Connection",
                        color = CleanShieldCyan.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) CleanShieldError else Color.White.copy(alpha = 0.2f))
                        .testTag("call_mic_toggle")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Video-specific toggles
                if (isVideoCall) {
                    // Camera flip (front/back)
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("call_camera_switch")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Camera On/Off
                    IconButton(
                        onClick = { isCameraEnabled = !isCameraEnabled },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (!isCameraEnabled) CleanShieldError else Color.White.copy(alpha = 0.2f))
                            .testTag("call_camera_toggle")
                    ) {
                        Icon(
                            imageVector = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Speaker toggle
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) CleanShieldBlue else Color.White.copy(alpha = 0.2f))
                            .testTag("call_speaker_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speaker",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // End Call Button with pulsing glow
                IconButton(
                    onClick = {
                        callState = CallState.ENDED
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .drawBehind {
                            // Pulsing red glow around end-call button
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CleanShieldError.copy(alpha = endCallGlowAlpha),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.width * 0.72f
                                )
                            )
                        }
                        .clip(CircleShape)
                        .background(CleanShieldError)
                        .testTag("call_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
