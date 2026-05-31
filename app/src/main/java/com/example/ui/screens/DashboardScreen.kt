package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.ui.components.AssetItemCard

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    onScrollAtTopChanged: (Boolean) -> Unit = {},
    // Consolidated actions
    onTabChange: (AppTab) -> Unit = {},
    onExportClick: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    currentUsername: String = "User",
    currentUserRole: String = "Admin"
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
        // 1/3 height area header
        val headerHeight = screenHeight / 3

        val maxScrollOffsetPx = with(density) { headerHeight.toPx() }

        val progressByScroll by remember {
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

        // Notify parent layout
        LaunchedEffect(progressByScroll) {
            onScrollAtTopChanged(progressByScroll == 0f)
        }

        // 3-step transition formula
        // Step 1: Alpha 1f to 0f at 1/3 scroll progress
        val step1Alpha = (1f - (progressByScroll / 0.33f)).coerceIn(0f, 1f)

        // Step 2: Alpha 0f to 1f between 1/3 and 2/3 scroll progress
        val step2Alpha = ((progressByScroll - 0.33f) / 0.33f).coerceIn(0f, 1f)

        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_column"),
            contentPadding = PaddingValues(
                bottom = searchBarTopDp + 24.dp
            )
        ) {
            // First item: Expanded Header Area (1/3 of screen height) containing statistics and categories and fades out on Step 1.
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .graphicsLayer { alpha = step1Alpha },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
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
                            "$activeCount perangkat aktif"
                        } else {
                            "$activeCount $selectedCategory aktif"
                        }

                        // Big Stats Header (Weight is FontWeight.Normal per instructions)
                        Text(
                            text = activeHeaderText,
                            fontFamily = FontFamily.Default,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp)) // Sangat mepet per instruksi

                        val categoryNamesWithSemua = remember(categoryNames) {
                            listOf("Semua") + categoryNames
                        }

                        val pagerNestedScrollConnection = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    return Offset(x = available.x, y = 0f)
                                }
                            }
                        }

                        // Sub-header Categories formatted as: "Kategori 1 • Kategori 2" with light font weight
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(pagerNestedScrollConnection)
                                .testTag("row_categories"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(categoryNamesWithSemua) { index, cat ->
                                val isSelected = if (cat == "Semua") selectedCategory == null else selectedCategory == cat
                                val textAlpha = if (isSelected) 1f else 0.5f
                                val textWeight = if (isSelected) FontWeight.Medium else FontWeight.Light
                                val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground

                                Text(
                                    text = cat,
                                    fontFamily = FontFamily.Default,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = textWeight
                                    ),
                                    color = textColor,
                                    modifier = Modifier
                                        .clickable {
                                            selectedCategory = if (cat == "Semua") null else (if (selectedCategory == cat) null else cat)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                        .graphicsLayer { alpha = textAlpha }
                                )

                                if (index < categoryNamesWithSemua.lastIndex) {
                                    Text(
                                        text = "  •  ",
                                        fontFamily = FontFamily.Default,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraLight
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Header containing Left Title + action buttons
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("control_action_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Title fading in on Step 2
                    // Padded precisely by start = 20.dp (so total horizontal starts at 16.dp + 20.dp = 36.dp matching card's Asset Name)
                    val collapsedTitle = if (selectedCategory == null) "Perangkat" else selectedCategory!!
                    Text(
                        text = collapsedTitle,
                        fontFamily = FontFamily.Default,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .graphicsLayer { alpha = step2Alpha }
                            .padding(start = 20.dp)
                    )

                    // Right side: Quick actions + Consolidated Dropdown Menu Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.width(260.dp)
                            ) {
                                // User Profile Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Halo, $currentUsername!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Role: $currentUserRole",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Navigations (Drawer Consolidation)
                                DropdownMenuItem(
                                    text = { Text("Dasbor & Aset (Beranda)") },
                                    onClick = {
                                        menuExpanded = false
                                        onTabChange(AppTab.Dashboard)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Perbaikan") },
                                    onClick = {
                                        menuExpanded = false
                                        onTabChange(AppTab.Perbaikan)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Perawatan Rutin") },
                                    onClick = {
                                        menuExpanded = false
                                        onTabChange(AppTab.Perawatan)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Manajemen User") },
                                    onClick = {
                                        menuExpanded = false
                                        onTabChange(AppTab.UserManagement)
                                    },
                                    leadingIcon = { Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Manajemen Kategori") },
                                    onClick = {
                                        menuExpanded = false
                                        onTabChange(AppTab.CategoryManagement)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Filter Action
                                DropdownMenuItem(
                                    text = { Text("Tampilkan Semua Kategori") },
                                    onClick = {
                                        menuExpanded = false
                                        selectedCategory = null
                                    },
                                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Export actions
                                DropdownMenuItem(
                                    text = { Text("Ekspor PDF Inventaris") },
                                    onClick = {
                                        menuExpanded = false
                                        onExportClick("assets")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Ekspor PDF Perbaikan") },
                                    onClick = {
                                        menuExpanded = false
                                        onExportClick("repairs")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Ekspor PDF Perawatan") },
                                    onClick = {
                                        menuExpanded = false
                                        onExportClick("maintenances")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Ekspor Log Excel") },
                                    onClick = {
                                        menuExpanded = false
                                        onExportClick("excel")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // DB status
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Database: " + if (isFirebaseEnabled) "Firebase Cloud" else "Local SQLite",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Default
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Logout Info
                                DropdownMenuItem(
                                    text = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onLogout()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            // Empty state or elements list
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
                                fontFamily = FontFamily.Default,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Silakan ketik nomor inventaris baru di search bar untuk mendaftar.",
                                fontFamily = FontFamily.Default,
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
    }
}
