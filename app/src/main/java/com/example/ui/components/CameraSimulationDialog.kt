package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.util.MOCK_PHOTOS
import com.example.util.compressAndWatermarkBitmap
import com.example.util.loadBitmapFromUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraSimulationDialog(
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()) }

    // 1. Camera taking launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val compressedBase64 = compressAndWatermarkBitmap(
                bitmap = bitmap,
                location = "Kamera Perangkat IT Support",
                dateStr = currentDateStr
            )
            onPhotoCaptured(compressedBase64)
        }
    }

    // 2. Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                val compressedBase64 = compressAndWatermarkBitmap(
                    bitmap = bitmap,
                    location = "Galeri Perangkat IT Support",
                    dateStr = currentDateStr
                )
                onPhotoCaptured(compressedBase64)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ambil Foto Bukti IT Support",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Gunakan kamera asli, galeri HP, atau pilih preset visual di bawah untuk demonstrasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Row of main actions for Camera / Gallery
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch() },
                        modifier = Modifier.weight(1f).testTag("btn_real_camera"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kamera", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("btn_real_gallery"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galeri HP", fontSize = 12.sp)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.12f), modifier = Modifier.padding(bottom = 12.dp))

                Text(
                    "Preset Visual Simulasi (Emulator)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(MOCK_PHOTOS) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPhotoCaptured(item.url) }
                                .testTag("btn_capture_mock_${item.title.replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.url,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Preset: ${item.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}
