package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.ui.components.WatermarkedAsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepairForm(
    assetList: List<Asset>,
    onSave: (Repair) -> Unit,
    onCancel: () -> Unit,
    onOpenScanner: ((String) -> Unit) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    initialInventoryNumber: String? = null,
    currentUser: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var invNum by remember { mutableStateOf(initialInventoryNumber ?: "") }
    var problem by remember { mutableStateOf("") }
    var cause by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    var holdReason by remember { mutableStateOf("") }
    var holdEstimate by remember { mutableStateOf("") }
    
    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf(currentUser) }

    val statusOptions = listOf("Dalam Pengerjaan", "Hold", "Selesai")
    var statusExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("add_repair_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Registrasi Perbaikan Kerusakan",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Segera catat kerusakan laptop, computer, printer, atau jaringan yang ditangani.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Device selection via typing or simulator scanning
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Kaitkan Inventaris Perangkat",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = invNum,
                        onValueChange = { invNum = it.trim().uppercase() },
                        label = { Text("Nomor Inventaris Perangkat") },
                        placeholder = { Text("No. Inventaris (Contoh: INV-PC-001)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onOpenScanner { code ->
                                        invNum = code.trim().uppercase()
                                    }
                                },
                                modifier = Modifier.testTag("repair_btn_inv_scan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR/Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("repair_tf_inv"),
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = problem,
                onValueChange = { problem = it },
                label = { Text("Kendala / Keluhan Kerusakan *") },
                placeholder = { Text("Contoh: Monitor flicker merah jambu atau komputer sering hang.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("repair_tf_problem")
            )
        }

        item {
            OutlinedTextField(
                value = cause,
                onValueChange = { cause = it },
                label = { Text("Penyebab Kerusakan (Bisa diisi nanti jika hold)") },
                placeholder = { Text("Contoh: Overheat pendingin kering kotor, atau RAM kendor.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("repair_tf_cause")
            )
        }

        item {
            OutlinedTextField(
                value = actionTaken,
                onValueChange = { actionTaken = it },
                label = { Text("Tindak Lanjut / Solusi (Wajib jika selesai)") },
                placeholder = { Text("Contoh: Re-pasta thermal, pembersihan debu kipas, kencangkan ram.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("repair_tf_action")
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status Hasil Pekerjaan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("repair_tf_status")
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                status = opt
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Conditional Alasan Hold & Estimasi
        if (status == "Hold") {
            item {
                OutlinedTextField(
                    value = holdReason,
                    onValueChange = { holdReason = it },
                    label = { Text("Alasan Pending / Hold *") },
                    placeholder = { Text("Contoh: Menunggu modul sparepart impor / LCD pengganti.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_hold_reason")
                )
            }
            item {
                OutlinedTextField(
                    value = holdEstimate,
                    onValueChange = { holdEstimate = it },
                    label = { Text("Estimasi Penyelesaian (Waktu) *") },
                    placeholder = { Text("Contoh: 3-5 Hari Kerja") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_hold_est")
                )
            }
        }

        // Technician Name
        item {
            OutlinedTextField(
                value = technician,
                onValueChange = { technician = it },
                label = { Text("Teknisi Penanggung Jawab") },
                placeholder = { Text("Nama Lengkap") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repair_tf_tech"),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
        }

        // Camera Simulated Photo Pickers (3 Photo Workflow)
        item {
            val matchedAsset = assetList.find { it.inventoryNumber == invNum }
            val assetLocation = matchedAsset?.location ?: "Gedung IT"
            val currentDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Watermarked Photo Verification Flow",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Semua foto dilengkapi tanda air berisi Lokasi perangkat ($assetLocation) dan Tanggal ($currentDateStr). Pengambilan dari galeri dinonaktifkan demi keaslian data.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1. Photo Before
                    Column {
                        Text("1. Foto Sebelum Perbaikan (Before) *Wajib", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        if (!photoBefore.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoBefore, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoBefore = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoBefore = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sebelum / Mulai Kerja")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 2. Photo After
                    Column {
                        Text(
                            text = "2. Foto Sesudah Perbaikan (After) " + if (status == "Selesai") "*Wajib" else "(Opsional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (status == "Selesai" && photoAfter.isNullOrBlank()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!photoAfter.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoAfter, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoAfter = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoAfter = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                enabled = status == "Selesai" || photoBefore != null
							) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sesudah Pekerjaan Selesai")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 3. Photo with User
                    Column {
                        Text(
                            text = "3. Foto Bersama Tim & Unit " + if (status == "Selesai") "*Wajib" else "(Opsional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (status == "Selesai" && photoUser.isNullOrBlank()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!photoUser.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoUser, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoUser = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoUser = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = status == "Selesai" || photoBefore != null
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Bukti Bersama Unit")
                            }
                        }
                    }
                }
            }
        }

        // Form Submit Buttons
        item {
            val isFormValid = invNum.isNotBlank() && problem.isNotBlank() && technician.isNotBlank() &&
                    !photoBefore.isNullOrBlank() &&
                    (status != "Selesai" || (actionTaken.isNotBlank() && !photoAfter.isNullOrBlank() && !photoUser.isNullOrBlank())) &&
                    (status != "Hold" || (holdReason.isNotBlank() && holdEstimate.isNotBlank()))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isDark = isSystemInDarkTheme()
                val themeBgColor = if (isDark) Color(0xFF111827) else Color(0xFFF9FAFB)
                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onCancel()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFD1D5DB)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeBgColor,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_cancel_repair")
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        val repair = Repair(
                            inventoryNumber = invNum,
                            startTime = System.currentTimeMillis() - 2 * 3600 * 1000, // started 2 hours ago
                            endTime = if (status == "Selesai") System.currentTimeMillis() else null,
                            problem = problem,
                            cause = cause,
                            actionTaken = actionTaken,
                            status = status,
                            holdReason = if (status == "Hold") holdReason else null,
                            holdEstimate = if (status == "Hold") holdEstimate else null,
                            photoBefore = photoBefore,
                            photoAfter = photoAfter,
                            photoUser = photoUser,
                            technician = technician
                        )
                        onSave(repair)
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_save_repair")
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}
