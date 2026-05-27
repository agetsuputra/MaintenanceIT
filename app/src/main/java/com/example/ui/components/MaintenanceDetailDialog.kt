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
import com.example.data.model.Maintenance
import com.example.ui.viewmodel.ITViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaintenanceDetailDialog(
    maintenance: Maintenance,
    onDismiss: () -> Unit,
    onUpdateMaintenance: (Maintenance) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    userRole: String,
    onVerifyMaintenance: (Maintenance) -> Unit,
    viewModel: ITViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isUnfinished = maintenance.status == "Dalam Pengerjaan" || maintenance.status == "Hold"

    // Edit states for concluding the maintenance activity
    var localActionTaken by remember { mutableStateOf(maintenance.actionTaken) }
    var localIssuesFound by remember { mutableStateOf(maintenance.issuesFound) }
    var localResult by remember { mutableStateOf(maintenance.result.ifBlank { "Kondisi Prima & Normal" }) }
    var localPhotoAfter by remember { mutableStateOf<String?>(maintenance.photoAfter) }
    var localPhotoUser by remember { mutableStateOf<String?>(maintenance.photoUser) }

    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }

    var showingPinVerification by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val matchedAsset = viewModel.allAssets.collectAsStateWithLifecycle(emptyList()).value
        .find { it.inventoryNumber == maintenance.inventoryNumber }
    val assetLocation = matchedAsset?.location ?: "Gedung IT"

    if (showingPinVerification) {
        PinVerificationDialog(
            viewModel = viewModel,
            onDismiss = { showingPinVerification = false },
            onPinCorrect = {
                showingPinVerification = false
                val finalMaint = maintenance.copy(
                    status = "Selesai & Terverifikasi",
                    actionTaken = localActionTaken.ifBlank { maintenance.actionTaken },
                    issuesFound = localIssuesFound.ifBlank { maintenance.issuesFound },
                    result = localResult.ifBlank { maintenance.result },
                    photoAfter = localPhotoAfter ?: maintenance.photoAfter,
                    photoUser = localPhotoUser ?: maintenance.photoUser
                )
                onVerifyMaintenance(finalMaint)
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
                        "Detail Perawatan Rutin",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0277BD)
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
                                Text("Aset yang Dirawat:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(maintenance.inventoryNumber, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0277BD))
                                Spacer(Modifier.height(8.dp))
                                RowValue("Waktu Mulai", dateFormatter.format(Date(maintenance.startTime)))
                                RowValue("Waktu Selesai", maintenance.endTime?.let { dateFormatter.format(Date(it)) } ?: "Masih Tertunda (Dalam Pengerjaan)")
                                RowValue("Teknisi", maintenance.technician)
                            }
                        }
                    }

                    item {
                        DetailSection("Status Perawatan", maintenance.status) {
                            val color = if (maintenance.status == "Selesai" || maintenance.status == "Selesai & Terverifikasi") {
                                Color(0xFF2E7D32)
                            } else if (maintenance.status == "Hold") {
                                Color(0xFFF57C00)
                            } else {
                                Color(0xFF0277BD)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    maintenance.status,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (maintenance.status == "Hold") {
                        item {
                            DetailTextSection("Alasan Pending / Hold", maintenance.holdReason ?: "-")
                        }
                        item {
                            DetailTextSection("Estimasi Penyelesaian", maintenance.holdEstimate ?: "-")
                        }
                    }

                    if (maintenance.status == "Selesai" || maintenance.status == "Selesai & Terverifikasi") {
                        item {
                            DetailTextSection("Tindakan yang Dilakukan", maintenance.actionTaken)
                        }

                        item {
                            DetailTextSection("Kendala Temuan", maintenance.issuesFound.ifBlank { "Tidak Ada / Bersih" })
                        }

                        item {
                            DetailTextSection("Hasil Akhir", maintenance.result)
                        }
                    }

                    // Photo Display (Before / After / User)
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Laporan Bukti Foto Sesuai SOP:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                // Before Photo displays
                                Text("📸 Foto Sebelum (Before)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                WatermarkedAsyncImage(
                                    photoStr = maintenance.photoBefore,
                                    defaultLocation = "IT Department",
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                )

                                if (maintenance.status == "Selesai" || maintenance.status == "Selesai & Terverifikasi") {
                                    Text("📸 Foto Sesudah (After)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = maintenance.photoAfter,
                                        defaultLocation = "IT Department",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )

                                    Text("📸 Foto Bersama Unit (Serah Terima)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = maintenance.photoUser,
                                        defaultLocation = "IT Department",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )
                                }
                            }
                        }
                    }

                    // INTERACTIVE RESOLUTION FOR UNFINISHED MAINTENACE
                    if (isUnfinished) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Form Penyelesaian Perawatan",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0277BD)
                                    )
                                    Text(
                                        "Lengkapi data pemeriksaan akhir secara real-time dan tangkap foto hasil verifikasi.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = localActionTaken,
                                        onValueChange = { localActionTaken = it },
                                        label = { Text("Tindakan Akhir *Wajib") },
                                        placeholder = { Text("Tindakan pembersihan, reparasi kecil, dll.") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    OutlinedTextField(
                                        value = localIssuesFound,
                                        onValueChange = { localIssuesFound = it },
                                        label = { Text("Kendala Ditemukan (Opsional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    OutlinedTextField(
                                        value = localResult,
                                        onValueChange = { localResult = it },
                                        label = { Text("Hasil Perawatan / Hasil Akhir") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    // Capture photo after
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

                                    // Capture photo user
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

                                    val isResolveEnabled = localActionTaken.isNotBlank() &&
                                            !localPhotoAfter.isNullOrBlank() &&
                                            !localPhotoUser.isNullOrBlank()

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            val updatedMaint = maintenance.copy(
                                                status = "Selesai",
                                                endTime = System.currentTimeMillis(),
                                                actionTaken = localActionTaken,
                                                issuesFound = localIssuesFound,
                                                result = localResult,
                                                photoAfter = localPhotoAfter,
                                                photoUser = localPhotoUser
                                            )
                                            onUpdateMaintenance(updatedMaint)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = isResolveEnabled,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_complete_unfinished_maint"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Kirim & Selesaikan Perawatan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (maintenance.status == "Selesai") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            if (userRole == "Kepala Unit IT") {
                                Button(
                                    onClick = { showingPinVerification = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("btn_verify_maint_acc"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verifikasi & ACC Laporan Perawatan", fontWeight = FontWeight.Bold)
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
                                            "Laporan perawatan selesai. Menunggu ACC/Verifikasi oleh Kepala Unit IT.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else if (maintenance.status == "Selesai & Terverifikasi") {
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
                                        "Laporan Perawatan Selesai & Terverifikasi oleh Kepala Unit IT (ACC)",
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
