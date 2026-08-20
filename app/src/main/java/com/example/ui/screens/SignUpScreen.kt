package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.data.AuthResult
import com.example.data.UsernameValidationResult
import com.example.ui.components.SecurityShieldIcon
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCardInner
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onSignUpSuccess: (email: String, username: String) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var usernameStatus by remember { mutableStateOf<String?>(null) }
    var isUsernameValid by remember { mutableStateOf(false) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameCheckJob by remember { mutableStateOf<Job?>(null) }

    var generalError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Real-time Instagram-format username checker with debounce
    fun checkUsername(input: String) {
        usernameCheckJob?.cancel()
        val trimmed = input.trim().lowercase()
        if (trimmed.isEmpty()) {
            usernameStatus = null
            isUsernameValid = false
            return
        }

        isCheckingUsername = true
        usernameCheckJob = scope.launch {
            delay(300) // 300ms debounce
            val result = repository.validateUsername(trimmed)
            isCheckingUsername = false
            when (result) {
                is UsernameValidationResult.Valid -> {
                    usernameStatus = "Username is available"
                    isUsernameValid = true
                }
                is UsernameValidationResult.Invalid -> {
                    usernameStatus = result.reason
                    isUsernameValid = false
                }
            }
        }
    }

    fun handleCreateAccount() {
        generalError = null
        focusManager.clearFocus()

        val trimmedUsername = username.trim().lowercase()
        val trimmedEmail = email.trim().lowercase()

        if (!isUsernameValid) {
            generalError = usernameStatus ?: "Please choose a valid username."
            return
        }
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            generalError = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            generalError = "Password must be at least 6 characters."
            return
        }
        if (password != confirmPassword) {
            generalError = "Passwords do not match."
            return
        }

        isLoading = true
        scope.launch {
            val result = repository.createAccount(
                usernameInput = trimmedUsername,
                emailInput = trimmedEmail,
                passwordInput = password
            )
            isLoading = false
            when (result) {
                is AuthResult.RequiresVerification -> {
                    Toast.makeText(context, "Verification code sent to $trimmedEmail", Toast.LENGTH_SHORT).show()
                    onSignUpSuccess(trimmedEmail, trimmedUsername)
                }
                is AuthResult.Success -> {
                    onSignUpSuccess(trimmedEmail, trimmedUsername)
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
            .testTag("sign_up_screen")
            .background(CleanShieldDeepBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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
                    modifier = Modifier.testTag("signup_back_button")
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

                Text(
                    text = "Create Account",
                    color = CleanShieldTextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Set up your encrypted profile to unlock full device control",
                    color = CleanShieldTextDim,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error Banner
                if (generalError != null) {
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
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 1. Username Field
                Text(
                    text = "Username (Instagram format)",
                    color = CleanShieldTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { input ->
                        // Automatically lowercase and prevent spaces
                        val filtered = input.lowercase().filter { it.isLetterOrDigit() || it == '.' || it == '_' }
                        username = filtered
                        checkUsername(filtered)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_username_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("e.g. alex_shield.01", color = CleanShieldTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isUsernameValid) CleanShieldGreen else CleanShieldCyan
                        )
                    },
                    trailingIcon = {
                        if (isCheckingUsername) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CleanShieldCyan,
                                strokeWidth = 2.dp
                            )
                        } else if (username.isNotEmpty()) {
                            Icon(
                                imageVector = if (isUsernameValid) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isUsernameValid) CleanShieldGreen else CleanShieldAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CleanShieldCardBg,
                        unfocusedContainerColor = CleanShieldCardBg,
                        focusedBorderColor = if (isUsernameValid) CleanShieldGreen else CleanShieldCyan,
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

                // Username live feedback
                if (usernameStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = usernameStatus ?: "",
                        color = if (isUsernameValid) CleanShieldGreen else CleanShieldAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Email Address Field
                Text(
                    text = "Email Address",
                    color = CleanShieldTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("name@example.com", color = CleanShieldTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
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
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Password Field
                Text(
                    text = "Password (min. 6 characters)",
                    color = CleanShieldTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Enter password", color = CleanShieldTextMuted) },
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
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Confirm Password Field
                Text(
                    text = "Confirm Password",
                    color = CleanShieldTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_confirm_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Re-enter password", color = CleanShieldTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (confirmPassword.isNotEmpty() && confirmPassword == password) CleanShieldGreen else CleanShieldCyan
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = CleanShieldTextDim
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CleanShieldCardBg,
                        unfocusedContainerColor = CleanShieldCardBg,
                        focusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword == password) CleanShieldGreen else CleanShieldCyan,
                        unfocusedBorderColor = CleanShieldCardBorder,
                        focusedTextColor = CleanShieldTextWhite,
                        unfocusedTextColor = CleanShieldTextWhite
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleCreateAccount() }
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Create Account Button
                Button(
                    onClick = { handleCreateAccount() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_account_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CleanShieldCyan,
                        contentColor = CleanShieldDarkNavy
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = CleanShieldDarkNavy,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Link to Sign In
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = CleanShieldTextDim,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Sign In",
                        color = CleanShieldCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigateToSignIn() }
                            .testTag("navigate_to_signin_link")
                    )
                }
            }
        }
    }
}
