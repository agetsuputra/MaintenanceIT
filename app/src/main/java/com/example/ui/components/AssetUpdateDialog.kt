package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Asset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetUpdateDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSave: (newLocation: String, newStatus: String, newDescription: String?, reasonForPermanentDamage: String?) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var location by remember { mutableStateOf(asset.location) }
    var status by remember { mutableStateOf(asset.status) }
    var description by remember { mutableStateOf(asset.description ?: "") }
    var reasonForPermanentDamage by remember { mutableStateOf("") }

    val statusOptions = listOf("Aktif", "Hold", "Dalam Pengerjaan", "Rusak Permanen")
    var statusExpanded by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Update Informasi & Status Aset",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "No. Inventaris: ${asset.inventoryNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi Perangkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = status,
                        onValueChange = {},
                        label = { Text("Status Perangkat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Conditional Reason Field if "Rusak Permanen" is selected
                if (status == "Rusak Permanen") {
                    OutlinedTextField(
                        value = reasonForPermanentDamage,
                        onValueChange = { reasonForPermanentDamage = it },
                        label = { Text("Penyebab Kerusakan Permanen *Wajib") },
                        isError = reasonForPermanentDamage.isBlank(),
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Spesifikasi & Catatan Tambahan") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    val canSave = status != "Rusak Permanen" || reasonForPermanentDamage.isNotBlank()
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onSave(
                                location,
                                status,
                                description.ifBlank { null },
                                if (status == "Rusak Permanen") reasonForPermanentDamage else null
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
