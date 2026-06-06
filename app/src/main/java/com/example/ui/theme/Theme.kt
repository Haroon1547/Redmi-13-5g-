package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = SleekCardBg,
    onSecondary = Color.Black,
    tertiary = SleekBadgeBg,
    background = ZeroBlack,
    surface = CardDark,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekBlueAccent,
    onPrimary = Color.White,
    secondary = SleekCardTextSecondary,
    onSecondary = Color.White,
    tertiary = SleekCardTextPrimary,
    background = SleekBg,
    surface = Color.White,
    onBackground = SleekTextPrimary,
    onSurface = SleekTextPrimary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Use falsy default so the Sleek Interface's highly requested light theme becomes default
  forceDark: Boolean = false,
  content: @Composable () -> Unit,
) {
  val useDark = forceDark || darkTheme
  val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
