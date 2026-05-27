package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Repair
import com.example.ui.viewmodel.ITViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RepairDetailDialog(
    repair: Repair,
    onDismiss: () -> Unit,
    onResumeRepair: (Repair) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    userRole: String,
    onVerifyRepair: (Repair) -> Unit,
    viewModel: ITViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isUnfinished = repair.status == "Hold" || repair.status == "Dalam Pengerjaan"

    // Local mutable state for editing/resolving the repair
    var localCause by remember { mutableStateOf(repair.cause) }
    var localActionTaken by remember { mutableStateOf(repair.actionTaken) }
    var localPhotoAfter by remember { mutableStateOf<String?>(repair.photoAfter) }
    var localPhotoUser by remember { mutableStateOf<String?>(repair.photoUser) }

    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }

    var showingPinVerification by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val matchedAsset = viewModel.allAssets.collectAsStateWithLifecycle(emptyList()).value
        .find { it.inventoryNumber == repair.inventoryNumber }
    val assetLocation = matchedAsset?.location ?: "Gedung IT"

    if (showingPinVerification) {
        PinVerificationDialog(
            viewModel = viewModel,
            onDismiss = { showingPinVerification = false },
            onPinCorrect = {
                showingPinVerification = false
                val finalRepair = repair.copy(
                    status = "Selesai & Terverifikasi",
                    cause = localCause.ifBlank { repair.cause },
                    actionTaken = localActionTaken.ifBlank { repair.actionTaken },
                    photoAfter = localPhotoAfter ?: repair.photoAfter,
                    photoUser = localPhotoUser ?: repair.photoUser
                )
                onVerifyRepair(finalRepair)
            }
        )
    }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detail Laporan Perbaikan",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Aset yang Diperbaiki:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(matchedAsset?.name ?: "Perangkat Tidak Dikenal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                Text("No. Inventaris: ${repair.inventoryNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                RowValue("Waktu Mulai", dateFormatter.format(Date(repair.startTime)))
                                RowValue("Waktu Selesai", repair.endTime?.let { dateFormatter.format(Date(it)) } ?: "Masih Tertunda (${repair.status})")
                                RowValue("Teknisi", repair.technician)
                            }
                        }
                    }

                    item {
                        DetailSection("Status Kerusakan", repair.status) {
                            val color = when (repair.status) {
                                "Hold" -> Color(0xFFE65100)
                                "Dalam Pengerjaan" -> Color(0xFF0277BD)
                                else -> Color(0xFF2E7D32)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    repair.status,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        DetailTextSection("Masalah / Kendala", repair.problem)
                    }

                    if (repair.status == "Selesai" || repair.status == "Selesai & Terverifikasi") {
                        item {
                            DetailTextSection("Penyebab", repair.cause.ifBlank { "Belum dicatat / Belum diketahui" })
                        }

                        item {
                            DetailTextSection("Tindakan / Solusi", repair.actionTaken)
                        }
                    }

                    if (repair.status == "Hold") {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Informasi Hold / Pending:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    Spacer(Modifier.height(6.dp))
                                    RowValue("Alasan Pending", repair.holdReason ?: "")
                                    RowValue("Estimasi Selesai", repair.holdEstimate ?: "")
                                }
                            }
                        }
                    }

                    // Display all 3 photos
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                textBuktiSOP()
                                
                                // Before photo status
                                Text("📸 Foto Sebelum (Before)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                WatermarkedAsyncImage(
                                    photoStr = repair.photoBefore,
                                    defaultLocation = "IT Room",
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                )

                                if (repair.status == "Selesai" || repair.status == "Selesai & Terverifikasi") {
                                    Text("📸 Foto Sesudah (After)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = repair.photoAfter,
                                        defaultLocation = "IT Room",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )

                                    Text("📸 Foto Bersama Unit (Serah Terima)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = repair.photoUser,
                                        defaultLocation = "IT Room",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )
                                }
                            }
                        }
                    }

                    // INTERACTIVE COMPLETION SUB-FORM FOR UNFINISHED REPAIRS
                    if (isUnfinished) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Form Penyelesaian Pekerjaan",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Untuk menyelesaikan pekerjaan perbaikan ini, isi solusi akhir and ambil 2 foto verifikasi penutup berikut secara langsung.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = localCause,
                                        onValueChange = { localCause = it },
                                        label = { Text("Penyebab Akhir *Wajib") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    OutlinedTextField(
                                        value = localActionTaken,
                                        onValueChange = { localActionTaken = it },
                                        label = { Text("Solusi / Tindak Lanjut *Wajib") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    // Capture Photo After Slot
                                    Text("📸 Ambil Foto Sesudah (After) *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoAfter.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoAfter, assetLocation, modifier = Modifier.fillMaxSize())
                                            IconButton(
                                                onClick = { localPhotoAfter = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(0.6f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenCamera { photo -> localPhotoAfter = photo } },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ambil Foto Sesudah", fontSize = 11.sp)
                                        }
                                    }

                                    // Capture Photo User Slot
                                    Text("📸 Ambil Foto Bersama Unit *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoUser.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoUser, assetLocation, modifier = Modifier.fillMaxSize())
                                            IconButton(
                                                onClick = { localPhotoUser = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(0.6f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenCamera { photo -> localPhotoUser = photo } },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ambil Foto Bersama Unit", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    // Validate and Submit
                                    val isResolveEnabled = localActionTaken.isNotBlank() &&
                                            localCause.isNotBlank() &&
                                            !localPhotoAfter.isNullOrBlank() &&
                                            !localPhotoUser.isNullOrBlank()

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            val updatedRepair = repair.copy(
                                                status = "Selesai",
                                                endTime = System.currentTimeMillis(),
                                                cause = localCause,
                                                actionTaken = localActionTaken,
                                                photoAfter = localPhotoAfter,
                                                photoUser = localPhotoUser,
                                                holdReason = null,
                                                holdEstimate = null
                                            )
                                            onResumeRepair(updatedRepair)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = isResolveEnabled,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_complete_unfinished_repair"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Kirim & Selesaikan Perbaikan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (repair.status == "Selesai") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            if (userRole == "Kepala Unit IT") {
                                Button(
                                    onClick = { showingPinVerification = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("btn_verify_repair_acc"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verifikasi & ACC Laporan Perbaikan", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Laporan perbaikan selesai. Menunggu ACC/Verifikasi oleh Kepala Unit IT.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else if (repair.status == "Selesai & Terverifikasi") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Laporan Perbaikan Selesai & Terverifikasi oleh Kepala Unit IT (ACC)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun textBuktiSOP() {
    Text("Laporan Bukti Foto Sesuai SOP:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
