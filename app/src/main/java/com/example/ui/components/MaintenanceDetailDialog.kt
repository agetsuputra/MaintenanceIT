package com.example.ui.components

import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import com.example.ui.theme.AdaptiveColors
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
fun ActionTakenChecklist(actionStr: String) {
    val lines = actionStr.split("\n").filter { it.trim().isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val trim = line.trim()
            if (trim.contains("☑") || trim.contains("☐")) {
                val isChecked = trim.contains("☑")
                // remove the symbol marker
                val text = trim.replace("☑", "").replace("☐", "").trim()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = if (isChecked) "Selesai" else "Belum",
                        tint = if (isChecked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(trim, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

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

    var showingPinVerification by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val matchedAsset = viewModel.allAssets.collectAsStateWithLifecycle(emptyList()).value
        .find { it.inventoryNumber == maintenance.inventoryNumber }
    val assetLocation = matchedAsset?.location ?: "Gedung IT"

    val isDark = isSystemInDarkTheme()
    val cardBgColor = AdaptiveColors.cardColorAccent()
    val cardBorderColor = AdaptiveColors.cardBorderColor(cardBgColor)

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
            Box(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 0.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Detail Perawatan Rutin",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 0.dp), thickness = 0.5.dp)

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        val bottomStickyHeight = 56.dp
                        val cardSpacing = 16.dp
                        val safeBottomPadding = bottomStickyHeight + 20.dp + cardSpacing

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = cardSpacing, bottom = safeBottomPadding),
                            verticalArrangement = Arrangement.spacedBy(cardSpacing)
                        ) {
                            // 1. CARD IDENTITAS ASET
                            item {
                                val assetCardBgColor = AdaptiveColors.cardColorAccent(bg = MaterialTheme.colorScheme.surface)
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = assetCardBgColor),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                // Nama Aset
                                                Text(
                                                    text = matchedAsset?.name ?: "Perangkat IT",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 20.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                
                                                // Nomor Inventaris  -  Kategori Aset (Double spasi)
                                                Text(
                                                    text = "${maintenance.inventoryNumber}  -  ${matchedAsset?.type ?: "Lainnya"}",
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                                
                                                Spacer(Modifier.height(16.dp))
                                                
                                                // Bottom Info Row: dates and technician
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Start: ",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = dateFormatter.format(Date(maintenance.startTime)),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Finish: ",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = maintenance.endTime?.let { dateFormatter.format(Date(it)) } ?: "Dalam Pengerjaan",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    
                                                    // Bottom Right: Technician Name only
                                                    Text(
                                                        text = maintenance.technician,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            
                                            // Status Perawatan pojok kanan atas (hanya data statusnya saja)
                                            val statusColor = when (maintenance.status) {
                                                "Selesai", "Selesai & Terverifikasi" -> Color(0xFF2E7D32)
                                                "Hold" -> Color(0xFFF57C00)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = statusColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = maintenance.status,
                                                    color = statusColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. CARD PERAWATAN RUTIN (Tindakan, Catatan, Hasil)
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AdaptiveColors.cardColorAccent(bg = MaterialTheme.colorScheme.surface)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Section 1: Tindakan yang dilakukan
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Tindakan yang Dilakukan",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            ActionTakenChecklist(maintenance.actionTaken.ifBlank { localActionTaken })
                                        }

                                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // Section 2: Catatan / kendala
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Catatan / Kendala",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val notes = maintenance.issuesFound.ifBlank { localIssuesFound }
                                            Text(
                                                text = notes.ifBlank { "Tidak ada catatan atau kendala." },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // Section 3: Hasil akhir
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Hasil Akhir",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val outcome = maintenance.result.ifBlank { localResult }
                                            Text(
                                                text = outcome.ifBlank { "Kondisi Prima & Normal" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. CARD BUKTI FOTO (Before, After, Bersama Unit)
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = AdaptiveColors.cardColorAccent(bg = MaterialTheme.colorScheme.surface)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Foto Before
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            WatermarkedAsyncImage(
                                                photoStr = maintenance.photoBefore,
                                                defaultLocation = assetLocation,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f) // Square!
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Foto sebelum perawatan",
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // Foto After
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            WatermarkedAsyncImage(
                                                photoStr = maintenance.photoAfter ?: localPhotoAfter,
                                                defaultLocation = assetLocation,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f) // Square!
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Foto setelah perawatan",
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // Foto Bersama Unit
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            WatermarkedAsyncImage(
                                                photoStr = maintenance.photoUser ?: localPhotoUser,
                                                defaultLocation = assetLocation,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f) // Square!
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Foto unit pemilik perangkat",
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Interactive input form for incomplete items (retained dynamically)
                            if (isUnfinished) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                "Form Penyelesaian Perawatan",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
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
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.Transparent, 
                                                    focusedContainerColor = Color.Transparent
                                                )
                                            )

                                            OutlinedTextField(
                                                value = localIssuesFound,
                                                onValueChange = { localIssuesFound = it },
                                                label = { Text("Kendala Ditemukan (Opsional)") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.Transparent, 
                                                    focusedContainerColor = Color.Transparent
                                                )
                                            )

                                            OutlinedTextField(
                                                value = localResult,
                                                onValueChange = { localResult = it },
                                                label = { Text("Hasil Perawatan / Hasil Akhir") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    unfocusedContainerColor = Color.Transparent, 
                                                    focusedContainerColor = Color.Transparent
                                                )
                                            )

                                            // Capture photo after
                                            Text("📸 Ambil Foto Sesudah (After) *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            if (!localPhotoAfter.isNullOrBlank()) {
                                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
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
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Ambil Foto Sesudah", fontSize = 11.sp)
                                                }
                                            }

                                            // Capture photo user
                                            Text("📸 Ambil Foto Bersama Unit *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            if (!localPhotoUser.isNullOrBlank()) {
                                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
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
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(12.dp)
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
                        }

                        // Bottom Gradient Overlay (anchored directly to the top edge of the floating buttons)
                        com.example.ui.components.BottomFadeOverlay(
                            height = bottomStickyHeight + 20.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        // FLOATING STICKY ACTION PANEL AT THE BOTTOM
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(bottomStickyHeight + 20.dp)
                                .padding(bottom = 20.dp) // Sits perfectly flush on the overlay anchor
                        ) {
                            val bottomBarBgColor = AdaptiveColors.cardColorAccent()
                            val bottomBarBorderColor = AdaptiveColors.cardBorderColor(bottomBarBgColor)

                            if (maintenance.status == "Selesai") {
                                if (userRole == "Kepala Unit IT") {
                                    // ACC Button (Capsule design matching dashboard searchbar)
                                    Button(
                                        onClick = { showingPinVerification = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .testTag("btn_verify_maint_acc"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = bottomBarBgColor,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(28.dp),
                                        border = BorderStroke(1.dp, bottomBarBorderColor),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = "Verified Icon", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Verifikasi & ACC Laporan Perawatan", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    // Menunggu ACC status card (Same capsule shape, height and style)
                                    Card(
                                        shape = RoundedCornerShape(28.dp),
                                        colors = CardDefaults.cardColors(containerColor = bottomBarBgColor),
                                        border = BorderStroke(1.dp, bottomBarBorderColor),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "Info Icon", tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Menunggu ACC Kepala Unit IT",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            } else if (maintenance.status == "Selesai & Terverifikasi") {
                                // Approved status card (Same capsule shape, height and style)
                                Card(
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = bottomBarBgColor),
                                    border = BorderStroke(1.dp, bottomBarBorderColor),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = "Verified Status", tint = Color(0xFF2E7D32))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Laporan Perawatan Selesai & Terverifikasi (ACC)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            } else {
                                // For "Dalam Pengerjaan" or "Hold" states
                                val isHold = maintenance.status == "Hold"
                                val statusColor = if (isHold) Color(0xFFF57C00) else Color(0xFF0277BD)
                                Card(
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = bottomBarBgColor),
                                    border = BorderStroke(1.dp, bottomBarBorderColor),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = "Active Status Icon", tint = statusColor)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isHold) "Perawatan Tertunda / Hold" else "Perawatan Sedang Berlangsung",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
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
}
