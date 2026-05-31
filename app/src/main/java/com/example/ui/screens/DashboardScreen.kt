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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.zIndex
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
        val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cleanHeightDp = screenHeight - statusBarHeightDp
        
        // Header height area representing the upper 1/3 of the clean screen
        val headerHeightDp = statusBarHeightDp + (cleanHeightDp / 3)

        val scrollOffsetDp = if (scrollState.firstVisibleItemIndex > 0) {
            headerHeightDp
        } else {
            with(density) { scrollState.firstVisibleItemScrollOffset.toDp() }
        }

        val totalScrollRangeDp = cleanHeightDp / 3
        val progress = if (totalScrollRangeDp > 0.dp) {
            (scrollOffsetDp / totalScrollRangeDp).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Notify parent layout if the list is scrolled to the top
        LaunchedEffect(progress) {
            onScrollAtTopChanged(progress == 0f)
        }

        // 3-step transition formula
        // Step 1: Alpha 1f to 0f at 1/3 scroll progress
        val step1Alpha = (1f - (progress / 0.33f)).coerceIn(0f, 1f)

        // Step 2: Alpha 0f to 1f between 1/3 and 2/3 scroll progress
        val step2Alpha = ((progress - 0.33f) / 0.33f).coerceIn(0f, 1f)

        // LazyColumn containing ONLY list items and spacer areas for absolute fluid scroll isolation
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_column"),
            contentPadding = PaddingValues(
                bottom = searchBarTopDp + 24.dp
            )
        ) {
            // Spacer to reserve exact vertical height for the header area
            item {
                Spacer(modifier = Modifier.height(headerHeightDp))
            }
            // Spacer to reserve exact vertical height for the action bar
            item {
                Spacer(modifier = Modifier.height(56.dp))
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
                                fontWeight = FontWeight.Normal,
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
                    // Raised vertical padding between elements from 6.dp to 10.dp for more breathing space
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        AssetItemCard(
                            asset = asset,
                            onStatusChange = { newStatus -> onStatusChange(asset, newStatus) },
                            onClick = { onAssetClick(asset) }
                        )
                    }
                }
            }
        }

        // Header Area overlay containing centered main statistics and spacing-optimized category LazyRow
        // Top offset moves up natively with scroll (-scrollOffsetDp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeightDp)
                .offset(y = -scrollOffsetDp)
                .graphicsLayer { alpha = step1Alpha }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cleanHeightDp / 3)
                    .padding(top = statusBarHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                        "$activeCount ${selectedCategory?.lowercase()} aktif"
                    }

                    // Centered stats title text centered vertically and horizontally relative to Garis 3
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

                    Spacer(modifier = Modifier.height(2.dp)) // Tightly packed per instructions

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

                    // Tightly-spaced Category Sub-header with horizontal padding 24.dp and spacedBy 12.dp
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .nestedScroll(pagerNestedScrollConnection)
                            .testTag("row_categories"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(categoryNamesWithSemua) { index, cat ->
                            val isSelected = if (cat == "Semua") selectedCategory == null else selectedCategory == cat
                            val textAlpha = if (isSelected) 1f else 0.5f
                            val textWeight = if (isSelected) FontWeight.Medium else FontWeight.ExtraLight
                            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground

                            Text(
                                text = cat,
                                fontFamily = FontFamily.Default,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = textWeight
                                ),
                                color = textColor,
                                modifier = Modifier
                                    .clickable {
                                        selectedCategory = if (cat == "Semua") null else (if (selectedCategory == cat) null else cat)
                                    }
                                    .padding(vertical = 4.dp)
                                    .graphicsLayer { alpha = textAlpha }
                            )

                            if (index < categoryNamesWithSemua.lastIndex) {
                                Text(
                                    text = "•",
                                    fontFamily = FontFamily.Default,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraLight
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Header Overlay (isolated from overscroll elastic pull)
        // Starts exactly at Garis 2 (headerHeightDp) and scrolls up capped at Garis 1 (statusBarHeightDp)
        val stickyHeaderTopDp = (headerHeightDp - scrollOffsetDp).coerceAtLeast(statusBarHeightDp)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = stickyHeaderTopDp)
                .background(MaterialTheme.colorScheme.background)
                .testTag("control_action_row"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val collapsedTitle = if (selectedCategory == null) "Perangkat" else selectedCategory!!
            
            // Fades in from alpha 0f to 1f between 1/3 and 2/3 scroll distance
            // Aligned perfectly vertically with Asset Name inside Card (horizontal padding 36.dp)
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
                    .padding(start = 36.dp)
            )

            // Right side: Quick actions + Consolidated Dropdown Menu Trigger
            Row(
                modifier = Modifier.padding(end = 16.dp),
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

        // Temporary semi-transparent red guideline overlays for visual alignment precision
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            // Garis 1 (Status Bar bottom edge boundary)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = statusBarHeightDp)
                    .background(Color.Red.copy(alpha = 0.5f))
            )
            // Garis 2 (Garis 1/3 Atas of screen height area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = headerHeightDp)
                    .background(Color.Red.copy(alpha = 0.5f))
            )
            // Garis 3 (Garis Tengah Header, exactly vertical center of clean status bar space and 1/3 region)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .offset(y = statusBarHeightDp + (cleanHeightDp / 6f))
                    .background(Color.Red.copy(alpha = 0.5f))
            )
        }
    }
}
