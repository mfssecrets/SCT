package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthRepository
import com.example.ui.components.SecurityShieldIcon
import com.example.ui.theme.CleanShieldAmber
import com.example.ui.theme.CleanShieldCardBg
import com.example.ui.theme.CleanShieldCardBorder
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldDarkButton
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldDeepBg
import com.example.ui.theme.CleanShieldDeepBlue
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldTextDim
import com.example.ui.theme.CleanShieldTextMuted
import com.example.ui.theme.CleanShieldTextWhite
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class PinSetupStep {
    CREATE_PIN,
    CONFIRM_PIN
}

@Composable
fun CreateAccessPinScreen(
    onPinCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(PinSetupStep.CREATE_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    fun handleDigitInput(digit: String) {
        errorMessage = null
        if (currentStep == PinSetupStep.CREATE_PIN) {
            if (firstPin.length < 6) {
                firstPin += digit
                if (firstPin.length == 6) {
                    val validationError = repository.validatePin(firstPin)
                    if (validationError != null) {
                        errorMessage = validationError
                        triggerShake()
                    } else {
                        // Advance to confirm step
                        currentStep = PinSetupStep.CONFIRM_PIN
                    }
                }
            }
        } else {
            if (confirmPin.length < 6) {
                confirmPin += digit
                if (confirmPin.length == 6) {
                    if (confirmPin != firstPin) {
                        errorMessage = "PINs do not match. Please re-enter."
                        triggerShake()
                        confirmPin = ""
                    } else {
                        // Save securely
                        val (success, error) = repository.saveAccessPin(confirmPin)
                        if (success) {
                            Toast.makeText(context, "6-Digit Access PIN successfully configured", Toast.LENGTH_SHORT).show()
                            onPinCreated()
                        } else {
                            errorMessage = error ?: "Failed to save PIN."
                            triggerShake()
                        }
                    }
                }
            }
        }
    }

    fun handleDelete() {
        errorMessage = null
        if (currentStep == PinSetupStep.CREATE_PIN) {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                // Back to step 1
                currentStep = PinSetupStep.CREATE_PIN
                firstPin = ""
            }
        }
    }

    val currentEnteredLength = if (currentStep == PinSetupStep.CREATE_PIN) firstPin.length else confirmPin.length

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_access_pin_screen")
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
                    onClick = {
                        if (currentStep == PinSetupStep.CONFIRM_PIN) {
                            currentStep = PinSetupStep.CREATE_PIN
                            confirmPin = ""
                            firstPin = ""
                            errorMessage = null
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.testTag("create_pin_back_button")
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

            Spacer(modifier = Modifier.height(16.dp))

            // Step Indicator Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CleanShieldCyan.copy(alpha = 0.15f))
                    .border(1.dp, CleanShieldCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (currentStep == PinSetupStep.CREATE_PIN) "Step 1 of 2: Create PIN" else "Step 2 of 2: Confirm PIN",
                    color = CleanShieldCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Instruction
            Text(
                text = if (currentStep == PinSetupStep.CREATE_PIN) "Create 6-Digit Access PIN" else "Confirm 6-Digit Access PIN",
                color = CleanShieldTextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentStep == PinSetupStep.CREATE_PIN)
                    "This PIN protects access to Clean Shield.\nIt is required every time the application is opened."
                else
                    "Re-enter the same 6 digits to confirm your security PIN.",
                color = CleanShieldTextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Display
            Row(
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < currentEnteredLength
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) CleanShieldCyan
                                else CleanShieldCardBg
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) CleanShieldCyan else CleanShieldCardBorder,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error Message Display
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (errorMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
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
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Numeric Keypad
            NumericKeypad(
                onDigitClick = { digit -> handleDigitInput(digit) },
                onDeleteClick = { handleDelete() },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun EnterAccessPinScreen(
    onPinSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { AuthRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableStateOf(0) }

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

    fun handleDigit(digit: String) {
        errorMessage = null
        if (enteredPin.length < 6) {
            enteredPin += digit
            if (enteredPin.length == 6) {
                val isValid = repository.verifyAccessPin(enteredPin)
                if (isValid) {
                    onPinSuccess()
                } else {
                    failedAttempts++
                    errorMessage = "Incorrect PIN. Please try again."
                    triggerShake()
                    enteredPin = ""
                }
            }
        }
    }

    fun handleDelete() {
        errorMessage = null
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("enter_access_pin_screen")
            .background(CleanShieldDeepBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    modifier = Modifier.testTag("enter_pin_back_button")
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

            Spacer(modifier = Modifier.height(24.dp))

            // Lock Icon Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CleanShieldCardBg)
                    .border(1.dp, CleanShieldCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = CleanShieldCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Enter Access PIN",
                color = CleanShieldTextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your 6-digit security PIN to unlock Clean Shield",
                color = CleanShieldTextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Display
            Row(
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) CleanShieldCyan
                                else CleanShieldCardBg
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) CleanShieldCyan else CleanShieldCardBorder,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error text space
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (errorMessage != null) {
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Keypad
            NumericKeypad(
                onDigitClick = { digit -> handleDigit(digit) },
                onDeleteClick = { handleDelete() },
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    when (key) {
                        "" -> {
                            Spacer(modifier = Modifier.size(68.dp))
                        }
                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(color = CleanShieldCyan)
                                    ) { onDeleteClick() }
                                    .testTag("keypad_delete"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = CleanShieldTextWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadButton(
                                text = key,
                                onClick = { onDigitClick(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(CleanShieldCardBg)
            .border(1.dp, CleanShieldCardBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = CleanShieldCyan)
            ) { onClick() }
            .testTag("keypad_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CleanShieldTextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
