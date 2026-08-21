package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldCyan
import com.example.ui.theme.CleanShieldDarkNavy
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldSurfaceBorder
import com.example.ui.theme.CleanShieldTextHint
import com.example.ui.theme.CleanShieldTextSecondary

// ═══════════════════════════════════════════════
// Branded Dialog Components
// ═══════════════════════════════════════════════

@Composable
fun CleanShieldAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    confirmColor: Color = CleanShieldBlue,
    icon: ImageVector? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.85f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ═══════════════════════════════════
                // Optional Icon with circular background
                // ═══════════════════════════════════
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CleanShieldCyan, CleanShieldBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ═══════════════════════════════════
                // Title
                // ═══════════════════════════════════
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanShieldDarkNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ═══════════════════════════════════
                // Message
                // ═══════════════════════════════════
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = CleanShieldTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ═══════════════════════════════════
                // Action Buttons
                // ═══════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dismiss Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            CleanShieldTextHint
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CleanShieldTextHint
                        )
                    ) {
                        Text(
                            text = dismissText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Confirm Button with gradient
                    Button(
                        onClick = {
                            onDismiss()
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(CleanShieldCyan, confirmColor)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Report User Dialog
// ═══════════════════════════════════════════════

private val reportReasons = listOf(
    "Sexual Content",
    "Violence/Hate Speech",
    "Spam/Scam",
    "Inappropriate Activity",
    "Other"
)

@Composable
fun CleanShieldReportDialog(
    targetUsername: String,
    onReport: (reason: String, details: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedReason by remember { mutableStateOf("") }
    var additionalDetails by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // ═══════════════════════════════════
                // Header
                // ═══════════════════════════════════
                Text(
                    text = "Report User",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanShieldDarkNavy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You are reporting @$targetUsername",
                    fontSize = 13.sp,
                    color = CleanShieldTextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ═══════════════════════════════════
                // Divider
                // ═══════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CleanShieldSurfaceBorder)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ═══════════════════════════════════
                // Reason Selection (Radio Buttons)
                // ═══════════════════════════════════
                Text(
                    text = "Select a reason",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CleanShieldDarkNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reportReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selectedReason == reason)
                                        CleanShieldError.copy(alpha = 0.08f)
                                    else
                                        Color.Transparent
                                )
                                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CleanShieldError,
                                    unselectedColor = CleanShieldSurfaceBorder
                                )
                            )
                            Text(
                                text = reason,
                                fontSize = 14.sp,
                                color = if (selectedReason == reason)
                                    CleanShieldError
                                else
                                    CleanShieldTextSecondary,
                                fontWeight = if (selectedReason == reason)
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ═══════════════════════════════════
                // Additional Details Text Field
                // ═══════════════════════════════════
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = {
                        Text(
                            text = "Additional details (optional)",
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CleanShieldError,
                        unfocusedBorderColor = CleanShieldSurfaceBorder,
                        cursorColor = CleanShieldError
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ═══════════════════════════════════
                // Submit Button
                // ═══════════════════════════════════
                Button(
                    onClick = {
                        if (selectedReason.isNotBlank()) {
                            onReport(selectedReason, additionalDetails)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        CleanShieldError,
                                        CleanShieldError.copy(alpha = 0.75f)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Report",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
