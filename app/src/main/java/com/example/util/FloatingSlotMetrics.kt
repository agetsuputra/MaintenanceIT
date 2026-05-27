package com.example.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FloatingSlotMetrics(
    val bottomOffset: Dp,
    val anchorHeight: Dp,
    val isKeyboardVisible: Boolean
)

@Composable
fun rememberFloatingSlotMetrics(
    floatingElementHeight: Dp = 56.dp,
    extraPadding: Dp = 16.dp,
    fadeArea: Dp = 16.dp
): FloatingSlotMetrics {
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    return remember(imeBottom, navBottom, floatingElementHeight, extraPadding, fadeArea) {
        derivedStateOf {
            val isKeyboardOpen = imeBottom > 0.dp
            val bottomOffset = if (isKeyboardOpen) {
                imeBottom + extraPadding
            } else {
                navBottom + extraPadding
            }
            FloatingSlotMetrics(
                bottomOffset = bottomOffset,
                anchorHeight = bottomOffset + floatingElementHeight,
                isKeyboardVisible = isKeyboardOpen
            )
        }
    }.value
}
