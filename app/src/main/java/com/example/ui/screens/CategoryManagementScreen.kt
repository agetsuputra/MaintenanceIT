package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.Category

@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onScrollAtTopChanged: (Boolean) -> Unit = {},
    onTabChange: (AppTab) -> Unit = {},
    onLogout: () -> Unit = {},
    currentUsername: String = "User",
    currentUserRole: String = "Admin",
    searchBarTopDp: androidx.compose.ui.unit.Dp = 80.dp
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
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

        // 10.dp is the offset calibration
        val boxControlDefaultTop = headerHeightDp - 10.dp
        val maxScrollOffset = (boxControlDefaultTop - statusBarHeightDp).coerceAtLeast(0.dp)

        val scrollOffsetDp = if (listState.firstVisibleItemIndex > 0) {
            maxScrollOffset
        } else {
            with(density) { listState.firstVisibleItemScrollOffset.toDp() }
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = clipShape
                }
                .testTag("category_management_screen"),
            contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(boxControlDefaultTop + 56.dp - 14.dp))
            }

            if (categories.isEmpty()) {
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
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tidak ada kategori terdaftar",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(categories, key = { it.name }) { category ->
                    val itemsCount = if (category.guidelines.isBlank()) 0 else category.guidelines.split("||~||").size
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clickable { editingCategory = category }
                                .testTag("category_card_${category.name}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = category.name,
                                        fontWeight = FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "$itemsCount panduan perawatan",
                                        fontWeight = FontWeight.Light,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                val firstSpacerHeight = boxControlDefaultTop + 56.dp - 14.dp
                val N = categories.size
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
                    val dynamicTitle = "${categories.size} kategori"

                    Text(
                        text = dynamicTitle,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.graphicsLayer { alpha = step1Alpha }
                    )
                }
            }
        }

        // Action Control Row (Sticky Header)
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
            Text(
                text = "Manajemen Kategori",
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
 
            // Right side: Quick actions aligned CenterVertically
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Kategori",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Lainnya",
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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("Log Out") },
                            onClick = {
                                menuExpanded = false
                                onLogout()
                            },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }

        // Full-Page Card Form for Add Category
        if (showAddDialog) {
            CategoryEditorForm(
                category = null,
                onDismiss = { showAddDialog = false },
                onSave = { savedCategory ->
                    onSaveCategory(savedCategory)
                    showAddDialog = false
                },
                onDelete = null
            )
        }

        // Full-Page Card Form for Edit Category
        editingCategory?.let { category ->
            val isReadOnly = listOf("Laptop", "PC Desktop", "Printer", "Server", "Network Device", "Lainnya")
                .contains(category.name)
            CategoryEditorForm(
                category = category,
                onDismiss = { editingCategory = null },
                onSave = { savedCategory ->
                    onSaveCategory(savedCategory)
                    editingCategory = null
                },
                onDelete = if (!isReadOnly) {
                    {
                        onDeleteCategory(category)
                        editingCategory = null
                    }
                } else null
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorForm(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    val items = remember {
        val loaded = category?.guidelines?.split("||~||")?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateListOf<String>().apply { 
            if (loaded.isNotEmpty()) addAll(loaded) else add("") 
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current

    val themeBgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val view = androidx.compose.ui.platform.LocalView.current

    DisposableEffect(themeBgColor, surfaceColor, isDark) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && !view.isInEditMode) {
            window.statusBarColor = surfaceColor.toArgb()
            androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
        onDispose {
            if (window != null && !view.isInEditMode) {
                window.statusBarColor = themeBgColor.toArgb()
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBgColor)
            .statusBarsPadding()
            .zIndex(5f),
        color = themeBgColor
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        // Baris 1: Nama Kategori Input (No Icon, horizontal padding exactly 24.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (name.isEmpty()) {
                                            Text(
                                                "Nama Kategori *",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }

                    item {
                        Text(
                            text = "Daftar Item Pengecekan :",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }

                    items(items.size) { index ->
                        var cumulativeDrag by remember { mutableStateOf(0f) }
                        val dragThresholdPx = with(density) { 36.dp.toPx() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 64.dp)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle Icon (2 horizontal lines)
                            DragHandleIcon(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .wrapContentSize(Alignment.Center)
                                    .pointerInput(index) {
                                        detectDragGestures(
                                            onDragStart = { cumulativeDrag = 0f },
                                            onDragEnd = { cumulativeDrag = 0f },
                                            onDragCancel = { cumulativeDrag = 0f },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                cumulativeDrag += dragAmount.y
                                                if (cumulativeDrag < -dragThresholdPx && index > 0) {
                                                    val temp = items[index]
                                                    items[index] = items[index - 1]
                                                    items[index - 1] = temp
                                                    cumulativeDrag = 0f
                                                } else if (cumulativeDrag > dragThresholdPx && index < items.size - 1) {
                                                    val temp = items[index]
                                                    items[index] = items[index + 1]
                                                    items[index + 1] = temp
                                                    cumulativeDrag = 0f
                                                }
                                            }
                                        )
                                    }
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            BasicTextField(
                                value = items[index],
                                onValueChange = { items[index] = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                singleLine = false,
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (items[index].isEmpty()) {
                                            Text(
                                                "Masukkan poin panduan...",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = { 
                                    if (items.size > 1) {
                                        items.removeAt(index)
                                    } else {
                                        items[0] = ""
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus panduan",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 24.dp)
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { items.add("") }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tambah item baru",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // High-fidelity flat action buttons pinned seamlessly at the bottom edge (0-pixel gap with keyboard/screen edge) matching UserEditorForm style exactly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .background(themeBgColor)
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 0.dp)
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeBgColor,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("btn_cancel_category")
                ) {
                    Text(
                        text = "Batal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete option for editing category
                if (onDelete != null) {
                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        border = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeBgColor,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("btn_delete_category")
                    ) {
                        Text(
                            text = "Hapus",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Save button
                val isFormValid = name.isNotBlank() && items.any { it.isNotBlank() }
                Button(
                    onClick = {
                        val filteredGuidelines = items.map { it.trim() }.filter { it.isNotBlank() }
                        if (name.isBlank()) {
                            Toast.makeText(context, "Harap isi nama kategori!", Toast.LENGTH_SHORT).show()
                        } else if (filteredGuidelines.isEmpty()) {
                            Toast.makeText(context, "Harap isi minimal 1 panduan / hal yang diperhatikan!", Toast.LENGTH_SHORT).show()
                        } else {
                            onSave(
                                Category(
                                    name = name.trim(),
                                    guidelines = filteredGuidelines.joinToString("||~||")
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = null,
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeBgColor,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = themeBgColor,
                        disabledContentColor = if (isDark) Color(0xFF555555) else Color(0xFFB0B0B0)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("btn_save_category")
                ) {
                    Text(
                        text = "Simpan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DragHandleIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val y1 = size.height * 0.38f
        val y2 = size.height * 0.62f
        val strokePx = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, y1),
            end = Offset(size.width, y1),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = Offset(0f, y2),
            end = Offset(size.width, y2),
            strokeWidth = strokePx
        )
    }
}
