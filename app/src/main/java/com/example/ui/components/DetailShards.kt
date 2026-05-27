package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RowValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DetailSection(label: String, valRaw: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
fun DetailTextSection(label: String, text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )
        }
    }
}
