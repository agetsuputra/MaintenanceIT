package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.example.data.model.Maintenance
import com.example.ui.components.WatermarkedAsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceForm(
    assetList: List<Asset>,
    onSave: (Maintenance) -> Unit,
    onCancel: () -> Unit,
    onOpenScanner: ((String) -> Unit) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    initialInventoryNumber: String? = null,
    currentUser: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var invNum by remember { mutableStateOf(initialInventoryNumber ?: "") }
    var actionTaken by remember { mutableStateOf("") }
    var issuesFound by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Kondisi Prima & Normal") }
    
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    val statusOptions = listOf("Dalam Pengerjaan", "Selesai")
    var statusExpanded by remember { mutableStateOf(false) }

    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf(currentUser) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("add_maint_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Input Perawatan Rutin Perangkat",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0277BD)
            )
            Text(
                "Masukkan data aktivitas pembersihan, kalibrasi, cek berkala perangkat IT.",
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
                        placeholder = { Text("No. Inventaris (Contoh: INV-PR-004)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onOpenScanner { code ->
                                        invNum = code.trim().uppercase()
                                    }
                                },
                                modifier = Modifier.testTag("maint_btn_inv_scan")
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
                            .testTag("maint_tf_inv"),
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = actionTaken,
                onValueChange = { actionTaken = it },
                label = { Text("Tindakan yang Dilakukan (Wajib jika selesai) *") },
                placeholder = { Text("Contoh: Pembersihan debu fisik CPU, defrag hardisk, pengecekan tinta printer.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("maint_tf_action")
            )
        }

        item {
            OutlinedTextField(
                value = issuesFound,
                onValueChange = { issuesFound = it },
                label = { Text("Kendala yang Ditemukan (Jika Ada)") },
                placeholder = { Text("Contoh: Nozzle printer warna merah kotor buntu, fan berbunyi berisik.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("maint_tf_issue")
            )
        }

        item {
            OutlinedTextField(
                value = result,
                onValueChange = { result = it },
                label = { Text("Hasil Perawatan / Rekomendasi") },
                placeholder = { Text("Contoh: Lolos cetak test page kualifikasi normal, disarankan ganti kipas bulan depan.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("maint_tf_result")
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
                    label = { Text("Status Perawatan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("maint_tf_status")
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
                    .testTag("maint_tf_tech"),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
        }

        // Camera Simulated Photo Pickers (Maintenance 3 Photo Workflow)
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
                        "Semua foto dilengkapi tanda air berisi Lokasi perangkat ($assetLocation) dan Tanggal ($currentDateStr). Pengambilan dari galeri dinonaktifkan demi keaslian data Tiruan.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1. Photo Before
                    Column {
                        Text("1. Foto Sebelum Perawatan (Before) *Wajib", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                Text("Ambil Foto Sebelum / Mulai Perawatan")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 2. Photo After
                    Column {
                        Text(
                            text = "2. Foto Sesudah Perawatan (After) " + if (status == "Selesai") "*Wajib" else "(Opsional)",
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
                                Text("Ambil Foto Sesudah Perawatan Selesai")
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

        item {
            val isFormValid = invNum.isNotBlank() && technician.isNotBlank() &&
                    !photoBefore.isNullOrBlank() &&
                    (status != "Selesai" || (actionTaken.isNotBlank() && !photoAfter.isNullOrBlank() && !photoUser.isNullOrBlank()))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onCancel()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_cancel_maint")
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        val maint = Maintenance(
                            inventoryNumber = invNum,
                            startTime = System.currentTimeMillis() - 3600 * 1000,
                            endTime = if (status == "Selesai") System.currentTimeMillis() else null,
                            actionTaken = actionTaken,
                            issuesFound = issuesFound,
                            result = result,
                            status = status,
                            photoBefore = photoBefore,
                            photoAfter = photoAfter,
                            photoUser = photoUser,
                            technician = technician
                        )
                        onSave(maint)
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_save_maint")
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}
