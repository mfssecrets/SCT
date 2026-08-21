package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════
// Clean Shield uses FIXED brand colors (not dynamic Material You)
// to ensure the #5DE0E6 → #0078A6 gradient is always consistent.
// ═══════════════════════════════════════════════

private val DarkColorScheme =
  darkColorScheme(
    primary = CleanShieldCyan,
    onPrimary = CleanShieldDarkNavy,
    primaryContainer = CleanShieldCyanDark,
    secondary = CleanShieldPink,
    tertiary = CleanShieldBlue,
    background = CleanShieldDeepBg,
    onBackground = CleanShieldTextWhite,
    surface = CleanShieldCardBg,
    onSurface = CleanShieldTextWhite,
    surfaceVariant = CleanShieldCardInner,
    onSurfaceVariant = CleanShieldTextDim,
    outline = CleanShieldCardBorder,
    error = CleanShieldError
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CleanShieldBlue,
    onPrimary = Color.White,
    primaryContainer = CleanShieldCyan.copy(alpha = 0.12f),
    secondary = CleanShieldPink,
    tertiary = CleanShieldCyan,
    background = CleanShieldSurface,
    onBackground = CleanShieldTextPrimary,
    surface = CleanShieldSurfaceCard,
    onSurface = CleanShieldTextPrimary,
    surfaceVariant = CleanShieldSurfaceHover,
    onSurfaceVariant = CleanShieldTextSecondary,
    outline = CleanShieldSurfaceBorder,
    error = CleanShieldError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  // dynamicColor is NEVER enabled — brand colors must be consistent
  // per spec requirement: "Every screen must use #5DE0E6, #0078A6"
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
