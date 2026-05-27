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
import com.example.data.model.Category
import com.example.data.model.Maintenance
import com.example.ui.components.WatermarkedAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceForm(
    assetList: List<Asset>,
    categoryList: List<Category>,
    onSave: (Maintenance) -> Unit,
    onCancel: () -> Unit,
    onOpenScanner: ((String) -> Unit) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    initialInventoryNumber: String? = null,
    currentUser: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val listState = rememberLazyListState()

    var invNum by remember { mutableStateOf(initialInventoryNumber ?: "") }
    var issuesFound by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Kondisi Prima & Normal") }
    
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    val statusOptions = listOf("Dalam Pengerjaan", "Hold", "Selesai")
    var statusExpanded by remember { mutableStateOf(false) }

    var holdReason by remember { mutableStateOf("") }
    var holdEstimate by remember { mutableStateOf("") }

    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf(currentUser) }

    // Dynamic Chceklist states
    val checklistStates = remember { mutableStateMapOf<String, Boolean>() }

    val themeBgColor = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val slotMetrics = com.example.util.rememberFloatingSlotMetrics(floatingElementHeight = 56.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_maint_form")
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
                    "Perawatan Rutin",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF0277BD)
                )
            }

            // Input No. Inventaris Perangkat (Skenario TextBox langsung tanpa Card induk)
            item {
                OutlinedTextField(
                    value = invNum,
                    onValueChange = { invNum = it.trim().uppercase() },
                    label = { Text("Nomor Inventaris Perangkat *") },
                    placeholder = { Text("No. Inventaris (Contoh: INV-PR-004)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onOpenScanner { code ->
                                    invNum = code.trim().uppercase()
                                }
                            },
                            modifier = Modifier
                                .testTag("maint_btn_inv_scan")
                                .offset(x = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR/Barcode",
                                tint = MaterialTheme.colorScheme.onSurface
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
                        .testTag("maint_tf_inv")
                )
            }

            // Dynamic Checklist Card (Replacing actionTaken textarea)
            item {
                val matchedAsset = assetList.find { it.inventoryNumber == invNum }
                val assetCategoryName = matchedAsset?.type
                val matchedCategory = categoryList.find { it.name.equals(assetCategoryName, ignoreCase = true) }
                val guidelinesList = remember(matchedCategory) {
                    matchedCategory?.guidelines?.split("||~||")?.filter { it.isNotBlank() } ?: emptyList()
                }

                // Synch checkboxes
                LaunchedEffect(guidelinesList) {
                    val currentKeys = checklistStates.keys.toList()
                    currentKeys.forEach { key ->
                        if (key !in guidelinesList) {
                            checklistStates.remove(key)
                        }
                    }
                    guidelinesList.forEach { item ->
                        if (item !in checklistStates) {
                            checklistStates[item] = false
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .testTag("maint_action_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    if (guidelinesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (invNum.isBlank()) "Pilih No. Inventaris untuk memuat checklist perawatan"
                                       else "Kategori perangkat tidak memiliki checklist tindakan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Checklist Tindakan Perawatan" + (if (assetCategoryName != null) " - $assetCategoryName" else ""),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            guidelinesList.forEach { question ->
                                val isChecked = checklistStates[question] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { checklistStates[question] = !isChecked }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checklistStates[question] = it },
                                        modifier = Modifier.testTag("maint_chk_${question.replace(" ", "_")}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Catatan / Kendala textarea (Optional)
            item {
                OutlinedTextField(
                    value = issuesFound,
                    onValueChange = { issuesFound = it },
                    label = { Text("Catatan / Kendala (Opsional)") },
                    placeholder = { Text("Catatan / Kendala") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("maint_tf_issue")
                )
            }

            item {
                OutlinedTextField(
                    value = result,
                    onValueChange = { result = it },
                    label = { Text("Hasil Perawatan / Rekomendasi") },
                    placeholder = { Text("Contoh: Lolos cetak test page kualifikasi normal, disarankan ganti kipas bulan depan.") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
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

            // Dynamic Hold Fields (Alasan hold & Estimasi)
            if (status == "Hold") {
                item {
                    OutlinedTextField(
                        value = holdReason,
                        onValueChange = { holdReason = it },
                        label = { Text("Alasan Pending / Hold *") },
                        placeholder = { Text("Masukkan alasan penundaan perawatan device") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("maint_tf_hold_reason")
                    )
                }

                item {
                    OutlinedTextField(
                        value = holdEstimate,
                        onValueChange = { holdEstimate = it },
                        label = { Text("Estimasi Penyelesaian (Waktu) *") },
                        placeholder = { Text("Contoh: 2 hari, s/d tanggal 25, setelah part datang") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("maint_tf_hold_estimate")
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
                        .testTag("maint_tf_tech")
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
                            .testTag("maint_btn_photo_before"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null,
                        border = null
                    ) {
                        Text("Ambil Foto Sebelum / Mulai Perawatan", fontWeight = FontWeight.Medium)
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
                            .testTag("maint_btn_photo_after"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null,
                        border = null,
                        enabled = status == "Selesai" || photoBefore != null
                    ) {
                        Text("Ambil Foto Sesudah Perawatan Selesai", fontWeight = FontWeight.Medium)
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
                            .testTag("maint_btn_photo_user"),
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
            val isFormValid = invNum.isNotBlank() && technician.isNotBlank() &&
                    !photoBefore.isNullOrBlank() &&
                    (
                        when (status) {
                            "Selesai" -> {
                                !photoAfter.isNullOrBlank() && !photoUser.isNullOrBlank()
                            }
                            "Hold" -> {
                                holdReason.isNotBlank() && holdEstimate.isNotBlank()
                            }
                            else -> true
                        }
                    )

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
                    .testTag("btn_cancel_maint")
            ) {
                Text("Batal")
            }

            // Tombol Simpan: Berwarna solid non-transparan
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    
                    // Serialize checkboxes to actionTaken
                    val matchedAsset = assetList.find { it.inventoryNumber == invNum }
                    val assetCategoryName = matchedAsset?.type
                    val matchedCategory = categoryList.find { it.name.equals(assetCategoryName, ignoreCase = true) }
                    val guidelinesList = matchedCategory?.guidelines?.split("||~||")?.filter { it.isNotBlank() } ?: emptyList()
                    val serializedActionTaken = if (guidelinesList.isEmpty()) {
                        "Melakukan perawatan rutin standar"
                    } else {
                        guidelinesList.joinToString("\n") { question ->
                            val isChecked = checklistStates[question] ?: false
                            val marker = if (isChecked) "☑" else "☐"
                            "$marker $question"
                        }
                    }

                    val maint = Maintenance(
                        inventoryNumber = invNum,
                        startTime = System.currentTimeMillis() - 3600 * 1000,
                        endTime = if (status == "Selesai") System.currentTimeMillis() else null,
                        actionTaken = serializedActionTaken,
                        issuesFound = issuesFound,
                        result = result,
                        status = status,
                        holdReason = if (status == "Hold") holdReason else null,
                        holdEstimate = if (status == "Hold") holdEstimate else null,
                        photoBefore = photoBefore,
                        photoAfter = photoAfter,
                        photoUser = photoUser,
                        technician = technician
                    )
                    onSave(maint)
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
                    .testTag("btn_save_maint")
            ) {
                Text("Simpan")
            }
        }
    }
}
