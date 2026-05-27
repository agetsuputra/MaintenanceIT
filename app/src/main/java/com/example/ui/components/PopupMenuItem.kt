package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PopupMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    isDestructive: Boolean = false,
    isSelected: Boolean = false
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconColor = if (isDestructive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    } else if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
