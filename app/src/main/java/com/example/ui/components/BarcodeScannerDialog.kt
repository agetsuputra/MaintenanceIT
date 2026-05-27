package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Asset
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.util.Locale

@Composable
fun BarcodeScannerDialog(
    assetList: List<Asset>,
    onAssetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }

    val qrScanner = remember {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            GmsBarcodeScanning.getClient(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val launchScanner = {
        if (qrScanner != null) {
            try {
                qrScanner.startScan()
                    .addOnSuccessListener { barcode ->
                        val result = barcode.rawValue
                        if (!result.isNullOrBlank()) {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onAssetSelected(result)
                        }
                    }
                    .addOnFailureListener { e ->
                        scanError = "Pindai gagal atau dibatalkan."
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                scanError = "Layanan pemindai tidak tersedia di perangkat ini."
            }
        } else {
            scanError = "Fitur scan tidak didukung di perangkat ini (GMS tidak tersedia)."
        }
    }

    // Proactively launch scanner when dialog first opens
    LaunchedEffect(Unit) {
        launchScanner()
    }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Pemindai Barcode & QR Code",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Menggunakan kamera internal Google Play Services untuk membaca kode UTF-8.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Beautiful Scan Visualizer Frame with laser line on top
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clickable { launchScanner() }
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Dotted corners & animated lasers
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = Color.Green,
                            size = size,
                            style = Stroke(width = 4f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        // Laser red horizontal line in center
                        drawLine(
                            color = Color.Red,
                            start = Offset(10f, size.height / 2),
                            end = Offset(size.width - 10f, size.height / 2),
                            strokeWidth = 6f
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(0.8f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "KETUK UNTUK PINDAI",
                            color = Color.Green,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (scanError != null) {
                    Text(
                        scanError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Button(
                    onClick = { launchScanner() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Kamera Scan QR", fontSize = 13.sp)
                }

                // Asset selection from registered DB to "simulate a successful scan"
                Text(
                    "Ketuk aset untuk menyelesaikan pemindaian:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(6.dp))

                if (assetList.isEmpty()) {
                    Text(
                        "Belum ada inventaris. Ketik kode manual di bawah ini.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                                RoundedCornerShape(8.dp)
                             )
                            .padding(4.dp)
                    ) {
                        items(assetList) { asset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        onAssetSelected(asset.inventoryNumber)
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(asset.inventoryNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                Text(asset.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Manual manual inputs backup
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it.uppercase(Locale.getDefault()) },
                    label = { Text("Input Nomor Inventaris Manual") },
                    placeholder = { Text("Contoh: INV-PC-102") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_tf_manual"),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onAssetSelected(textInput)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Simpan", tint = Color.Green)
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}
