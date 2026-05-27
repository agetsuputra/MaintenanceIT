package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.Repair
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairsScreen(
    repairs: List<Repair>,
    assets: List<Asset>,
    startDate: Long,
    endDate: Long,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onRepairClick: (Repair) -> Unit,
    onAddRepairClick: () -> Unit,
    onExportClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Category filter state for repairs list (matching dashboard filter)
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterAllSelected by remember { mutableStateOf(true) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val filteredRepairs = remember(repairs, assets, selectedCategories, filterAllSelected) {
        repairs.filter { repair ->
            val matchedAsset = assets.find { it.inventoryNumber == repair.inventoryNumber }
            val assetType = matchedAsset?.type ?: "Lainnya"
            filterAllSelected || selectedCategories.contains(assetType)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("repairs_screen")
    ) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp // Distance from lowest repair card to Catat Perbaikan is 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title (scrollable)
            item {
                Text(
                    "Laporan Perbaikan Kerusakan",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Header with Filter button (scrollable)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Perbaikan (${filteredRepairs.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
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
                            contentDescription = "Filter Kategori",
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

            // Repair list logic
            if (filteredRepairs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tidak ada perbaikan pada rentang ini",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRepairs, key = { it.id }) { repair ->
                    RepairItemCard(
                        repair = repair,
                        assets = assets,
                        onClick = { onRepairClick(repair) }
                    )
                }
            }
        }

        // Floating Gradient Overlay for bottom floating buttons
        com.example.ui.components.BottomFadeOverlay(
            height = 120.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        val cardColor = com.example.ui.theme.AdaptiveColors.cardColorAccent()
        val cardBorderColor = com.example.ui.theme.AdaptiveColors.cardBorderColor(cardColor)

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Circular Date Range Picker (like hamburger menu style)
            IconButton(
                onClick = { showDateRangePicker = true },
                modifier = Modifier
                    .size(56.dp)
                    .background(cardColor, CircleShape)
                    .border(1.dp, cardBorderColor, CircleShape)
                    .testTag("btn_filter_start_date")
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Pilih Rentang Tanggal",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Center Button: Wide "Catat Perbaikan" (Styled like Searchbar, Centered, No Shadow)
            Card(
                onClick = onAddRepairClick,
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, cardBorderColor),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("btn_add_repair")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Catat Perbaikan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right Button: Export PDF (Circular like hamburger menu style)
            IconButton(
                onClick = onExportClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(cardColor, CircleShape)
                    .border(1.dp, cardBorderColor, CircleShape)
                    .testTag("btn_export_repairs")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Export PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDateRangePicker) {
        SimpleDateRangePickerDialog(
            initialStartDate = startDate,
            initialEndDate = endDate,
            onDismiss = { showDateRangePicker = false },
            onDateRangeSelected = { start, end ->
                onStartDateChange(start)
                onEndDateChange(end)
            }
        )
    }

    if (showFilterDialog) {
        val availableCategories = listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Kategori Perangkat", fontWeight = FontWeight.Bold) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDateRangePickerDialog(
    initialStartDate: Long,
    initialEndDate: Long,
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDate,
        initialSelectedEndDateMillis = initialEndDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    } else if (start != null) {
                        onDateRangeSelected(start, start + 86399000L)
                    }
                    onDismiss()
                }
            ) {
                Text("Pilih", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Pilih Rentang Tanggal",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RepairItemCard(repair: Repair, assets: List<Asset>, onClick: () -> Unit) {
    val matchedAsset = assets.find { it.inventoryNumber == repair.inventoryNumber }
    val assetName = matchedAsset?.name ?: "Perangkat Tidak Dikenal"
    val assetType = matchedAsset?.type ?: "Lainnya"
    
    val statusColor = when (repair.status) {
        "Hold" -> Color(0xFFEF6C00) // Orange
        "Dalam Pengerjaan" -> Color(0xFF1976D2) // Blue
        else -> Color(0xFF2E7D32) // Green
    }

    val cardColor = com.example.ui.theme.AdaptiveColors.cardColorAccent()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("repair_card_${repair.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // First Row: Asset Name and Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = repair.inventoryNumber,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = repair.status,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = assetType,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thin Horizontal Divider Line like main assets card style
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Second Row: Kendala (on the bottom left) and Teknisi (on the bottom right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kendala
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = repair.problem,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Teknisi Penanggung Jawab
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = repair.technician,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
