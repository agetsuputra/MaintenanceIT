package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import com.example.data.model.Maintenance
import androidx.compose.ui.zIndex
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MaintenanceScreen(
    maintenances: List<Maintenance>,
    assets: List<Asset>,
    startDate: Long,
    endDate: Long,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onMaintClick: (Maintenance) -> Unit,
    onAddMaintClick: () -> Unit,
    searchBarTopDp: Dp = 56.dp,
    onExportClick: () -> Unit = {},
    onScrollAtTopChanged: (Boolean) -> Unit = {},
    onTabChange: (AppTab) -> Unit = {},
    onLogout: () -> Unit = {},
    currentUsername: String = "User",
    currentUserRole: String = "Teknisi"
) {
    val context = LocalContext.current
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val defaultCategoryNames = remember {
        listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    var visibleSlot2 by remember { mutableStateOf("Laptop") }
    var visibleSlot3 by remember { mutableStateOf("PC Desktop") }

    val visibleItems = remember(selectedCategory, visibleSlot2, visibleSlot3) {
        listOf("Semua", visibleSlot2, visibleSlot3)
    }

    val hiddenCategories = remember(visibleSlot2, visibleSlot3) {
        defaultCategoryNames.filter { it != visibleSlot2 && it != visibleSlot3 }
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

    val filteredMaintenances = remember(maintenances, assets, selectedCategory) {
        maintenances.filter { maint ->
            val matchedAsset = assets.find { it.inventoryNumber == maint.inventoryNumber }
            val assetType = matchedAsset?.type ?: "Lainnya"
            selectedCategory == null || assetType.equals(selectedCategory, ignoreCase = true)
        }
    }

    val scrollState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("maintenances_screen")
    ) {
        val screenHeight = maxHeight
        val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cleanHeightDp = screenHeight - statusBarHeightDp
        
        // Header height area representing the upper 1/3 of the clean screen
        val headerHeightDp = statusBarHeightDp + (cleanHeightDp / 3)

        // 10.dp is the offset calibration to align the 36.dp button's top edge to Garis 2 (Garis C)
        val boxControlDefaultTop = headerHeightDp - 10.dp
        val maxScrollOffset = (boxControlDefaultTop - statusBarHeightDp).coerceAtLeast(0.dp)

        val scrollOffsetDp = if (scrollState.firstVisibleItemIndex > 0) {
            maxScrollOffset
        } else {
            with(density) { scrollState.firstVisibleItemScrollOffset.toDp() }
        }.coerceAtMost(maxScrollOffset)

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
        val step1Alpha = (1f - (progress / 0.33f)).coerceIn(0f, 1f)
        val step2Alpha = ((progress - 0.33f) / 0.33f).coerceIn(0f, 1f)

        val clipShape = remember(boxControlTopDp) {
            object : androidx.compose.ui.graphics.Shape {
                override fun createOutline(
                    size: androidx.compose.ui.geometry.Size,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    density: androidx.compose.ui.unit.Density
                ): androidx.compose.ui.graphics.Outline {
                    val topY = with(density) { (boxControlTopDp + 56.dp).toPx() }
                    return androidx.compose.ui.graphics.Outline.Rectangle(
                        androidx.compose.ui.geometry.Rect(
                            left = 0f,
                            top = topY,
                            right = size.width,
                            bottom = size.height
                        )
                    )
                }
            }
        }

        // LazyColumn containing ONLY list items and spacer areas for absolute fluid scroll isolation
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = clipShape
                }
                .testTag("maintenances_column"),
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

            // Routine Maintenance list logic
            if (filteredMaintenances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tidak ada perawatan pada rentang ini",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredMaintenances, key = { it.id }) { maint ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        MaintItemCard(
                            maintenance = maint,
                            assets = assets,
                            onClick = { onMaintClick(maint) }
                        )
                    }
                }
            }

            // Dynamic bottom Spacer tracking total searchbar area adaptively, ensuring consistent spacing under the last card
            item {
                val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                val firstSpacerHeight = boxControlDefaultTop + 56.dp - 14.dp
                val N = filteredMaintenances.size
                val targetTotalHeight = screenHeight + maxScrollOffset
                val nativeBottomSpacer = if (N == 0) {
                    val emptyStateHeight = 140.dp
                    targetTotalHeight - firstSpacerHeight - emptyStateHeight - 28.dp
                } else {
                    targetTotalHeight - firstSpacerHeight - (N * 76).dp - ((N + 1) * 14).dp
                }
                
                val minBottomSpacer = (searchBarTopDp + 14.dp + keyboardHeight)
                val bottomSpacerHeight = nativeBottomSpacer.coerceAtLeast(minBottomSpacer)
                
                Spacer(modifier = Modifier.height(bottomSpacerHeight))
            }
        }

        // Sticky Header Overlay (isolated from overscroll elastic pull and colored solid)
        val stickyHeaderTopDp = (headerHeightDp - scrollOffsetDp).coerceAtLeast(statusBarHeightDp)

        // Opaque solid background cover for Status Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stickyHeaderTopDp)
                .background(MaterialTheme.colorScheme.background)
                .zIndex(2f)
        )

        // Header Area overlay with solid non-transparent background to cut off scrolling cards cleanly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeightDp)
                .offset(y = -scrollOffsetDp)
                .background(MaterialTheme.colorScheme.background)
                .zIndex(3f)
        ) {
            // Central Title aligned vertically centered relative to Garis 3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cleanHeightDp / 3)
                    .offset(y = statusBarHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    val totalPerawatan = filteredMaintenances.size
                    val namaKategori = if (selectedCategory == null) "perawatan rutin" else "perawatan ${selectedCategory!!.lowercase()}"
                    val dynamicTitle = "$totalPerawatan $namaKategori"

                    Text(
                        text = dynamicTitle,
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
            }

            // Categories list and interactive menu centered below the title text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = statusBarHeightDp + (cleanHeightDp / 6f) + 16.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = step1Alpha }
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
                                    .align(Alignment.CenterVertically)
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
                                        .offset(y = 1.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Box Kontrol Dinamis (Sticky Header setinggi 56.dp)
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
            val collapsedTitle = if (selectedCategory == null) "Perawatan" else selectedCategory!!

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

            // Right side: Quick actions
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onAddMaintClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_list_add_maintenance")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Perawatan Baru",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { showDateRangePicker = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_list_date_range_maint")
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Pilih Rentang Tanggal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_list_more_menu_maint")
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
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ekspor Laporan (PDF)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                menuExpanded = false
                                onExportClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("menu_export_maint_pdf")
                        )
                    }
                }
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
}

@Composable
fun MaintItemCard(maintenance: Maintenance, assets: List<Asset>, onClick: () -> Unit) {
    val (assetName, assetType) = remember(maintenance.inventoryNumber, assets) {
        val matchedAsset = assets.find { it.inventoryNumber == maintenance.inventoryNumber }
        Pair(
            matchedAsset?.name ?: "Perangkat Tidak Dikenal",
            matchedAsset?.type ?: "Lainnya"
        )
    }

    val cleanStatus = remember(maintenance.status) {
        if (maintenance.status == "Selesai & Terverifikasi") "Terverifikasi" else maintenance.status
    }

    val statusColor = when (cleanStatus) {
        "Terverifikasi", "Selesai" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val cardColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clickable { onClick() }
            .testTag("maint_card_${maintenance.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Row Atas: assetName di kiri dan maintenance.status polos di kanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assetName,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cleanStatus,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            // Row Bawah: maintenance.actionTaken di kiri [weight 0.33f] dan maintenance.technician di kanan [weight 0.67f]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = maintenance.actionTaken,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.33f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = maintenance.technician,
                    fontWeight = FontWeight.Light,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.67f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
