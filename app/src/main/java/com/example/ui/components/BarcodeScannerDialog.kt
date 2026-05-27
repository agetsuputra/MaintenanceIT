package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Asset
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BarcodeScannerDialog(
    assetList: List<Asset>,
    onAssetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var scanStatus by remember { mutableStateOf("Membuka Kamera...") }
    var scaleFraction by remember { mutableStateOf(0.9f) }

    // Pulse animation for the viewfinder corners
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinder_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Run the high-fidelity Google Play Services auto-scan simulation sequence
    LaunchedEffect(Unit) {
        scaleFraction = 1.0f
        delay(400)
        scanStatus = "Mencari Kode QR / Barcode..."
        delay(1000)
        scanStatus = "Mendeteksi..."
        
        // Determine the simulated barcode value
        val scannedCode = if (assetList.isNotEmpty()) {
            // Pick a random existing asset ID to seamlessly simulate matching an asset for Repair/Maintenance
            assetList.random().inventoryNumber
        } else {
            // Generate a fresh clean ID if registering a new asset
            val prefix = listOf("AST-HW", "AST-NW", "AST-SW", "AST-IT").random()
            val randomNumber = Random.nextInt(10000, 99999)
            "$prefix-$randomNumber"
        }
        
        delay(400)
        scanStatus = "Kode berhasil dipindai!"
        delay(300)
        onAssetSelected(scannedCode)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.95f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                // Main Google Play Services style Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Google Play Services logo and brand indicator at the top
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Play Services",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Authentic Play Services Viewfinder Window
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Play Services style Corner Guides (Clean, professional white borders)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            val cornerSize = 24.dp
                            val lineThickness = 3.dp
                            val cornerColor = Color.White.copy(alpha = borderAlpha)

                            // Top Left Corner
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.TopStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(lineThickness)
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(lineThickness)
                                        .fillMaxHeight()
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                            }

                            // Top Right Corner
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.TopEnd)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(lineThickness)
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(lineThickness)
                                        .fillMaxHeight()
                                        .align(Alignment.TopEnd)
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                            }

                            // Bottom Left Corner
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.BottomStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(lineThickness)
                                        .align(Alignment.BottomStart)
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(lineThickness)
                                        .fillMaxHeight()
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                            }

                            // Bottom Right Corner
                            Box(
                                modifier = Modifier
                                    .size(cornerSize)
                                    .align(Alignment.BottomEnd)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(lineThickness)
                                        .align(Alignment.BottomEnd)
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(lineThickness)
                                        .fillMaxHeight()
                                        .align(Alignment.BottomEnd)
                                        .background(cornerColor, RoundedCornerShape(lineThickness))
                                )
                            }
                        }

                        // Central Indicator Icon
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Simulated Scanning State Status Title
                    Text(
                        text = scanStatus,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    // Ergonomic Close Button centered directly below the viewfinder as requested
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("btn_close_scanner_sim"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Pemindai",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
