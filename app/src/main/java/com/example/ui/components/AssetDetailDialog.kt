package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Asset
import com.example.data.model.AssetUpdateLog
import com.example.data.model.Maintenance
import com.example.data.model.Repair
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssetDetailDialog(
    asset: Asset,
    repairs: List<Repair>,
    maintenances: List<Maintenance>,
    updates: List<AssetUpdateLog>,
    onDismiss: () -> Unit,
    onAddRepairDirectly: () -> Unit,
    onAddMaintDirectly: () -> Unit,
    onUpdateAssetDirectly: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Informasi Detail Aset",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Aset
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = asset.name,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "No. Inventaris: ${asset.inventoryNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(asset.acquisitionDate))
                                val priceStr = asset.purchasePrice?.let {
                                    "Rp " + String.format(Locale.getDefault(), "%,.0f", it).replace(',', '.')
                                } ?: "-"
                                RowValue("Kategori", asset.type)
                                RowValue("Lokasi", asset.location)
                                RowValue("Status", asset.status)
                                RowValue("Tgl Pengadaan", dateStr)
                                RowValue("Harga Beli", priceStr)
                                RowValue("Spesifikasi", asset.description ?: "-")
                            }
                        }
                    }

                    // Direct action options
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onAddRepairDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Catat Perbaikan", fontSize = 10.sp, maxLines = 1)
                            }
                            Button(
                                onClick = onAddMaintDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Rawat Rutin", fontSize = 10.sp, maxLines = 1)
                            }
                            Button(
                                onClick = onUpdateAssetDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Update Aset", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    // History Title
                    item {
                        Text(
                            "Riwayat Aktivitas & Perbaikan",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Loop Update Logs First if any exists
                    if (updates.isNotEmpty()) {
                        item {
                            Text(
                                "Riwayat Update Detail & Status (${updates.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        items(updates) { u ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Update Informasi",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            dateFormatter.format(Date(u.updateTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    if (u.oldLocation != u.newLocation) {
                                        Text("Lokasi: ${u.oldLocation} ➔ ${u.newLocation}", fontSize = 11.sp)
                                    } else {
                                        Text("Lokasi: ${u.newLocation}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    if (u.oldStatus != u.newStatus) {
                                        Text("Status: ${u.oldStatus} ➔ ${u.newStatus}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text("Status: ${u.newStatus}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (!u.reasonForPermanentDamage.isNullOrBlank()) {
                                        Text("Sebab Rusak Permanen: ${u.reasonForPermanentDamage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                    }

                                    if (u.oldDescription != u.newDescription) {
                                        Text("Spesifikasi: ${u.oldDescription ?: "-"} ➔ ${u.newDescription ?: "-"}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (repairs.isEmpty() && maintenances.isEmpty() && updates.isEmpty()) {
                        item {
                            Text(
                                "Belum terdapat riwayat perbaikan maupun perawatan rutin.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                             )
                        }
                    }

                    // Loop Repairs
                    if (repairs.isNotEmpty()) {
                        item {
                            Text(
                                "Riwayat Perbaikan Kerusakan (${repairs.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(repairs) { r ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            r.status,
                                            fontWeight = FontWeight.Bold,
                                            color = if (r.status == "Selesai") Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            dateFormatter.format(Date(r.startTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Kendala: ${r.problem}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("Tindak Lanjut: ${r.actionTaken}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (r.status == "Hold") {
                                        Text("Alasan Hold: ${r.holdReason}", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Medium)
                                    }
                                    Text("Teknisi: ${r.technician}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Loop Maintenances
                    if (maintenances.isNotEmpty()) {
                        item {
                            Text(
                                "Riwayat Perawatan Rutin (${maintenances.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF0277BD)
                            )
                        }
                        items(maintenances) { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Berhasil Diperiksa",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            dateFormatter.format(Date(m.startTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tindakan: ${m.actionTaken}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("Hasil: ${m.result}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Teknisi: ${m.technician}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
