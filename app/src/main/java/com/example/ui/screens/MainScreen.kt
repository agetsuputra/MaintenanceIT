package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Asset
import com.example.data.model.Maintenance
import com.example.data.model.Repair
import com.example.ui.components.*
import com.example.ui.viewmodel.ITViewModel
import com.example.util.compressAndWatermarkBitmap
import com.example.util.fetchRealtimeLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab {
    Dashboard,
    Perbaikan,
    Perawatan,
    UserManagement,
    CategoryManagement
}

enum class SubScreen {
    List,
    AddAsset,
    AddRepair,
    AddMaintenance
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ITViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.Dashboard) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.List) }
    var showSplash by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var userMenuExpanded by remember { mutableStateOf(false) }
    var hamburgerMenuExpanded by remember { mutableStateOf(false) }

    val slotMetrics = com.example.util.rememberFloatingSlotMetrics(floatingElementHeight = 56.dp)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // Seed preview database if empty and trigger splash fadeout
    LaunchedEffect(Unit) {
        viewModel.seedSampleDataIfEmpty()
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        delay(1500)
        showSplash = false
    }

    // DB States
    val assets by viewModel.allAssets.collectAsStateWithLifecycle(emptyList())
    val repairs by viewModel.allRepairs.collectAsStateWithLifecycle(emptyList())
    val filteredRepairs by viewModel.filteredRepairs.collectAsStateWithLifecycle(emptyList())
    val filteredMaintenances by viewModel.filteredMaintenances.collectAsStateWithLifecycle(emptyList())

    // Date Filters
    val startDate by viewModel.filterStartDate.collectAsStateWithLifecycle()
    val endDate by viewModel.filterEndDate.collectAsStateWithLifecycle()

    // Dialog & Detail Sheet states
    var showingAssetDetail by remember { mutableStateOf<Asset?>(null) }
    var showingRepairDetail by remember { mutableStateOf<Repair?>(null) }
    var showingMaintenanceDetail by remember { mutableStateOf<Maintenance?>(null) }
    var showingBarcodeScanner by remember { mutableStateOf<((String) -> Unit)?>(null) } // callback function
    var prefilledInventoryNumber by remember { mutableStateOf<String?>(null) }
    var prefilledInventoryForAddAsset by remember { mutableStateOf<String?>(null) }
    var showingUpdateAssetDialog by remember { mutableStateOf<Asset?>(null) }
    
    var fetchedLiveLocation by remember { mutableStateOf("IT Office Desk") }
    var currentCameraCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        fetchRealtimeLocation(context) { locationResult ->
            fetchedLiveLocation = locationResult
        }
    }

    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val currentDateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
            val compressedBase64 = compressAndWatermarkBitmap(
                bitmap = bitmap,
                location = fetchedLiveLocation,
                dateStr = currentDateStr
            )
            val combinedStr = "$compressedBase64|||$fetchedLiveLocation|||$currentDateStr"
            currentCameraCallback?.invoke(combinedStr)
        }
        currentCameraCallback = null
    }

    val systemCameraOpener: ((String) -> Unit) -> Unit = remember {
        { callback ->
            currentCameraCallback = callback
            fetchRealtimeLocation(context) { locationResult ->
                fetchedLiveLocation = locationResult
            }
            try {
                systemCameraLauncher.launch()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var currentUsername by rememberSaveable { mutableStateOf("") }
    var currentUserRole by rememberSaveable { mutableStateOf("") }

    if (showSplash) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.size(130.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "IT Support Service",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Record & Maintenance Hub",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    } else if (currentUsername.isEmpty()) {
        LoginScreen(viewModel = viewModel, onLoginSuccess = { username, role ->
            currentUsername = username
            currentUserRole = role
        })
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "IT Support Service",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Record & Maintenance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Dasbor & Aset", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == AppTab.Dashboard,
                        onClick = {
                            currentTab = AppTab.Dashboard
                            currentSubScreen = SubScreen.List
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_menu_dashboard")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NavigationDrawerItem(
                        label = { Text("Perbaikan", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == AppTab.Perbaikan,
                        onClick = {
                            currentTab = AppTab.Perbaikan
                            currentSubScreen = SubScreen.List
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_menu_perbaikan")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    NavigationDrawerItem(
                        label = { Text("Perawatan Rutin", fontWeight = FontWeight.SemiBold) },
                        selected = currentTab == AppTab.Perawatan,
                        onClick = {
                            currentTab = AppTab.Perawatan
                            currentSubScreen = SubScreen.List
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("drawer_menu_perawatan")
                    )
                }
            },
            gesturesEnabled = false
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Scaffold(
                    modifier = modifier.testTag("main_screen_scaffold"),
                    contentWindowInsets = WindowInsets(0.dp)
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            when (currentTab) {
                                AppTab.Dashboard -> {
                                    when (currentSubScreen) {
                                        SubScreen.List -> {
                                            DashboardScreen(
                                                assets = assets,
                                                repairs = repairs,
                                                isFirebaseEnabled = viewModel.isFirebaseEnabled,
                                                onAssetClick = { asset -> showingAssetDetail = asset },
                                                searchQuery = searchQuery,
                                                searchBarTopDp = slotMetrics.anchorHeight
                                            )
                                        }
                                        SubScreen.AddAsset -> {
                                            val categoryListState = viewModel.allCategories.collectAsStateWithLifecycle(emptyList())
                                            val categoriesNames = categoryListState.value.map { it.name }
                                            AddAssetForm(
                                                onSave = { asset ->
                                                    viewModel.saveAsset(asset) {
                                                        currentSubScreen = SubScreen.List
                                                        prefilledInventoryForAddAsset = null
                                                        showingAssetDetail = asset
                                                        Toast.makeText(context, "Aset berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onCancel = { 
                                                    currentSubScreen = SubScreen.List
                                                    prefilledInventoryForAddAsset = null
                                                },
                                                assetList = assets,
                                                onOpenScanner = { callback -> showingBarcodeScanner = callback },
                                                initialInventoryNumber = prefilledInventoryForAddAsset,
                                                categories = categoriesNames
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                                AppTab.Perbaikan -> {
                                    when (currentSubScreen) {
                                        SubScreen.List -> {
                                            RepairsScreen(
                                                repairs = filteredRepairs,
                                                assets = assets,
                                                startDate = startDate,
                                                endDate = endDate,
                                                onStartDateChange = { viewModel.filterStartDate.value = it },
                                                onEndDateChange = { viewModel.filterEndDate.value = it },
                                                onRepairClick = { showingRepairDetail = it },
                                                onAddRepairClick = { currentSubScreen = SubScreen.AddRepair }
                                            )
                                        }
                                        SubScreen.AddRepair -> {
                                            AddRepairForm(
                                                assetList = assets,
                                                onSave = { repair ->
                                                    viewModel.saveRepair(repair) {
                                                        currentSubScreen = SubScreen.List
                                                        prefilledInventoryNumber = null
                                                        Toast.makeText(context, "Perbaikan berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onCancel = { 
                                                    currentSubScreen = SubScreen.List
                                                    prefilledInventoryNumber = null
                                                },
                                                onOpenScanner = { callback -> showingBarcodeScanner = callback },
                                                onOpenCamera = systemCameraOpener,
                                                initialInventoryNumber = prefilledInventoryNumber,
                                                currentUser = currentUsername
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                                AppTab.Perawatan -> {
                                    when (currentSubScreen) {
                                        SubScreen.List -> {
                                            MaintenanceScreen(
                                                maintenances = filteredMaintenances,
                                                assets = assets,
                                                startDate = startDate,
                                                endDate = endDate,
                                                onStartDateChange = { viewModel.filterStartDate.value = it },
                                                onEndDateChange = { viewModel.filterEndDate.value = it },
                                                onMaintClick = { showingMaintenanceDetail = it },
                                                onAddMaintClick = { currentSubScreen = SubScreen.AddMaintenance }
                                            )
                                        }
                                        SubScreen.AddMaintenance -> {
                                            AddMaintenanceForm(
                                                assetList = assets,
                                                onSave = { maint ->
                                                    viewModel.saveMaintenance(maint) {
                                                        currentSubScreen = SubScreen.List
                                                        prefilledInventoryNumber = null
                                                        Toast.makeText(context, "Pencatatan perawatan rutin disimpan!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onCancel = { 
                                                    currentSubScreen = SubScreen.List
                                                    prefilledInventoryNumber = null
                                                },
                                                onOpenScanner = { callback -> showingBarcodeScanner = callback },
                                                onOpenCamera = systemCameraOpener,
                                                initialInventoryNumber = prefilledInventoryNumber,
                                                currentUser = currentUsername
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                                AppTab.UserManagement -> {
                                    val userListState = viewModel.allUsers.collectAsStateWithLifecycle(emptyList())
                                    UserManagementScreen(
                                        users = userListState.value,
                                        onSaveUser = { user ->
                                            viewModel.saveUser(user) {
                                                Toast.makeText(context, "User berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDeleteUser = { user ->
                                            viewModel.deleteUser(user) {
                                                Toast.makeText(context, "User berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                                AppTab.CategoryManagement -> {
                                    val categoryListState = viewModel.allCategories.collectAsStateWithLifecycle(emptyList())
                                    CategoryManagementScreen(
                                        categories = categoryListState.value,
                                        onSaveCategory = { category ->
                                            viewModel.saveCategory(category) {
                                                Toast.makeText(context, "Kategori berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDeleteCategory = { category ->
                                            viewModel.deleteCategory(category) {
                                                Toast.makeText(context, "Kategori berhasil dihapus!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 1.5 Custom Gradient Overlays ---
                if (currentSubScreen == SubScreen.List) {
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    
                    // Top Gradient Overlay (starts from floating profile area and fades upward)
                    com.example.ui.components.TopFadeOverlay(
                        height = statusBarHeight + 72.dp,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    // Bottom Gradient Overlay (anchored directly to the top edge of the floating searchbar) - ONLY on Dashboard
                    if (currentTab == AppTab.Dashboard) {
                        com.example.ui.components.BottomFadeOverlay(
                            height = slotMetrics.anchorHeight + 16.dp,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                // 2. Floating Buttons (on top of everything else)
                if (currentSubScreen == SubScreen.List) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = {
                                hamburgerMenuExpanded = true
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_drawer_toggle"),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 1.5.dp, pressedElevation = 3.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer")
                        }

                        Box {
                            FloatingActionButton(
                                onClick = { userMenuExpanded = true },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_user_profile_floating"),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 1.5.dp, pressedElevation = 3.dp)
                            ) {
                                Text(
                                    text = currentUsername.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // 3. Floating Bottom Search Bar
                val keyboardController = LocalSoftwareKeyboardController.current

                val cardColor = com.example.ui.theme.AdaptiveColors.cardColorAccent()
                val cardBorderColor = com.example.ui.theme.AdaptiveColors.cardBorderColor(cardColor)

                AnimatedVisibility(
                    visible = (currentTab == AppTab.Dashboard && currentSubScreen == SubScreen.List),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = slotMetrics.bottomOffset)
                        .padding(horizontal = 16.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("cari kode atau nama...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            val trimmedQuery = searchQuery.trim()
                                            val matched = assets.firstOrNull { it.inventoryNumber.equals(trimmedQuery, ignoreCase = true) }
                                            if (matched != null) {
                                                showingAssetDetail = matched
                                            } else {
                                                prefilledInventoryForAddAsset = trimmedQuery
                                                currentSubScreen = SubScreen.AddAsset
                                            }
                                            keyboardController?.hide()
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tf_search_asset")
                            )
                            IconButton(
                                onClick = {
                                    showingBarcodeScanner = { code ->
                                        val trimmedCode = code.trim()
                                        if (trimmedCode.isNotEmpty()) {
                                            val matched = assets.firstOrNull { it.inventoryNumber.equals(trimmedCode, ignoreCase = true) }
                                            if (matched != null) {
                                                showingAssetDetail = matched
                                            } else {
                                                prefilledInventoryForAddAsset = trimmedCode
                                                currentSubScreen = SubScreen.AddAsset
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("btn_dashboard_scan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR/Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Custom Contextual Popup Menu
                AnimatedVisibility(
                    visible = userMenuExpanded,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)) +
                            scaleIn(initialScale = 0.92f, transformOrigin = TransformOrigin(0.95f, 0.05f), animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                            scaleOut(targetScale = 0.92f, transformOrigin = TransformOrigin(0.95f, 0.05f), animationSpec = tween(durationMillis = 150)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { userMenuExpanded = false }
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 12.dp, end = 16.dp)
                                .width(240.dp)
                                .align(Alignment.TopEnd)
                                .clickable(enabled = false) {}
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Halo, $currentUsername!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Role: $currentUserRole",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                
                                PopupMenuItem(
                                    text = "Manajemen User",
                                    icon = Icons.Default.ManageAccounts,
                                    onClick = {
                                        userMenuExpanded = false
                                        currentTab = AppTab.UserManagement
                                        currentSubScreen = SubScreen.List
                                    },
                                    testTag = "menu_user_management"
                                )

                                PopupMenuItem(
                                    text = "Manajemen Kategori",
                                    icon = Icons.Default.Category,
                                    onClick = {
                                        userMenuExpanded = false
                                        currentTab = AppTab.CategoryManagement
                                        currentSubScreen = SubScreen.List
                                    },
                                    testTag = "menu_kategori"
                                )

                                PopupMenuItem(
                                    text = "Pengaturan",
                                    icon = Icons.Default.Tune,
                                    onClick = {
                                        userMenuExpanded = false
                                        Toast.makeText(context, "Fitur Pengaturan akan segera hadir", Toast.LENGTH_SHORT).show()
                                    },
                                    testTag = "menu_pengaturan"
                                )
                                
                                PopupMenuItem(
                                    text = "Log Out",
                                    icon = Icons.Default.ExitToApp,
                                    onClick = {
                                        currentUsername = ""
                                        currentUserRole = ""
                                        userMenuExpanded = false
                                        currentTab = AppTab.Dashboard
                                        currentSubScreen = SubScreen.List
                                    },
                                    testTag = "menu_logout",
                                    isDestructive = true
                                )
                            }
                        }
                    }
                }

                // Custom Contextual Left Hamburger Popup Menu
                AnimatedVisibility(
                    visible = hamburgerMenuExpanded,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)) +
                            scaleIn(initialScale = 0.92f, transformOrigin = TransformOrigin(0.05f, 0.05f), animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                            scaleOut(targetScale = 0.92f, transformOrigin = TransformOrigin(0.05f, 0.05f), animationSpec = tween(durationMillis = 150)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { hamburgerMenuExpanded = false }
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 12.dp, start = 16.dp)
                                .width(260.dp)
                                .align(Alignment.TopStart)
                                .clickable(enabled = false) {}
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "IT Support Service",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Inventaris & Perawatan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                
                                PopupMenuItem(
                                    text = "Dasbor & Aset",
                                    icon = Icons.Default.Home,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        currentTab = AppTab.Dashboard
                                        currentSubScreen = SubScreen.List
                                    },
                                    testTag = "menu_dashboard",
                                    isSelected = (currentTab == AppTab.Dashboard)
                                )
                                
                                PopupMenuItem(
                                    text = "Perbaikan",
                                    icon = Icons.Default.Build,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        currentTab = AppTab.Perbaikan
                                        currentSubScreen = SubScreen.List
                                    },
                                    testTag = "menu_perbaikan",
                                    isSelected = (currentTab == AppTab.Perbaikan)
                                )
                                
                                PopupMenuItem(
                                    text = "Perawatan Rutin",
                                    icon = Icons.Default.Settings,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        currentTab = AppTab.Perawatan
                                        currentSubScreen = SubScreen.List
                                    },
                                    testTag = "menu_perawatan",
                                    isSelected = (currentTab == AppTab.Perawatan)
                                )

                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                PopupMenuItem(
                                    text = "Export PDF Inventaris",
                                    icon = Icons.Default.Share,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        val f = viewModel.exportToPdf(context, "assets")
                                        if (f != null) {
                                            viewModel.shareExportFile(context, f)
                                        } else {
                                            Toast.makeText(context, "Ekspor gagal!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    testTag = "menu_export_assets",
                                    isSelected = false
                                )

                                PopupMenuItem(
                                    text = "Export PDF Perbaikan",
                                    icon = Icons.Default.Share,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        val f = viewModel.exportToPdf(context, "repairs")
                                        if (f != null) {
                                            viewModel.shareExportFile(context, f)
                                        } else {
                                            Toast.makeText(context, "Ekspor gagal atau rentang tanggal kosong!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    testTag = "menu_export_repairs",
                                    isSelected = false
                                )

                                PopupMenuItem(
                                    text = "Export PDF Perawatan",
                                    icon = Icons.Default.Share,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        val f = viewModel.exportToPdf(context, "maintenances")
                                        if (f != null) {
                                            viewModel.shareExportFile(context, f)
                                        } else {
                                            Toast.makeText(context, "Ekspor gagal atau rentang tanggal kosong!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    testTag = "menu_export_maintenances",
                                    isSelected = false
                                )

                                PopupMenuItem(
                                    text = "Export Log Excell",
                                    icon = Icons.Default.Share,
                                    onClick = {
                                        hamburgerMenuExpanded = false
                                        val f = viewModel.exportAllLogsToExcel(context)
                                        if (f != null) {
                                            viewModel.shareExportFile(context, f)
                                        } else {
                                            Toast.makeText(context, "Ekspor log sistem gagal!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    testTag = "menu_export_logs",
                                    isSelected = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    showingAssetDetail?.let { asset ->
        val assetRepairsState = viewModel.getRepairsForAsset(asset.inventoryNumber).collectAsStateWithLifecycle(emptyList())
        val assetMaintsState = viewModel.getMaintenancesForAsset(asset.inventoryNumber).collectAsStateWithLifecycle(emptyList())
        val assetUpdatesState = viewModel.getUpdateLogsForAsset(asset.inventoryNumber).collectAsStateWithLifecycle(emptyList())

        AssetDetailDialog(
            asset = asset,
            repairs = assetRepairsState.value,
            maintenances = assetMaintsState.value,
            updates = assetUpdatesState.value,
            onDismiss = { showingAssetDetail = null },
            onAddRepairDirectly = {
                prefilledInventoryNumber = asset.inventoryNumber
                showingAssetDetail = null
                currentTab = AppTab.Perbaikan
                currentSubScreen = SubScreen.AddRepair
            },
            onAddMaintDirectly = {
                prefilledInventoryNumber = asset.inventoryNumber
                showingAssetDetail = null
                currentTab = AppTab.Perawatan
                currentSubScreen = SubScreen.AddMaintenance
            },
            onUpdateAssetDirectly = {
                showingUpdateAssetDialog = asset
                showingAssetDetail = null
            }
        )
    }

    showingUpdateAssetDialog?.let { asset ->
        AssetUpdateDialog(
            asset = asset,
            onDismiss = { showingUpdateAssetDialog = null },
            onSave = { loc, status, desc, reason ->
                viewModel.saveAssetUpdate(asset, loc, status, desc, reason) {
                    showingUpdateAssetDialog = null
                    Toast.makeText(context, "Detail dan status aset berhasil di-update!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    showingRepairDetail?.let { repair ->
        RepairDetailDialog(
            repair = repair,
            onDismiss = { showingRepairDetail = null },
            onResumeRepair = { resolvedRepair ->
                viewModel.resumeRepairToComplete(resolvedRepair, System.currentTimeMillis()) {
                    showingRepairDetail = null
                    Toast.makeText(context, "Status perbaikan berhasil diganti ke Selesai!", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenCamera = systemCameraOpener,
            userRole = currentUserRole,
            onVerifyRepair = { verifiedRepair ->
                viewModel.verifyRepair(verifiedRepair) {
                    showingRepairDetail = null
                    Toast.makeText(context, "Laporan perbaikan berhasil diverifikasi oleh Kepala Unit IT!", Toast.LENGTH_SHORT).show()
                }
            },
            viewModel = viewModel
        )
    }

    showingMaintenanceDetail?.let { maint ->
        MaintenanceDetailDialog(
            maintenance = maint,
            onDismiss = { showingMaintenanceDetail = null },
            onUpdateMaintenance = { updatedMaint ->
                viewModel.updateMaintenance(updatedMaint) {
                    showingMaintenanceDetail = null
                    Toast.makeText(context, "Perawatan rutin telah diselesaikan!", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenCamera = systemCameraOpener,
            userRole = currentUserRole,
            onVerifyMaintenance = { verifiedMaint ->
                viewModel.verifyMaintenance(verifiedMaint) {
                    showingMaintenanceDetail = null
                    Toast.makeText(context, "Laporan perawatan rutin berhasil diverifikasi oleh Kepala Unit IT!", Toast.LENGTH_SHORT).show()
                }
            },
            viewModel = viewModel
        )
    }

    // Barcode Scanner Simulator Modal
    showingBarcodeScanner?.let { callback ->
        BarcodeScannerDialog(
            assetList = assets,
            onAssetSelected = { code ->
                callback(code)
                showingBarcodeScanner = null
            },
            onDismiss = { showingBarcodeScanner = null }
        )
    }
}
