package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TopFadeOverlay(
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.95f),
                        color.copy(alpha = 0.85f),
                        color.copy(alpha = 0.50f),
                        color.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun BottomFadeOverlay(
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.15f),
                        color.copy(alpha = 0.50f),
                        color.copy(alpha = 0.85f),
                        color.copy(alpha = 0.95f),
                        color
                    )
                )
            )
    )
}
