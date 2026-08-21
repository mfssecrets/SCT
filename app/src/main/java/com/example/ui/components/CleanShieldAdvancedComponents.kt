package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CleanShieldBlue
import com.example.ui.theme.CleanShieldError
import com.example.ui.theme.CleanShieldGreen
import com.example.ui.theme.CleanShieldSurfaceBorder
import com.example.ui.theme.CleanShieldTextPrimary
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════
// Online Status Indicator
// ═══════════════════════════════════════════════

/**
 * A small dot indicator for avatar online/offline status.
 * Green with white border when online, gray when offline.
 */
@Composable
fun OnlineStatusIndicator(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isOnline) CleanShieldGreen else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .then(
                if (isOnline) {
                    Modifier.border(2.dp, Color.White, CircleShape)
                } else {
                    Modifier
                }
            )
            .background(dotColor, CircleShape)
    )
}

// ═══════════════════════════════════════════════
// Swipeable Item Container
// ═══════════════════════════════════════════════

/**
 * A swipeable container that reveals a delete action on swipe left.
 * Wraps the provided [content] and shows a red delete button behind it.
 */
@Composable
fun CleanShieldSwipeableItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteIcon: ImageVector = Icons.Default.Delete,
    backgroundColor: Color = Color.White
) {
    val swipeWidth = 80.dp
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {
        // ═══════════════════════════════════
        // Delete action behind content
        // ═══════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CleanShieldError, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.padding(end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Delete",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = onDeleteIcon,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ═══════════════════════════════════
        // Foreground content with swipe gesture
        // ═══════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX < -swipeWidth.value * 0.4f) {
                                // Swipe threshold exceeded — trigger delete
                                onDelete()
                            }
                            // Reset position
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetX + dragAmount.x
                        // Clamp: only allow left swipe, limited to swipeWidth
                        offsetX = newOffset.coerceIn(-swipeWidth.value, 0f)
                    }
                }
        ) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════
// Animated Number Counter
// ═══════════════════════════════════════════════

/**
 * An animated counter for badge numbers with scale-up + fade effect.
 * Smoothly animates when [targetCount] changes.
 */
@Composable
fun CleanShieldAnimatedCounter(
    targetCount: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    var displayCount by remember { mutableIntStateOf(targetCount) }
    var isAnimating by remember { mutableStateOf(false) }

    // Trigger animation on count change
    if (displayCount != targetCount) {
        displayCount = targetCount
        isAnimating = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        finishedListener = { isAnimating = false },
        label = "counter_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.6f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "counter_alpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (targetCount > 99) "99+" else targetCount.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        )
    }
}

// ═══════════════════════════════════════════════
// Pill Chip (Selectable)
// ═══════════════════════════════════════════════

/**
 * A selectable pill/chip component for categories, filters, or tags.
 * Features animated background color transition and bold text when selected.
 */
@Composable
fun CleanShieldPillChip(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = CleanShieldBlue,
    unselectedColor: Color = Color.White
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 250),
        label = "chip_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else CleanShieldTextPrimary,
        animationSpec = tween(durationMillis = 250),
        label = "chip_text"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedColor else CleanShieldSurfaceBorder,
        animationSpec = tween(durationMillis = 250),
        label = "chip_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .then(
                if (!selected) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
