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

private const val SHOW_DEBUG_LINES = true

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
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    // Slots for dynamic category menu layout
    var visibleSlot2 by remember(categoryNames) {
        mutableStateOf(categoryNames.getOrNull(0) ?: "Laptop")
    }
    var visibleSlot3 by remember(categoryNames) {
        mutableStateOf(categoryNames.getOrNull(1) ?: "PC Desktop")
    }

    val visibleItems = remember(visibleSlot2, visibleSlot3) {
        listOf("Semua", visibleSlot2, visibleSlot3)
    }

    val hiddenCategories = remember(categoryNames, visibleSlot2, visibleSlot3) {
        categoryNames.filter { it != visibleSlot2 && it != visibleSlot3 }
    }

    val onCategoryClick = { cat: String ->
        if (cat == "Semua") {
            selectedCategory = null
            categoryMenuExpanded = false
        } else {
            if (categoryMenuExpanded) {
                if (cat == visibleSlot3) {
                    val temp = visibleSlot2
                    visibleSlot2 = visibleSlot3
                    visibleSlot3 = temp
                } else if (cat != visibleSlot2) {
                    visibleSlot2 = cat
                }
            }
            selectedCategory = cat
            categoryMenuExpanded = false
        }
    }

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

        // 10.dp is the offset calibration to align the 36.dp button's top edge to Garis 2 (Garis C)
        val boxControlDefaultTop = headerHeightDp - 10.dp
        val boxControlTopDp = (boxControlDefaultTop - scrollOffsetDp).coerceAtLeast(statusBarHeightDp)

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
                top = 0.dp,
                bottom = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Spacer height is dynamically calculated to match the bottom of Box Kontrol at default scroll.
            // Subtracting the 14.dp spacing added by verticalArrangement guarantees the first Card
            // starts perfectly aligned with the bottom of Box Kontrol with no gap!
            item {
                Spacer(modifier = Modifier.height(boxControlDefaultTop + 56.dp - 14.dp))
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
                    // Corrected vertical padding to 0.dp to allow verticalArrangement = Arrangement.spacedBy(8.dp) to fully govern card spacing
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AssetItemCard(
                            asset = asset,
                            onStatusChange = { newStatus -> onStatusChange(asset, newStatus) },
                            onClick = { onAssetClick(asset) }
                        )
                    }
                }
            }

            // Dynamic bottom Spacer tracking system/IME insets to ensure the bottom card remains 14.dp above the searchbar
            item {
                val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                Spacer(modifier = Modifier.height(searchBarTopDp + 14.dp + keyboardHeight))
            }
        }

        // State and Category slots moved and declared at the top of Composable to prevent duplication and facilitate onCategoryClick

        // Sticky Header Overlay (isolated from overscroll elastic pull and colored solid)
        // Starts exactly at Garis 2 (headerHeightDp) and scrolls up capped at Garis 1 (statusBarHeightDp)
        val stickyHeaderTopDp = (headerHeightDp - scrollOffsetDp).coerceAtLeast(statusBarHeightDp)

        // 1. Opaque solid background cover for HP Status Bar and Header region to prevent visual leaks (0 to stickyHeaderTopDp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stickyHeaderTopDp)
                .background(MaterialTheme.colorScheme.background)
                .zIndex(2f)
        )

        // Header Area overlay with solid non-transparent background to cut off scrolling cards cleanly
        // Top offset moves up natively with scroll (-scrollOffsetDp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeightDp)
                .offset(y = -scrollOffsetDp)
                .background(MaterialTheme.colorScheme.background)
                .zIndex(3f)
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

            // Central Stats Header aligned vertically centered relative to Garis 3 (exactly center of upper room)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cleanHeightDp / 3)
                    .offset(y = statusBarHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeHeaderText,
                    fontFamily = FontFamily.Default,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = step1Alpha }
                )
            }

            // Categories list and interactive menu centered 16.dp below Center of Garis 3 (for tighter visual grouping)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = statusBarHeightDp + (cleanHeightDp / 6f) + 16.dp) // tighter and more compact gap below header text
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = step1Alpha } // entire subheader (including Row & FlowRow) fades out dynamically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("row_categories")
                ) {
                    visibleItems.forEachIndexed { index, cat ->
                        val isSelected = if (cat == "Semua") selectedCategory == null else selectedCategory == cat
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
                                    onCategoryClick(cat)
                                }
                                .padding(vertical = 4.dp)
                        )

                        if (index < visibleItems.lastIndex) {
                            Text(
                                text = "•",
                                fontFamily = FontFamily.Default,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraLight
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }

                    // Bullet separator before expansion '•••'
                    Text(
                        text = "•",
                        fontFamily = FontFamily.Default,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraLight
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    // Expansion clickable indicator
                    Text(
                        text = if (categoryMenuExpanded) "▲" else "•••",
                        fontFamily = FontFamily.Default,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraLight
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { categoryMenuExpanded = !categoryMenuExpanded }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("btn_expand_categories")
                    )
                }

                // Inline expansion of remaining categories
                if (categoryMenuExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        hiddenCategories.forEachIndexed { idx, cat ->
                            val isSelected = selectedCategory == cat
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
                                        onCategoryClick(cat)
                                    }
                                    .padding(vertical = 4.dp)
                                    .align(Alignment.CenterVertically) // align categories centered aligned in FlowRow line
                            )

                            if (idx < hiddenCategories.lastIndex) {
                                Text(
                                    text = "•",
                                    fontFamily = FontFamily.Default,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraLight
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .offset(y = 1.dp) // shift down for pixel-perfect vertical centering
                                        .align(Alignment.CenterVertically) // center alignment for bullet separator
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = boxControlTopDp)
                .background(MaterialTheme.colorScheme.background)
                .testTag("control_action_row")
                .zIndex(4f),
            contentAlignment = Alignment.CenterStart
        ) {
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
                    .padding(start = 36.dp)
                    .align(Alignment.CenterStart)
            )

            // Right side: Quick actions aligned CenterVertically to match high design layout
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
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
                                categoryMenuExpanded = false
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

        if (SHOW_DEBUG_LINES) {
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
}
