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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.User
import com.example.util.showBiometricPrompt

@Composable
fun UserManagementScreen(
    users: List<User>,
    onSaveUser: (User) -> Unit,
    onDeleteUser: (User) -> Unit,
    onExportAllLogs: () -> Unit = {},
    onScrollAtTopChanged: (Boolean) -> Unit = {},
    onTabChange: (AppTab) -> Unit = {},
    onLogout: () -> Unit = {},
    currentUsername: String = "User",
    currentUserRole: String = "Admin",
    searchBarTopDp: androidx.compose.ui.unit.Dp = 80.dp
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    var editingUser by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val cardColor = com.example.ui.theme.AdaptiveColors.cardColorMedium()
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
                .testTag("user_management_screen"),
            contentPadding = PaddingValues(top = 0.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(boxControlDefaultTop + 56.dp - 14.dp))
            }

            if (users.isEmpty()) {
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
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Tidak ada user terdaftar",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(users, key = { it.username }) { user ->
                    val displayRole = when (user.role) {
                        "Kepala Unit IT" -> "Kepala Unit"
                        "Staff IT" -> "Administrator"
                        "Teknisi" -> "Teknisi"
                        else -> user.role
                    }
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clickable { editingUser = user }
                                .testTag("user_card_${user.username}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "${user.username} \u2022 $displayRole",
                                        fontWeight = FontWeight.Light,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
 
                                if (user.isBiometricEnabled) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = "Sidik Jari Aktif",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
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
                val N = users.size
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
                    val dynamicTitle = "${users.size} user"

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
                text = "Manajemen User",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .graphicsLayer { alpha = step2Alpha }
                    .padding(start = 32.dp)
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
                        contentDescription = "Tambah User",
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
                            text = { Text("Ekspor Semua Log") },
                            onClick = {
                                menuExpanded = false
                                onExportAllLogs()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )

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

        // Full-Page Card Form for Add User
        if (showAddDialog) {
            UserEditorForm(
                user = null,
                onDismiss = { showAddDialog = false },
                onSave = { savedUser ->
                    onSaveUser(savedUser)
                    showAddDialog = false
                },
                onDelete = null,
                activity = activity
            )
        }

        // Full-Page Card Form for Edit User
        editingUser?.let { user ->
            UserEditorForm(
                user = user,
                onDismiss = { editingUser = null },
                onSave = { savedUser ->
                    onSaveUser(savedUser)
                    editingUser = null
                },
                onDelete = if (user.username != "sumayasa" && user.username != "deaget") {
                    {
                        onDeleteUser(user)
                        editingUser = null
                    }
                } else null,
                activity = activity
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditorForm(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    onDelete: (() -> Unit)?,
    activity: androidx.fragment.app.FragmentActivity?
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var name by remember { mutableStateOf(user?.name ?: "") }
    var pin by remember { mutableStateOf(user?.pin ?: "") }
    val initialRole = when (user?.role) {
        "Kepala Unit IT" -> "Kepala Unit"
        "Staff IT" -> "Administrator"
        "Teknisi" -> "Teknisi"
        else -> "Administrator"
    }
    var role by remember { mutableStateOf(initialRole) }
    var isBiometricEnabled by remember { mutableStateOf(user?.isBiometricEnabled ?: false) }

    val roleOptions = listOf("Kepala Unit", "Administrator", "Teknisi")
    val context = LocalContext.current

    var roleExpanded by remember { mutableStateOf(false) }

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
                        // Baris 1 (Topmost): Nama Lengkap Input (No Icon, horizontal padding exactly 24.dp to line up with icons beneath it)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .padding(start = 24.dp, end = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = {
                                    Text(
                                        "Nama Lengkap *",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
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
                        // Baris 2: Username Input with Outlined.Badge Icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            TextField(
                                value = username,
                                onValueChange = { if (user == null) username = it.trim().lowercase() },
                                placeholder = {
                                    Text(
                                        "Username *",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                enabled = user == null,
                                textStyle = MaterialTheme.typography.bodyLarge,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
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
                        // Baris 3: PIN Input with Outlined.Lock Icon & NumberPassword Keyboard
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            TextField(
                                value = pin,
                                onValueChange = { newVal ->
                                    if (newVal.all { it.isDigit() } && newVal.length <= 6) {
                                        pin = newVal
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "PIN (6 Digit) *",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
                        // Baris 4: Hak Akses Selector with Outlined.ManageAccounts Icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { roleExpanded = !roleExpanded }
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ManageAccounts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = if (role.isNotBlank()) role else "Hak Akses *",
                                color = if (role.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // Inline Expandable Dropdown with sliding transitions and selected checkmarks
                        AnimatedVisibility(visible = roleExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                            ) {
                                roleOptions.forEach { opt ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                role = opt
                                                roleExpanded = false
                                            }
                                            .padding(start = 66.dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = opt,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (role == opt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (role == opt) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (role == opt) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Terpilih",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
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
                        // Baris 5: Login Biometrik with Outlined.Fingerprint Icon & Switch Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                text = "Login Biometrik",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (activity != null) {
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            showBiometricPrompt(
                                                activity = activity,
                                                title = "Verifikasi Sidik Jari",
                                                subtitle = "Mendaftarkan perangkat sidik jari Anda",
                                                description = "Sentuh sensor sidik jari perangkat Anda untuk memverifikasi.",
                                                onSuccess = {
                                                    isBiometricEnabled = true
                                                    Toast.makeText(context, "Sidik Jari berhasil dikonfigurasi!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    isBiometricEnabled = false
                                                    Toast.makeText(context, "Batal / Gagal menyetel sidik jari: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            isBiometricEnabled = false
                                        }
                                    }
                                )
                            } else {
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = null,
                                    enabled = false
                                )
                            }
                        }
                    }
                }
            }

            // Flat Borderless Buttons pinned seamlessly at the bottom edge (0-pixel gap with keyboard/screen edge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .background(themeBgColor)
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button (Flat Borderless style matching background)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Batal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Quick delete option for editing user
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "Hapus",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Save button
                val isFormValid = username.isNotBlank() && name.isNotBlank() && pin.length == 6
                TextButton(
                    onClick = {
                        if (username.isBlank() || name.isBlank() || pin.length != 6) {
                            Toast.makeText(context, "Harap lengkapi semua isian (PIN harus 6 digit)!", Toast.LENGTH_SHORT).show()
                        } else {
                            val dbRole = when (role) {
                                "Kepala Unit" -> "Kepala Unit IT"
                                "Administrator" -> "Staff IT"
                                "Teknisi" -> "Teknisi"
                                else -> role
                            }
                            onSave(
                                User(
                                    username = username.trim().lowercase(),
                                    name = name.trim(),
                                    pin = pin,
                                    role = dbRole,
                                    isBiometricEnabled = isBiometricEnabled
                                )
                            )
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Text(
                        text = "Simpan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
