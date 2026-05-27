package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object AdaptiveColors {

    @Composable
    @ReadOnlyComposable
    fun cardColorMedium(
        bg: Color = MaterialTheme.colorScheme.background,
        isDark: Boolean = isSystemInDarkTheme()
    ): Color {
        return if (isDark) {
            lerp(bg, Color.Black, 0.15f)
        } else {
            lerp(bg, Color.Black, 0.05f)
        }
    }

    @Composable
    @ReadOnlyComposable
    fun cardColorAccent(
        bg: Color = MaterialTheme.colorScheme.background,
        isDark: Boolean = isSystemInDarkTheme()
    ): Color {
        return if (isDark) {
            lerp(bg, Color.Black, 0.25f)
        } else {
            lerp(bg, Color.Black, 0.07f)
        }
    }

    @Composable
    @ReadOnlyComposable
    fun cardBorderColor(
        cardColor: Color,
        isDark: Boolean = isSystemInDarkTheme()
    ): Color {
        return if (isDark) {
            lerp(cardColor, Color.Black, 0.15f)
        } else {
            lerp(cardColor, Color.Black, 0.12f)
        }
    }
}
