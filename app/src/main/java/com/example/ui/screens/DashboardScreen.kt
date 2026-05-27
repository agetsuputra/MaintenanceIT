package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.ui.components.AssetItemCard
import com.example.ui.components.StatCard

@Composable
fun DashboardScreen(
    assets: List<Asset>,
    repairs: List<Repair>,
    isFirebaseEnabled: Boolean,
    onAssetClick: (Asset) -> Unit,
    searchQuery: String,
    searchBarTopDp: Dp
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterAllSelected by remember { mutableStateOf(true) }

    val filteredAssets = remember(assets, searchQuery, selectedCategories, filterAllSelected) {
        assets.filter {
            val matchesSearch = it.inventoryNumber.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.location.contains(searchQuery, ignoreCase = true)
            val matchesCategory = filterAllSelected || selectedCategories.contains(it.type)
            matchesSearch && matchesCategory
        }
    }

    // Count statistics
    val totalAssets = remember(assets) { assets.size }
    val activeAssets = remember(assets) { assets.count { it.status == "Aktif" } }
    val brokenAssets = remember(assets) { assets.count { it.status == "Rusak Permanen" } }
    val holdAssets = remember(assets) { assets.count { it.status == "Hold" || it.status == "Dalam Pengerjaan" } }

    val dashboardBottomPadding = searchBarTopDp + 16.dp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card with statistics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dasbor Manajemen Perangkat",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isFirebaseEnabled) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isFirebaseEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirebaseEnabled) Color(0xFF2E7D32) else Color.Red)
                            )
                            Text(
                                text = if (isFirebaseEnabled) "Cloud Sync Aktif" else "Database Lokal",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFirebaseEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Grid stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Aset",
                            value = totalAssets.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        StatCard(
                            label = "Aktif",
                            value = activeAssets.toString(),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        StatCard(
                            label = "Rusak",
                            value = brokenAssets.toString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        StatCard(
                            label = "Hold",
                            value = holdAssets.toString(),
                            color = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            }
        }

        // Heading with Filter button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daftar Seluruh Perangkat (${filteredAssets.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFilterDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Filter",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Empty State checking
        if (filteredAssets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Belum ada perangkat terdaftar",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Silakan ketik nomor inventaris baru di search bar untuk mendaftar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredAssets, key = { it.inventoryNumber }) { asset ->
                AssetItemCard(asset = asset, onClick = { onAssetClick(asset) })
            }
        }

        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dashboardBottomPadding)
            )
        }
    }

    if (showFilterDialog) {
        val availableCategories = listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Kategori Aset", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filterAllSelected = !filterAllSelected
                                    if (filterAllSelected) {
                                        selectedCategories = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = filterAllSelected,
                                onCheckedChange = { checked ->
                                    filterAllSelected = checked
                                    if (checked) {
                                        selectedCategories = emptySet()
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Semua Kategori (All)")
                        }
                    }
                    items(availableCategories) { cat ->
                        val isChecked = !filterAllSelected && selectedCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedCategories.toMutableSet()
                                    if (current.contains(cat)) {
                                        current.remove(cat)
                                    } else {
                                        current.add(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val current = selectedCategories.toMutableSet()
                                    if (checked) {
                                        current.add(cat)
                                    } else {
                                        current.remove(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cat)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
