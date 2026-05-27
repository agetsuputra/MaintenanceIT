package com.example.ui.components

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.parseWatermarkedPhoto

@Composable
fun WatermarkedAsyncImage(
    photoStr: String?,
    defaultLocation: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val (url, loc, date) = parseWatermarkedPhoto(photoStr, defaultLocation)
    if (url.startsWith("http") || url.isNotEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            val decodedModel: Any = remember(url) {
                if (url.startsWith("data:image") && url.contains("base64,")) {
                    try {
                        val base64Data = url.substringAfter("base64,")
                        Base64.decode(base64Data, Base64.DEFAULT)
                    } catch (e: Exception) {
                        url
                    }
                } else {
                    url
                }
            }
            AsyncImage(
                model = decodedModel,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
            // Watermark bottom-left overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "📍 $loc \n📅 $date",
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Tidak Ada Foto Bukti", 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
