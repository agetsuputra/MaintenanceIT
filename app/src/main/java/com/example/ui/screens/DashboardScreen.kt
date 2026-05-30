package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    assets: List<Asset>,
    repairs: List<Repair>,
    categories: List<com.example.data.model.Category>,
    isFirebaseEnabled: Boolean,
    onAssetClick: (Asset) -> Unit,
    onStatusChange: (Asset, String) -> Unit,
    searchQuery: String,
    searchBarTopDp: Dp
) {
    val defaultCategoryNames = remember {
        listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
    }

    val categoryNames = remember(categories) {
        if (categories.isEmpty()) defaultCategoryNames else categories.map { it.name }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val filteredAssets by remember(assets, searchQuery, selectedCategory) {
        derivedStateOf {
            assets.filter { item ->
                val matchesSearch = item.inventoryNumber.contains(searchQuery, ignoreCase = true) ||
                        item.name.contains(searchQuery, ignoreCase = true) ||
                        item.location.contains(searchQuery, ignoreCase = true)
                val matchesCategory = selectedCategory == null || item.type.equals(selectedCategory, ignoreCase = true)
                matchesSearch && matchesCategory
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp,
            bottom = searchBarTopDp + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Redesigned Samsung One UI Clock style centered header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val activeCount = remember(assets, selectedCategory) {
                    if (selectedCategory == null) {
                        assets.count { it.status == "Aktif" }
                    } else {
                        assets.count { it.type.equals(selectedCategory, ignoreCase = true) && it.status == "Aktif" }
                    }
                }
                val activeHeaderText = if (selectedCategory == null) {
                    "$activeCount Perangkat Aktif"
                } else {
                    "$activeCount $selectedCategory Aktif"
                }
                
                Text(
                    text = activeHeaderText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Wrapping categories (like Sen, 1 Jun 06.45 style but dynamically wrapped list)
                val categoryNamesWithSemua = remember(categoryNames) {
                    listOf("Semua") + categoryNames
                }
                
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryNamesWithSemua.forEach { cat ->
                        val isSelected = if (cat == "Semua") selectedCategory == null else selectedCategory == cat
                        val textAlpha = if (isSelected) 1f else 0.5f
                        val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            fontWeight = textWeight,
                            color = textColor.copy(alpha = textAlpha),
                            modifier = Modifier
                                .clickable {
                                    if (cat == "Semua") {
                                        selectedCategory = null
                                    } else {
                                        selectedCategory = if (selectedCategory == cat) null else cat
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
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
                AssetItemCard(
                    asset = asset,
                    onStatusChange = { newStatus -> onStatusChange(asset, newStatus) },
                    onClick = { onAssetClick(asset) }
                )
            }
        }
    }
}
