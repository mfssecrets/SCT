package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.data.OtpVerificationResult
import com.example.ui.components.SecurityShieldIcon
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun EmailVerificationScreen(
    email: String,
    username: String,
    onVerificationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var enteredOtp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    // OTP expiration timer: 5 minutes
    var otpTimerRemaining by remember { mutableIntStateOf(300) }
    val isCodeExpired = otpTimerRemaining <= 0

    LaunchedEffect(Unit) {
        while (otpTimerRemaining > 0) {
            delay(1000)
            otpTimerRemaining--
        }
    }

    // Resend cooldown timer
    var cooldownRemaining by remember { mutableIntStateOf(repository.getResendCooldownSeconds(email)) }

    LaunchedEffect(key1 = email) {
        while (true) {
            val rem = repository.getResendCooldownSeconds(email)
            cooldownRemaining = rem
            delay(1000)
        }
    }

    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-16f) at 50
                    16f at 100
                    (-12f) at 150
                    12f at 200
                    (-8f) at 250
                    8f at 300
                    0f at 400
                }
            )
        }
    }

    fun submitOtp(codeToVerify: String) {
        if (codeToVerify.length != 6) {
            errorMessage = "Please enter all 6 digits of the verification code."
            triggerShake()
            return
        }

        errorMessage = null
        isVerifying = true
        scope.launch {
            val result = repository.verifySignupOtp(email, codeToVerify)
            isVerifying = false
            when (result) {
                is OtpVerificationResult.Success -> {
                    Toast.makeText(context, "Email verified successfully! Clean Shield unlocked.", Toast.LENGTH_LONG).show()
                    onVerificationSuccess()
                }
                is OtpVerificationResult.Error -> {
                    errorMessage = result.message
                    triggerShake()
                    enteredOtp = ""
                }
            }
        }
    }

    fun handleDigit(digit: String) {
        if (enteredOtp.length < 6) {
            errorMessage = null
            val newCode = enteredOtp + digit
            enteredOtp = newCode
            if (newCode.length == 6) {
                submitOtp(newCode)
            }
        }
    }

    fun handleDelete() {
        errorMessage = null
        if (enteredOtp.isNotEmpty()) {
            enteredOtp = enteredOtp.dropLast(1)
        }
    }

    fun handleResend() {
        if (cooldownRemaining > 0) return
        scope.launch {
            val (success, message) = repository.resendOtp(email, "SIGNUP")
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            cooldownRemaining = 60
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("email_verification_screen")
            .background(CleanShieldDeepBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("verify_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CleanShieldTextWhite
                    )
                }

                Text(
                    text = "Clean Shield Security",
                    color = CleanShieldTextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                SecurityShieldIcon(modifier = Modifier.padding(end = 12.dp))
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Mail icon container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CleanShieldCyan.copy(alpha = 0.12f))
                        .border(1.dp, CleanShieldCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = CleanShieldCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Email Verification",
                    color = CleanShieldTextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "We've sent a 6-digit verification code to",
                    color = CleanShieldTextDim,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = email,
                    color = CleanShieldCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timer display
                val minutes = otpTimerRemaining / 60
                val seconds = otpTimerRemaining % 60
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        color = if (isCodeExpired) CleanShieldAmber else CleanShieldTextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Expiration message
                if (isCodeExpired) {
                    Text(
                        text = "This code has expired. Please request a new one.",
                        color = CleanShieldAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 6 OTP Digit Boxes
                Row(
                    modifier = Modifier
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val digitChar = if (i < enteredOtp.length) enteredOtp[i].toString() else ""
                        val isFocused = i == enteredOtp.length

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CleanShieldCardBg)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) CleanShieldCyan
                                    else if (digitChar.isNotEmpty()) CleanShieldGreen
                                    else CleanShieldCardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digitChar,
                                color = CleanShieldTextWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Error message
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = CleanShieldCyan,
                            strokeWidth = 2.dp
                        )
                    } else if (errorMessage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CleanShieldAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = CleanShieldAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Resend / Code Expired Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (isCodeExpired) {
                        // When expired, show prominent resend button
                        Text(
                            text = "Request a new code",
                            color = CleanShieldCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { handleResend() }
                                .testTag("resend_expired_otp_button")
                        )
                    } else if (cooldownRemaining > 0) {
                        Text(
                            text = "Didn't receive the code? ",
                            color = CleanShieldTextDim,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Resend in ${cooldownRemaining}s",
                            color = CleanShieldTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Didn't receive the code? ",
                            color = CleanShieldTextDim,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Resend Code",
                            color = CleanShieldCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { handleResend() }
                                .testTag("resend_otp_button")
                        )
                    }
                }

                // "Code expired?" shortcut — appears after 60s but before full 5min
                if (otpTimerRemaining in 1..239) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Code expired?",
                        color = CleanShieldAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = cooldownRemaining <= 0) {
                                if (cooldownRemaining <= 0) handleResend()
                            }
                            .testTag("code_expired_shortcut")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Keypad
            NumericKeypad(
                onDigitClick = { digit -> handleDigit(digit) },
                onDeleteClick = { handleDelete() },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
