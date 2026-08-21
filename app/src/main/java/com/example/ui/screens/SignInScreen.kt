package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.data.AuthResult
import com.example.ui.components.SecurityShieldIcon
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import androidx.compose.material.icons.filled.Refresh
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onRequiresVerification: (email: String, username: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var generalError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showForgotPasswordSheet by remember { mutableStateOf(false) }

    fun handleSignIn() {
        generalError = null
        focusManager.clearFocus()

        if (identifier.isBlank()) {
            generalError = "Please enter your username or email."
            return
        }
        if (password.isBlank()) {
            generalError = "Please enter your password."
            return
        }

        isLoading = true
        scope.launch {
            val result = repository.signIn(identifier, password)
            isLoading = false
            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(context, "Welcome back, ${result.user.username}!", Toast.LENGTH_SHORT).show()
                    onSignInSuccess()
                }
                is AuthResult.RequiresVerification -> {
                    Toast.makeText(context, "Please verify your email address.", Toast.LENGTH_SHORT).show()
                    onRequiresVerification(result.email, result.username)
                }
                is AuthResult.Error -> {
                    generalError = result.message
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("sign_in_screen")
            .background(CleanShieldDeepBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("signin_back_button")
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

            // Scrollable Form
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Shield branding icon at top of form
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CleanShieldCyan.copy(alpha = 0.12f))
                        .border(1.dp, CleanShieldCyan.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = CleanShieldCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Administrator Sign In",
                    color = CleanShieldTextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter your verified credentials to access Clean Shield",
                    color = CleanShieldTextDim,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Error banner with dismiss button
                AnimatedVisibility(visible = generalError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CleanShieldAmber.copy(alpha = 0.15f))
                            .border(1.dp, CleanShieldAmber, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CleanShieldAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = generalError ?: "",
                                color = CleanShieldAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { generalError = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = CleanShieldAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 1. Username or Email Field
                Text(
                    text = "Username or Email Address",
                    color = CleanShieldTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(14.dp))
                        .testTag("signin_identifier_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("username or name@example.com", color = CleanShieldTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CleanShieldCyan
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CleanShieldCardBg,
                        unfocusedContainerColor = CleanShieldCardBg,
                        focusedBorderColor = CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder,
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Password Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password",
                        color = CleanShieldTextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Forgot Password?",
                        color = CleanShieldCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showForgotPasswordSheet = true }
                            .testTag("forgot_password_button")
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(14.dp))
                        .testTag("signin_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Enter your password", color = CleanShieldTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CleanShieldCyan
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = CleanShieldTextDim
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CleanShieldCardBg,
                        unfocusedContainerColor = CleanShieldCardBg,
                        focusedBorderColor = CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder,
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleSignIn() }
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Sign In Button – gradient (CleanShieldCyan → CleanShieldBlue)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (isLoading) CleanShieldCardBorder
                            else Brush.horizontalGradient(colors = listOf(CleanShieldCyan, CleanShieldBlue))
                        )
                        .clickable(enabled = !isLoading) { handleSignIn() }
                        .testTag("signin_submit_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = CleanShieldTextWhite,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = CleanShieldTextWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Link to Sign Up
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = CleanShieldTextDim,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Create Account",
                        color = CleanShieldCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigateToSignUp() }
                            .testTag("navigate_to_signup_link")
                    )
                }
            }
        }

        // Forgot Password Sheet
        if (showForgotPasswordSheet) {
            ForgotPasswordSheet(
                onDismiss = { showForgotPasswordSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var step by remember { mutableStateOf(1) } // 1: enter email, 2: enter OTP & new password
    var resetEmail by remember { mutableStateOf("") }
    var resetOtp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    var sheetError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isResendingCode by remember { mutableStateOf(false) }

    val latestDispatchedOtp by repository.latestDispatchedOtp.collectAsState()

    fun handleSendResetCode() {
        if (resetEmail.isBlank() || !resetEmail.contains("@")) {
            sheetError = "Please enter a valid email address."
            return
        }
        sheetError = null
        isProcessing = true
        scope.launch {
            val (success, msg) = repository.sendPasswordResetOtp(resetEmail)
            isProcessing = false
            if (success) {
                step = 2
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                sheetError = msg
            }
        }
    }

    fun handleResendCode() {
        sheetError = null
        isResendingCode = true
        scope.launch {
            val (success, msg) = repository.sendPasswordResetOtp(resetEmail)
            isResendingCode = false
            if (success) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                sheetError = msg
            }
        }
    }

    fun handleResetPassword() {
        if (resetOtp.length != 6) {
            sheetError = "Please enter the 6-digit reset code."
            return
        }
        if (newPassword.length < 6) {
            sheetError = "New password must be at least 6 characters."
            return
        }
        if (newPassword != confirmNewPassword) {
            sheetError = "Passwords do not match."
            return
        }

        sheetError = null
        isProcessing = true
        scope.launch {
            val (success, msg) = repository.verifyPasswordResetOtp(resetEmail, resetOtp, newPassword)
            isProcessing = false
            if (success) {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                onDismiss()
            } else {
                sheetError = msg
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CleanShieldCardBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (step == 1) "Reset Password" else "Enter Reset Code",
                    color = CleanShieldTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = CleanShieldTextDim
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (sheetError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CleanShieldAmber.copy(alpha = 0.15f))
                        .border(1.dp, CleanShieldAmber, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CleanShieldAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sheetError ?: "",
                            color = CleanShieldAmber,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { sheetError = null },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = CleanShieldAmber,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (step == 1) {
                Text(
                    text = "Enter your account email to receive a 6-digit password reset code.",
                    color = CleanShieldTextDim,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    placeholder = { Text("name@example.com", color = CleanShieldTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = CleanShieldCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite,
                        focusedBorderColor = CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Send Reset Code button – gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isProcessing) CleanShieldCardBorder
                            else Brush.horizontalGradient(colors = listOf(CleanShieldCyan, CleanShieldBlue))
                        )
                        .clickable(enabled = !isProcessing) { handleSendResetCode() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CleanShieldTextWhite)
                    } else {
                        Text("Send Reset Code", fontWeight = FontWeight.Bold, color = CleanShieldTextWhite)
                    }
                }
            } else {
                Text(
                    text = "Enter the 6-digit reset code sent to $resetEmail and choose your new password.",
                    color = CleanShieldTextDim,
                    fontSize = 13.sp
                )

                if (latestDispatchedOtp != null && latestDispatchedOtp?.first == resetEmail.trim().lowercase()) {
                    val code = latestDispatchedOtp?.second ?: ""
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reset Code: $code (Auto-fill)",
                        color = CleanShieldCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { resetOtp = code }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = resetOtp,
                    onValueChange = { if (it.length <= 6) resetOtp = it },
                    placeholder = { Text("6-digit Code", color = CleanShieldTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite,
                        focusedBorderColor = CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    placeholder = { Text("New Password (min. 6 chars)", color = CleanShieldTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CleanShieldCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite,
                        focusedBorderColor = CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    placeholder = { Text("Confirm New Password", color = CleanShieldTextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CleanShieldCardBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CleanShieldCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite,
                        focusedBorderColor = CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Resend Code link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isResendingCode) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = CleanShieldCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sending...",
                            color = CleanShieldTextDim,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Didn't receive the code? ",
                            color = CleanShieldTextDim,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Resend Code",
                            color = CleanShieldCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { handleResendCode() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Update Password button – gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isProcessing) CleanShieldCardBorder
                            else Brush.horizontalGradient(colors = listOf(CleanShieldCyan, CleanShieldBlue))
                        )
                        .clickable(enabled = !isProcessing) { handleResetPassword() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CleanShieldTextWhite)
                    } else {
                        Text("Update Password", fontWeight = FontWeight.Bold, color = CleanShieldTextWhite)
                    }
                }
            }
        }
    }
}
