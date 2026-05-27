package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.launch

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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

    val themeBgColor = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val slotMetrics = com.example.util.rememberFloatingSlotMetrics(floatingElementHeight = 56.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_repair_form")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 84.dp,
                bottom = slotMetrics.anchorHeight + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Input No. Inventaris Perangkat (Skenario TextBox langsung tanpa Card induk)
            item {
                OutlinedTextField(
                    value = invNum,
                    onValueChange = { invNum = it.trim().uppercase() },
                    label = { Text("Nomor Inventaris Perangkat *") },
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_inv")
                )
            }

            item {
                OutlinedTextField(
                    value = problem,
                    onValueChange = { problem = it },
                    label = { Text("Kendala / Keluhan Kerusakan *") },
                    placeholder = { Text("Contoh: Monitor flicker merah jambu atau komputer sering hang.") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_problem")
                )
            }

            item {
                OutlinedTextField(
                    value = cause,
                    onValueChange = { cause = it },
                    label = { Text("Penyebab Kerusakan (Bisa diisi nanti jika hold)") },
                    placeholder = { Text("Contoh: Overheat pendingin kering kotor, atau RAM kendor.") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_cause")
                )
            }

            item {
                OutlinedTextField(
                    value = actionTaken,
                    onValueChange = { actionTaken = it },
                    label = { Text("Tindak Lanjut / Solusi (Wajib jika selesai)") },
                    placeholder = { Text("Contoh: Re-pasta thermal, pembersihan debu kipas, kencangkan ram.") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_tech")
                )
            }

            // Camera Simulated Photo Pickers (3 Photo Workflow) - Directly embedded as separate spaced items
            item {
                val matchedAsset = assetList.find { it.inventoryNumber == invNum }
                val assetLocation = matchedAsset?.location ?: "Gedung IT"

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("repair_btn_photo_before"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null,
                        border = null
                    ) {
                        Text("Ambil Foto Sebelum / Mulai Kerja", fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                val matchedAsset = assetList.find { it.inventoryNumber == invNum }
                val assetLocation = matchedAsset?.location ?: "Gedung IT"

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("repair_btn_photo_after"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null,
                        border = null,
                        enabled = status == "Selesai" || photoBefore != null
                    ) {
                        Text("Ambil Foto Sesudah Pekerjaan Selesai", fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                val matchedAsset = assetList.find { it.inventoryNumber == invNum }
                val assetLocation = matchedAsset?.location ?: "Gedung IT"

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("repair_btn_photo_user"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null,
                        border = null,
                        enabled = status == "Selesai" || photoBefore != null
                    ) {
                        Text("Ambil Foto Bukti Bersama Unit", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Top Gradient (starts from floating profile area and fades upward)
        com.example.ui.components.TopFadeOverlay(
            height = statusBarHeight + 72.dp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Gradient Overlay (anchored directly to the top edge of the action buttons)
        val bottomEdge = (slotMetrics.bottomOffset - 16.dp).coerceAtLeast(0.dp)
        val gradientHeight = 56.dp + 16.dp
        com.example.ui.components.BottomFadeOverlay(
            height = gradientHeight,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomEdge)
        )

        // Floating Row of cancel and save buttons following the keyboard
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = slotMetrics.bottomOffset)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isFormValid = invNum.isNotBlank() && problem.isNotBlank() && technician.isNotBlank() &&
                    !photoBefore.isNullOrBlank() &&
                    (status != "Selesai" || (actionTaken.isNotBlank() && !photoAfter.isNullOrBlank() && !photoUser.isNullOrBlank())) &&
                    (status != "Hold" || (holdReason.isNotBlank() && holdEstimate.isNotBlank()))

            // Tombol Batal: Berwarna solid non-transparan
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
                    .height(56.dp)
                    .testTag("btn_cancel_repair")
            ) {
                Text("Batal")
            }

            // Tombol Simpan: Berwarna solid non-transparan
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    val repair = Repair(
                        inventoryNumber = invNum,
                        startTime = System.currentTimeMillis() - 2 * 3600 * 1000, 
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = if (isDark) Color(0xFF333333) else Color(0xFFE2E8F0),
                    disabledContentColor = if (isDark) Color(0xFF757575) else Color(0xFF94A3B8)
                ),
                elevation = null,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("btn_save_repair")
            ) {
                Text("Simpan")
            }
        }
    }
}
