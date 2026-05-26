package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),      // Soft Blue
    secondary = Color(0xFFA5D6A7),    // Soft Green
    tertiary = Color(0xFF81D4FA),     // Light Blue
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color(0xFF0D47A1),
    onSecondary = Color(0xFF1B5E20),
    onTertiary = Color(0xFF006064),
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF78909C)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0288D1),     // Primary Blue
    secondary = Color(0xFF2E7D32),   // Secondary Green
    tertiary = Color(0xFF0277BD),    // Accent Blue
    background = Color(0xFFF5F7FA),  // Soft neutral background
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF263238),
    onSurface = Color(0xFF263238),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFF90A4AE)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
