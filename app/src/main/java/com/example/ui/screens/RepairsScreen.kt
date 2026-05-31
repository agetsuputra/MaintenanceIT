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
import androidx.compose.ui.zIndex
import com.example.data.model.Asset
import com.example.data.model.Repair
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val SHOW_DEBUG_LINES = false

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    searchBarTopDp: Dp = 56.dp,
    onExportClick: () -> Unit = {},
    onScrollAtTopChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val defaultCategoryNames = remember {
        listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Slots for dynamic category menu layout
    var visibleSlot2 by remember {
        mutableStateOf("Laptop")
    }
    var visibleSlot3 by remember {
        mutableStateOf("PC Desktop")
    }

    val visibleItems = remember(visibleSlot2, visibleSlot3) {
        listOf("Semua", visibleSlot2, visibleSlot3)
    }

    val hiddenCategories = remember(defaultCategoryNames, visibleSlot2, visibleSlot3) {
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

    val filteredRepairs = remember(repairs, assets, selectedCategory) {
        repairs.filter { repair ->
            val matchedAsset = assets.find { it.inventoryNumber == repair.inventoryNumber }
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
            .testTag("repairs_screen")
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
        val step1Alpha = (1f - (progress / 0.33f)).coerceIn(0f, 1f)
        val step2Alpha = ((progress - 0.33f) / 0.33f).coerceIn(0f, 1f)

        // LazyColumn containing ONLY list items and spacer areas for absolute fluid scroll isolation
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("repairs_column"),
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

            // Repair list logic
            if (filteredRepairs.isEmpty()) {
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
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tidak ada perbaikan pada rentang ini",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredRepairs, key = { it.id }) { repair ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        RepairItemCard(
                            repair = repair,
                            assets = assets,
                            onClick = { onRepairClick(repair) }
                        )
                    }
                }
            }

            // Dynamic bottom Spacer tracking total searchbar area adaptively, ensuring consistent spacing under the last card
            item {
                val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                Spacer(modifier = Modifier.height(searchBarTopDp + 14.dp + keyboardHeight))
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
                    Text(
                        text = "Laporan Perbaikan Kerusakan",
                        fontFamily = FontFamily.Default,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer { alpha = step1Alpha }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Daftar Perbaikan (${filteredRepairs.size})",
                        fontFamily = FontFamily.Default,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraLight
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
            val collapsedTitle = if (selectedCategory == null) "Perbaikan" else selectedCategory!!

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
                    onClick = onAddRepairClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_list_add_repair")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Perbaikan Baru",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_list_export_repairs")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Ekspor Laporan Perbaikan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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

        if (SHOW_DEBUG_LINES) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .offset(y = statusBarHeightDp)
                        .background(Color.Red.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .offset(y = headerHeightDp)
                        .background(Color.Red.copy(alpha = 0.5f))
                )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDateRangePickerDialog(
    initialStartDate: Long,
    initialEndDate: Long,
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val safeStartDate = minOf(initialStartDate, initialEndDate)
    val safeEndDate = maxOf(initialStartDate, initialEndDate)
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = safeStartDate,
        initialSelectedEndDateMillis = safeEndDate
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
    val (assetName, assetType) = remember(repair.inventoryNumber, assets) {
        val matchedAsset = assets.find { it.inventoryNumber == repair.inventoryNumber }
        Pair(
            matchedAsset?.name ?: "Perangkat Tidak Dikenal",
            matchedAsset?.type ?: "Lainnya"
        )
    }
    
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
                        text = "${repair.inventoryNumber}  -  $assetType",
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
