package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.ui.components.AssetItemCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    assets: List<Asset>,
    repairs: List<Repair>,
    categories: List<com.example.data.model.Category>,
    isFirebaseEnabled: Boolean,
    onAssetClick: (Asset) -> Unit,
    onStatusChange: (Asset, String) -> Unit,
    searchQuery: String,
    searchBarTopDp: Dp,
    onAddAssetClick: () -> Unit = {},
    onScrollAtTopChanged: (Boolean) -> Unit = {}
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

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenHeight = maxHeight
        val headerHeight = screenHeight / 3

        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val minHeaderHeight = 56.dp + statusBarHeight

        val maxScrollOffsetPx = with(density) { (headerHeight - minHeaderHeight).toPx() }

        val collapseProgress by remember(maxScrollOffsetPx) {
            derivedStateOf {
                if (scrollState.firstVisibleItemIndex > 0) {
                    1f
                } else if (maxScrollOffsetPx <= 0f) {
                    0f
                } else {
                    (scrollState.firstVisibleItemScrollOffset.toFloat() / maxScrollOffsetPx).coerceIn(0f, 1f)
                }
            }
        }

        // Notify parent activity/screen layout about scroll status at the top
        LaunchedEffect(collapseProgress) {
            onScrollAtTopChanged(collapseProgress == 0f)
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_column"),
            contentPadding = PaddingValues(
                bottom = searchBarTopDp + 24.dp
            )
        ) {
            // First item: upper 1/3 height spacer placeholder so the content flows under the header
            item {
                Spacer(modifier = Modifier.height(headerHeight))
            }

            // Second item: control action row (Add IconButton (+) and Menu IconButton (⁝) horizontally)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("control_action_row"),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onAddAssetClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_list_add_asset")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Aset Baru",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_list_more_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Lainnya",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tambah Aset Baru") },
                                onClick = {
                                    menuExpanded = false
                                    onAddAssetClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Semua Kategori") },
                                onClick = {
                                    menuExpanded = false
                                    selectedCategory = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Status Database: " + if (isFirebaseEnabled) "Firebase Cloud" else "Local SQLite") },
                                onClick = {
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

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
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredAssets, key = { it.inventoryNumber }) { asset ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        AssetItemCard(
                            asset = asset,
                            onStatusChange = { newStatus -> onStatusChange(asset, newStatus) },
                            onClick = { onAssetClick(asset) }
                        )
                    }
                }
            }
        }

        // Sliding/Collapsing Top Header layout overlay
        val animatedHeaderHeight = androidx.compose.ui.unit.lerp(headerHeight, minHeaderHeight, collapseProgress)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeaderHeight)
                .background(MaterialTheme.colorScheme.background)
                .testTag("dashboard_header_area")
        ) {
            // Expanded Header Content (Fades out during scroll)
            if (collapseProgress < 1f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(bottom = 16.dp)
                        .graphicsLayer { alpha = 1f - collapseProgress },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    val pagerNestedScrollConnection = remember {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                return Offset(x = available.x, y = 0f)
                            }
                        }
                    }

                    val categoryNamesWithSemua = remember(categoryNames) {
                        listOf("Semua") + categoryNames
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .nestedScroll(pagerNestedScrollConnection)
                            .testTag("row_categories"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(categoryNamesWithSemua) { cat ->
                            val isSelected = if (cat == "Semua") selectedCategory == null else selectedCategory == cat
                            val textAlpha = if (isSelected) 1f else 0.5f
                            val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground

                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                fontWeight = textWeight,
                                color = textColor,
                                modifier = Modifier
                                    .clickable {
                                        if (cat == "Semua") {
                                            selectedCategory = null
                                        } else {
                                            selectedCategory = if (selectedCategory == cat) null else cat
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .graphicsLayer { alpha = textAlpha }
                            )
                        }
                    }
                }
            }

            // Collapsed Header Content (Fades in during scroll)
            if (collapseProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 72.dp)
                        .graphicsLayer { alpha = collapseProgress },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val collapsedTitle = if (selectedCategory == null) "Perangkat" else "Kategori"
                    Text(
                        text = collapsedTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
