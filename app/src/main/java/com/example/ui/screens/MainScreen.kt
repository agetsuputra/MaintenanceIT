package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Asset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import java.util.Calendar
import com.example.data.model.Maintenance
import com.example.data.model.Repair
import com.example.ui.viewmodel.ITViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import java.text.SimpleDateFormat
import java.util.*

// Simulated Sample Images for Camera Photos
data class MockPhoto(val title: String, val url: String, val category: String)

val MOCK_PHOTOS = listOf(
    MockPhoto("Kerusakan LCD", "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400", "Layar"),
    MockPhoto("Mainboard Berdebu", "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=400", "Hub/PC"),
    MockPhoto("Pembersihan Switch", "https://images.unsplash.com/photo-1512486130939-2c4f79935e4f?w=400", "Jaringan"),
    MockPhoto("Uji Printer Test Page", "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=400", "Printer"),
    MockPhoto("Instalasi Bersih OS", "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=400", "Sistem"),
    MockPhoto("Kabel Manajemen Rapi", "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400", "Server")
)

fun loadBitmapFromUri(context: android.content.Context, uri: android.net.Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val ratio = width.toFloat() / height.toFloat()
    
    val (newWidth, newHeight) = if (width > height) {
        if (width > maxSize) {
            Pair(maxSize, (maxSize / ratio).toInt())
        } else {
            Pair(width, height)
        }
    } else {
        if (height > maxSize) {
            Pair((maxSize * ratio).toInt(), maxSize)
        } else {
            Pair(width, height)
        }
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

fun addWatermarkToBitmap(bitmap: Bitmap, location: String, dateStr: String): Bitmap {
    try {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = (bitmap.height / 22).toFloat().coerceAtLeast(16f)
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }
        
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(120, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        
        val textHeight = paint.textSize
        val padding = textHeight / 3
        val bannerHeight = textHeight * 2.2f
        
        canvas.drawRect(
            0f,
            bitmap.height - bannerHeight,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            bgPaint
        )
        
        val xPos = padding
        val yPosLine1 = bitmap.height - bannerHeight + textHeight + padding
        val yPosLine2 = yPosLine1 + textHeight + padding / 2
        
        canvas.drawText("📍 $location", xPos, yPosLine1, paint)
        canvas.drawText("📅 $dateStr | IT Hub Secured Verified", xPos, yPosLine2, paint)
        
        return result
    } catch (e: Exception) {
        return bitmap
    }
}

fun getDeviceLocation(context: android.content.Context, fallbackLocation: String): String {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
    if (locationManager != null) {
        try {
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasCoarse || hasFine) {
                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        return "Lat: ${String.format(Locale.US, "%.4f", loc.latitude)}, Lon: ${String.format(Locale.US, "%.4f", loc.longitude)} (GPS) - $fallbackLocation"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return fallbackLocation
}

fun fetchRealtimeLocation(context: android.content.Context, onResult: (String) -> Unit) {
    val fusedLocationClient = try {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
    } catch (e: Exception) {
        null
    }

    val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    
    val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!hasCoarse && !hasFine) {
        onResult("Location Permission Denied")
        return
    }

    if (fusedLocationClient != null) {
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult("Lat: ${String.format(Locale.US, "%.5f", loc.latitude)}, Lon: ${String.format(Locale.US, "%.5f", loc.longitude)}")
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            onResult("Lat: ${String.format(Locale.US, "%.5f", lastLoc.latitude)}, Lon: ${String.format(Locale.US, "%.5f", lastLoc.longitude)}")
                        } else {
                            fallbackLocationManager(context, onResult)
                        }
                    }.addOnFailureListener {
                        fallbackLocationManager(context, onResult)
                    }
                }
            }.addOnFailureListener {
                fallbackLocationManager(context, onResult)
            }
        } catch (e: SecurityException) {
            fallbackLocationManager(context, onResult)
        }
        
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            cts.cancel()
        }, 1500)
    } else {
        fallbackLocationManager(context, onResult)
    }
}

private fun fallbackLocationManager(context: android.content.Context, onResult: (String) -> Unit) {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
    if (locationManager == null) {
        onResult("GPS Unavailable")
        return
    }
    try {
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
                onResult("Lat: ${String.format(Locale.US, "%.5f", loc.latitude)}, Lon: ${String.format(Locale.US, "%.5f", loc.longitude)}")
                return
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    onResult("GPS Signal Lost")
}

fun compressAndWatermarkBitmap(
    bitmap: Bitmap,
    location: String,
    dateStr: String,
    maxSize: Int = 600,
    quality: Int = 30
): String {
    val resized = resizeBitmap(bitmap, maxSize)
    val watermarked = addWatermarkToBitmap(resized, location, dateStr)
    val outputStream = ByteArrayOutputStream()
    watermarked.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val bytes = outputStream.toByteArray()
    val base64Str = Base64.encodeToString(bytes, Base64.DEFAULT)
    return "data:image/jpeg;base64,$base64Str"
}

fun parseWatermarkedPhoto(photoStr: String?, defaultLocation: String = "IT Office"): Triple<String, String, String> {
    if (photoStr.isNullOrBlank()) return Triple("", "", "")
    val parts = photoStr.split("|||")
    return if (parts.size >= 3) {
        Triple(parts[0], parts[1], parts[2])
    } else {
        val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        Triple(parts[0], defaultLocation, dateString)
    }
}

@Composable
fun WatermarkedAsyncImage(
    photoStr: String?,
    defaultLocation: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val (url, loc, date) = parseWatermarkedPhoto(photoStr, defaultLocation)
    if (url.startsWith("http") || url.isNotEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            val decodedModel: Any = remember(url) {
                if (url.startsWith("data:image") && url.contains("base64,")) {
                    try {
                        val base64Data = url.substringAfter("base64,")
                        Base64.decode(base64Data, Base64.DEFAULT)
                    } catch (e: Exception) {
                        url
                    }
                } else {
                    url
                }
            }
            AsyncImage(
                model = decodedModel,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
            // Watermark bottom-left overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "📍 $loc \n📅 $date",
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Tidak Ada Foto Bukti", 
                fontSize = 11.sp, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: ITViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.Dashboard) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.List) }
    var showSplash by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var userMenuExpanded by remember { mutableStateOf(false) }
    var hamburgerMenuExpanded by remember { mutableStateOf(false) }

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
    val assets by viewModel.allAssets.collectAsStateWithLifecycle()
    val repairs by viewModel.allRepairs.collectAsStateWithLifecycle()
    val filteredRepairs by viewModel.filteredRepairs.collectAsStateWithLifecycle()
    val filteredMaintenances by viewModel.filteredMaintenances.collectAsStateWithLifecycle()

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
    var isSearchingGps by remember { mutableStateOf(false) }

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

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

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
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = modifier.testTag("main_screen_scaffold")
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
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
                                                searchQuery = searchQuery
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
                    val themeBgColor = MaterialTheme.colorScheme.background
                    
                    // Top Gradient Overlay (for status bar & floating buttons readability)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        themeBgColor.copy(alpha = 0.95f),
                                        themeBgColor.copy(alpha = 0.85f),
                                        themeBgColor.copy(alpha = 0.50f),
                                        themeBgColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Bottom Gradient Overlay (for navigation bar & floating search bar readability) - ONLY on Dashboard
                    if (currentTab == AppTab.Dashboard) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            themeBgColor.copy(alpha = 0.15f),
                                            themeBgColor.copy(alpha = 0.50f),
                                            themeBgColor.copy(alpha = 0.85f),
                                            themeBgColor.copy(alpha = 0.95f),
                                            themeBgColor
                                        )
                                    )
                                )
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
                val imeBottomPaddingForSearch = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                val isDashboardKeyboardOpen = imeBottomPaddingForSearch > 0.dp
                val searchBarBottomOffset = if (isDashboardKeyboardOpen) {
                    imeBottomPaddingForSearch + 24.dp
                } else {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                }

                // Dynamic blending color that guarantees slightly darker background dynamically in both themes
                val isDark = isSystemInDarkTheme()
                val bg = MaterialTheme.colorScheme.background
                val cardColor = if (isDark) {
                    androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.25f)
                } else {
                    androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.07f)
                }

                val cardBorderColor = if (isDark) {
                    androidx.compose.ui.graphics.lerp(cardColor, Color.Black, 0.15f)
                } else {
                    androidx.compose.ui.graphics.lerp(cardColor, Color.Black, 0.12f)
                }

                AnimatedVisibility(
                    visible = (currentTab == AppTab.Dashboard && currentSubScreen == SubScreen.List),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = searchBarBottomOffset)
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

                // Custom Left Hamburger Popup Menu
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
}

// ==================== DASHBOARD & INVENTORY ====================

@Composable
fun DashboardScreen(
    assets: List<Asset>,
    repairs: List<Repair>,
    isFirebaseEnabled: Boolean,
    onAssetClick: (Asset) -> Unit,
    searchQuery: String
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterAllSelected by remember { mutableStateOf(true) }

    val filteredAssets = remember(assets, searchQuery, selectedCategories, filterAllSelected) {
        assets.filter {
            val matchesSearch = it.inventoryNumber.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.location.contains(searchQuery, ignoreCase = true)
            val matchesCategory = filterAllSelected || selectedCategories.contains(it.type)
            matchesSearch && matchesCategory
        }
    }

    // Count statistics
    val totalAssets = remember(assets) { assets.size }
    val activeAssets = remember(assets) { assets.count { it.status == "Aktif" } }
    val brokenAssets = remember(assets) { assets.count { it.status == "Rusak Permanen" } }
    val holdAssets = remember(assets) { assets.count { it.status == "Hold" || it.status == "Dalam Pengerjaan" } }

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottomPadding > 0.dp
    val dashboardBottomPadding = if (isKeyboardOpen) {
        imeBottomPadding + 96.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_column"),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp,
            bottom = dashboardBottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming card with statistics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dasbor Manajemen Perangkat",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFirebaseEnabled) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isFirebaseEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirebaseEnabled) Color(0xFF2E7D32) else Color.Red)
                            )
                            Text(
                                text = if (isFirebaseEnabled) "Cloud Sync Aktif" else "Database Lokal",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFirebaseEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Grid stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Aset",
                            value = totalAssets.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        StatCard(
                            label = "Aktif",
                            value = activeAssets.toString(),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        StatCard(
                            label = "Rusak",
                            value = brokenAssets.toString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                        StatCard(
                            label = "Hold",
                            value = holdAssets.toString(),
                            color = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            }
        }

        // Heading with Filter button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daftar Seluruh Perangkat (${filteredAssets.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFilterDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Filter",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                AssetItemCard(asset = asset, onClick = { onAssetClick(asset) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showFilterDialog) {
        val availableCategories = listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Kategori Aset", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filterAllSelected = !filterAllSelected
                                    if (filterAllSelected) {
                                        selectedCategories = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = filterAllSelected,
                                onCheckedChange = { checked ->
                                    filterAllSelected = checked
                                    if (checked) {
                                        selectedCategories = emptySet()
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Semua Kategori (All)")
                        }
                    }
                    items(availableCategories) { cat ->
                        val isChecked = !filterAllSelected && selectedCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedCategories.toMutableSet()
                                    if (current.contains(cat)) {
                                        current.remove(cat)
                                    } else {
                                        current.add(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val current = selectedCategories.toMutableSet()
                                    if (checked) {
                                        current.add(cat)
                                    } else {
                                        current.remove(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cat)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AssetItemCard(asset: Asset, onClick: () -> Unit) {
    val statusColor = when (asset.status) {
        "Aktif" -> Color(0xFF2E7D32)
        "Rusak" -> MaterialTheme.colorScheme.error
        "Hold" -> Color(0xFFE65100)
        "Perawatan" -> Color(0xFF0277BD)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Dynamic blending color that guarantees slightly darker background dynamically in both themes
    val isDark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    val cardColor = if (isDark) {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.25f)
    } else {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.07f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("asset_card_${asset.inventoryNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = asset.inventoryNumber,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = asset.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lokasi
                Column {
                    Text(
                        text = asset.location,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Kategori
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = asset.type,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Dialog to input new Asset
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddAssetForm(
    onSave: (Asset) -> Unit,
    onCancel: () -> Unit,
    assetList: List<Asset>,
    onOpenScanner: (((String) -> Unit)) -> Unit,
    initialInventoryNumber: String? = null,
    categories: List<String> = emptyList()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val displayCategories = if (categories.isNotEmpty()) categories else listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")

    var invNum by remember { mutableStateOf(initialInventoryNumber ?: "") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(displayCategories.firstOrNull() ?: "Laptop") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var acquisitionDateLong by remember { mutableStateOf(System.currentTimeMillis()) }
    var purchasePriceInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val simpleDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var categoryExpanded by remember { mutableStateOf(false) }

    fun formatThousandSeparator(input: String): String {
        val clean = input.filter { it.isDigit() }
        if (clean.isEmpty()) return ""
        val reversed = clean.reversed()
        val builder = StringBuilder()
        for (i in reversed.indices) {
            if (i > 0 && i % 3 == 0) {
                builder.append('.')
            }
            builder.append(reversed[i])
        }
        return builder.reverse().toString()
    }

    fun capitalizeFirstLetter(input: String): String {
        if (input.isEmpty()) return ""
        return input[0].uppercaseChar().toString() + input.substring(1)
    }

    val themeBgColor = MaterialTheme.colorScheme.background

    val imeBottomPaddingForButtons = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottomPaddingForButtons > 0.dp
    val buttonsBottomOffset = if (isKeyboardOpen) {
        imeBottomPaddingForButtons + 24.dp
    } else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_asset_form")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 84.dp,
                bottom = buttonsBottomOffset + 76.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Pendaftaran Aset Inventaris Baru",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Masukkan data detail perangkat keras yang dikelola oleh tim IT support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = invNum,
                    onValueChange = { invNum = capitalizeFirstLetter(it.trim().uppercase(Locale.getDefault())) },
                    label = { Text("Nomor Inventaris Aset") },
                    placeholder = { Text("Contoh: INV-LP-025") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onOpenScanner { code ->
                                    invNum = capitalizeFirstLetter(code.trim().uppercase(Locale.getDefault()))
                                }
                            },
                            modifier = Modifier.testTag("btn_inv_number_scan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR/Barcode",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tf_inv_number"),
                    isError = assetList.any { it.inventoryNumber.equals(invNum, ignoreCase = true) }
                )
                if (assetList.any { it.inventoryNumber.equals(invNum, ignoreCase = true) }) {
                    Text(
                        "Nomor inventaris ini sudah terdaftar!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = capitalizeFirstLetter(it) },
                    label = { Text("Nama Perangkat") },
                    placeholder = { Text("Contoh: iMac Pro Retina 2024") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tf_asset_name")
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori Perangkat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("tf_asset_type")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        displayCategories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    type = selectionOption
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = capitalizeFirstLetter(it) },
                    label = { Text("Lokasi Perangkat") },
                    placeholder = { Text("Contoh: Ruang Meeting Lt. 2") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tf_asset_location")
                )
            }

            item {
                val calendar = Calendar.getInstance().apply { timeInMillis = acquisitionDateLong }
                val startYear = calendar.get(Calendar.YEAR)
                val startMonth = calendar.get(Calendar.MONTH)
                val startDay = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        val selectedCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, selectedDay)
                        }
                        acquisitionDateLong = selectedCal.timeInMillis
                    },
                    startYear,
                    startMonth,
                    startDay
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                ) {
                    OutlinedTextField(
                        value = simpleDateFormat.format(java.util.Date(acquisitionDateLong)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Tanggal Pengadaan Aset *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pilih Tanggal Pengadaan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tf_asset_acquisition_date"),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = purchasePriceInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        purchasePriceInput = formatThousandSeparator(clean)
                    },
                    label = { Text("Harga Beli (Rp) - Opsional") },
                    placeholder = { Text("Contoh: 1.250.000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tf_asset_purchase_price")
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = capitalizeFirstLetter(it) },
                    label = { Text("Spesifikasi & Keterangan Tambahan") },
                    placeholder = { Text("Prosesor, RAM, Penyimpanan, dll...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("tf_asset_desc")
                )
            }
        }

        // Top Gradient (same as Dashboard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            themeBgColor.copy(alpha = 0.95f),
                            themeBgColor.copy(alpha = 0.85f),
                            themeBgColor.copy(alpha = 0.50f),
                            themeBgColor.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Gradient Overlay (same as Dashboard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            themeBgColor.copy(alpha = 0.15f),
                            themeBgColor.copy(alpha = 0.50f),
                            themeBgColor.copy(alpha = 0.85f),
                            themeBgColor.copy(alpha = 0.95f),
                            themeBgColor
                        )
                    )
                )
        )

        // Floating Row of cancel and save buttons following the keyboard
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = buttonsBottomOffset)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    onCancel()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                elevation = null,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("btn_cancel_asset")
            ) {
                Text("Batal")
            }

            val isValid = invNum.isNotBlank() && name.isNotBlank() && location.isNotBlank() &&
                    assetList.none { it.inventoryNumber.equals(invNum, ignoreCase = true) }

            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    val priceClean = purchasePriceInput.replace(".", "")
                    val priceDouble = priceClean.toDoubleOrNull()
                    onSave(Asset(
                        inventoryNumber = invNum,
                        name = name,
                        type = type,
                        location = location,
                        status = "Aktif",
                        description = description,
                        createdAt = System.currentTimeMillis(),
                        acquisitionDate = acquisitionDateLong,
                        purchasePrice = priceDouble
                    ))
                },
                shape = RoundedCornerShape(12.dp),
                enabled = isValid,
                elevation = null,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("btn_save_asset")
            ) {
                Text("Simpan")
            }
        }
    }
}

// Detail dialog showing Asset Info and History
@Composable
fun AssetDetailDialog(
    asset: Asset,
    repairs: List<Repair>,
    maintenances: List<Maintenance>,
    updates: List<com.example.data.model.AssetUpdateLog>,
    onDismiss: () -> Unit,
    onAddRepairDirectly: () -> Unit,
    onAddMaintDirectly: () -> Unit,
    onUpdateAssetDirectly: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Informasi Detail Aset",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Aset
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = asset.name,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "No. Inventaris: ${asset.inventoryNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date(asset.acquisitionDate))
                                val priceStr = asset.purchasePrice?.let {
                                    "Rp " + String.format(Locale.getDefault(), "%,.0f", it).replace(',', '.')
                                } ?: "-"
                                RowValue("Kategori", asset.type)
                                RowValue("Lokasi", asset.location)
                                RowValue("Status", asset.status)
                                RowValue("Tgl Pengadaan", dateStr)
                                RowValue("Harga Beli", priceStr)
                                RowValue("Spesifikasi", asset.description ?: "-")
                            }
                        }
                    }

                    // Direct action options
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onAddRepairDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Catat Perbaikan", fontSize = 10.sp, maxLines = 1)
                            }
                            Button(
                                onClick = onAddMaintDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Rawat Rutin", fontSize = 10.sp, maxLines = 1)
                            }
                            Button(
                                onClick = onUpdateAssetDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Update Aset", fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    // History Title
                    item {
                        Text(
                            "Riwayat Aktivitas & Perbaikan",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Loop Update Logs First if any exists
                    if (updates.isNotEmpty()) {
                        item {
                            Text(
                                "Riwayat Update Detail & Status (${updates.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        items(updates) { u ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Update Informasi",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            dateFormatter.format(Date(u.updateTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    if (u.oldLocation != u.newLocation) {
                                        Text("Lokasi: ${u.oldLocation} ➔ ${u.newLocation}", fontSize = 11.sp)
                                    } else {
                                        Text("Lokasi: ${u.newLocation}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    if (u.oldStatus != u.newStatus) {
                                        Text("Status: ${u.oldStatus} ➔ ${u.newStatus}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text("Status: ${u.newStatus}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (!u.reasonForPermanentDamage.isNullOrBlank()) {
                                        Text("Sebab Rusak Permanen: ${u.reasonForPermanentDamage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                    }

                                    if (u.oldDescription != u.newDescription) {
                                        Text("Spesifikasi: ${u.oldDescription ?: "-"} ➔ ${u.newDescription ?: "-"}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (repairs.isEmpty() && maintenances.isEmpty() && updates.isEmpty()) {
                        item {
                            Text(
                                "Belum terdapat riwayat perbaikan maupun perawatan rutin.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    }

                    // Loop Repairs
                    if (repairs.isNotEmpty()) {
                        item {
                            Text(
                                "Riwayat Perbaikan Kerusakan (${repairs.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(repairs) { r ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            r.status,
                                            fontWeight = FontWeight.Bold,
                                            color = if (r.status == "Selesai") Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            dateFormatter.format(Date(r.startTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Kendala: ${r.problem}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("Tindak Lanjut: ${r.actionTaken}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (r.status == "Hold") {
                                        Text("Alasan Hold: ${r.holdReason}", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Medium)
                                    }
                                    Text("Teknisi: ${r.technician}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Loop Maintenances
                    if (maintenances.isNotEmpty()) {
                        item {
                            Text(
                                "Riwayat Perawatan Rutin (${maintenances.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF0277BD)
                            )
                        }
                        items(maintenances) { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Berhasil Diperiksa",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            dateFormatter.format(Date(m.startTime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text("Tindakan: ${m.actionTaken}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    Text("Hasil: ${m.result}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Teknisi: ${m.technician}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetUpdateDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSave: (newLocation: String, newStatus: String, newDescription: String?, reasonForPermanentDamage: String?) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var location by remember { mutableStateOf(asset.location) }
    var status by remember { mutableStateOf(asset.status) }
    var description by remember { mutableStateOf(asset.description ?: "") }
    var reasonForPermanentDamage by remember { mutableStateOf("") }

    val statusOptions = listOf("Aktif", "Hold", "Dalam Pengerjaan", "Rusak Permanen")
    var statusExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Update Informasi & Status Aset",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "No. Inventaris: ${asset.inventoryNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lokasi Perangkat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = status,
                        onValueChange = {},
                        label = { Text("Status Perangkat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    status = option
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Conditional Reason Field if "Rusak Permanen" is selected
                if (status == "Rusak Permanen") {
                    OutlinedTextField(
                        value = reasonForPermanentDamage,
                        onValueChange = { reasonForPermanentDamage = it },
                        label = { Text("Penyebab Kerusakan Permanen *Wajib") },
                        isError = reasonForPermanentDamage.isBlank(),
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Spesifikasi & Catatan Tambahan") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    val canSave = status != "Rusak Permanen" || reasonForPermanentDamage.isNotBlank()
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onSave(
                                location,
                                status,
                                description.ifBlank { null },
                                if (status == "Rusak Permanen") reasonForPermanentDamage else null
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun RowValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}


// ==================== PART 1: REPAIRS (PERBAIKAN KERUSAKAN) ====================

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
    onExportClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Filter Dialogs triggers
    val startPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 0, 0, 0)
            onStartDateChange(cal.timeInMillis)
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    val endPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 23, 59, 59)
            onEndDateChange(cal.timeInMillis)
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    // Category filter state for repairs list (matching dashboard filter)
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterAllSelected by remember { mutableStateOf(true) }

    val filteredRepairs = remember(repairs, assets, selectedCategories, filterAllSelected) {
        repairs.filter { repair ->
            val matchedAsset = assets.find { it.inventoryNumber == repair.inventoryNumber }
            val assetType = matchedAsset?.type ?: "Lainnya"
            filterAllSelected || selectedCategories.contains(assetType)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("repairs_screen")
    ) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 210.dp // High padding to scroll past the floating bottom Card
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title (scrollable)
            item {
                Text(
                    "Laporan Perbaikan Kerusakan",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Header with Filter button (scrollable)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Perbaikan (${filteredRepairs.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFilterDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Kategori",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Filter",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Repair list logic
            if (filteredRepairs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRepairs, key = { it.id }) { repair ->
                    RepairItemCard(
                        repair = repair,
                        assets = assets,
                        onClick = { onRepairClick(repair) }
                    )
                }
            }
        }

        // Floating Gradient Overlay for bottom floating Card
        val themeBgColor = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            themeBgColor.copy(alpha = 0.15f),
                            themeBgColor.copy(alpha = 0.50f),
                            themeBgColor.copy(alpha = 0.85f),
                            themeBgColor.copy(alpha = 0.95f),
                            themeBgColor
                        )
                    )
                )
        )

        // Date Picker Range Row inside Floating Sticky Card at the bottom
        val isDark = isSystemInDarkTheme()
        val bg = MaterialTheme.colorScheme.background
        val cardColor = if (isDark) {
            androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.25f)
        } else {
            androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.07f)
        }

        val cardBorderColor = if (isDark) {
            androidx.compose.ui.graphics.lerp(cardColor, Color.Black, 0.15f)
        } else {
            androidx.compose.ui.graphics.lerp(cardColor, Color.Black, 0.12f)
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Rentang Waktu Perbaikan:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { startPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_start_date"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = format.format(Date(startDate)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = " s.d ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { endPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_end_date"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = format.format(Date(endDate)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddRepairClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_add_repair"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Perbaikan", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_repairs")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Export PDF", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        val availableCategories = listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Kategori Perangkat", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filterAllSelected = !filterAllSelected
                                    if (filterAllSelected) {
                                        selectedCategories = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = filterAllSelected,
                                onCheckedChange = { checked ->
                                    filterAllSelected = checked
                                    if (checked) {
                                        selectedCategories = emptySet()
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Semua Kategori (All)")
                        }
                    }
                    items(availableCategories) { cat ->
                        val isChecked = !filterAllSelected && selectedCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedCategories.toMutableSet()
                                    if (current.contains(cat)) {
                                        current.remove(cat)
                                    } else {
                                        current.add(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val current = selectedCategories.toMutableSet()
                                    if (checked) {
                                        current.add(cat)
                                    } else {
                                        current.remove(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cat)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RepairItemCard(repair: Repair, assets: List<Asset>, onClick: () -> Unit) {
    val matchedAsset = assets.find { it.inventoryNumber == repair.inventoryNumber }
    val assetName = matchedAsset?.name ?: "Perangkat Tidak Dikenal"
    val assetType = matchedAsset?.type ?: "Lainnya"
    val isHold = repair.status == "Hold"
    
    val statusColor = when (repair.status) {
        "Hold" -> Color(0xFFEF6C00) // Orange
        "Dalam Pengerjaan" -> Color(0xFF1976D2) // Blue
        else -> Color(0xFF2E7D32) // Green
    }

    // Dynamic blending color that guarantees slightly darker background dynamically in both themes
    val isDark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    val cardColor = if (isDark) {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.25f)
    } else {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.07f)
    }

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
                        text = "Inventaris: ${repair.inventoryNumber}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Kategori: $assetType",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

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

            Spacer(modifier = Modifier.height(10.dp))

            // Thin Horizontal Divider Line like main assets card style
            Divider(
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
                        text = "Kendala",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(2.dp))
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
                        text = "Teknisi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(2.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRepairForm(
    assetList: List<Asset>,
    onSave: (Repair) -> Unit,
    onCancel: () -> Unit,
    onOpenScanner: ((String) -> Unit) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    initialInventoryNumber: String? = null,
    currentUser: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var invNum by remember { mutableStateOf(initialInventoryNumber ?: "") }
    var problem by remember { mutableStateOf("") }
    var cause by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    var holdReason by remember { mutableStateOf("") }
    var holdEstimate by remember { mutableStateOf("") }
    
    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf(currentUser) }

    val statusOptions = listOf("Dalam Pengerjaan", "Hold", "Selesai")
    var statusExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("add_repair_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Registrasi Perbaikan Kerusakan",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Segera catat kerusakan laptop, computer, printer, atau jaringan yang ditangani.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Device selection via typing or simulator scanning
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Kaitkan Inventaris Perangkat",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = invNum,
                        onValueChange = { invNum = it.trim().uppercase() },
                        label = { Text("Nomor Inventaris Perangkat") },
                        placeholder = { Text("No. Inventaris (Contoh: INV-PC-001)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onOpenScanner { code ->
                                        invNum = code.trim().uppercase()
                                    }
                                },
                                modifier = Modifier.testTag("repair_btn_inv_scan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR/Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("repair_tf_inv"),
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = problem,
                onValueChange = { problem = it },
                label = { Text("Kendala / Keluhan Kerusakan") },
                placeholder = { Text("Contoh: Monitor flicker merah jambu atau komputer sering hang.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("repair_tf_problem")
            )
        }

        item {
            OutlinedTextField(
                value = cause,
                onValueChange = { cause = it },
                label = { Text("Penyebab Kerusakan (Bisa diisi nanti jika hold)") },
                placeholder = { Text("Contoh: Overheat pendingin kering kotor, atau RAM kendor.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("repair_tf_cause")
            )
        }

        item {
            OutlinedTextField(
                value = actionTaken,
                onValueChange = { actionTaken = it },
                label = { Text("Tindak Lanjut / Solusi (Wajib jika selesai)") },
                placeholder = { Text("Contoh: Re-pasta thermal, pembersihan debu kipas, kencangkan ram.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("repair_tf_action")
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status Hasil Pekerjaan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("repair_tf_status")
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                status = opt
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Conditional Alasan Hold & Estimasi
        if (status == "Hold") {
            item {
                OutlinedTextField(
                    value = holdReason,
                    onValueChange = { holdReason = it },
                    label = { Text("Alasan Pending / Hold") },
                    placeholder = { Text("Contoh: Menunggu modul sparepart impor / LCD pengganti.") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_hold_reason")
                )
            }
            item {
                OutlinedTextField(
                    value = holdEstimate,
                    onValueChange = { holdEstimate = it },
                    label = { Text("Estimasi Penyelesaian (Waktu)") },
                    placeholder = { Text("Contoh: 3-5 Hari Kerja") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repair_tf_hold_est")
                )
            }
        }

        // Technician Name
        item {
            OutlinedTextField(
                value = technician,
                onValueChange = { technician = it },
                label = { Text("Teknisi Penanggung Jawab") },
                placeholder = { Text("Nama Lengkap") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repair_tf_tech"),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
        }

        // Camera Simulated Photo Pickers (3 Photo Workflow)
        item {
            val matchedAsset = assetList.find { it.inventoryNumber == invNum }
            val assetLocation = matchedAsset?.location ?: "Gedung IT"
            val currentDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Watermarked Photo Verification Flow",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Semua foto dilengkapi tanda air berisi Lokasi perangkat ($assetLocation) dan Tanggal ($currentDateStr). Pengambilan dari galeri dinonaktifkan demi keaslian data.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1. Photo Before
                    Column {
                        Text("1. Foto Sebelum Perbaikan (Before) *Wajib", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        if (!photoBefore.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoBefore, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoBefore = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoBefore = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sebelum / Mulai Kerja")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 2. Photo After
                    Column {
                        Text(
                            text = "2. Foto Sesudah Perbaikan (After) " + if (status == "Selesai") "*Wajib" else "(Opsional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (status == "Selesai" && photoAfter.isNullOrBlank()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!photoAfter.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoAfter, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoAfter = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoAfter = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                enabled = status == "Selesai" || photoBefore != null
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sesudah Pekerjaan Selesai")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 3. Photo with User
                    Column {
                        Text(
                            text = "3. Foto Bersama Tim & Unit " + if (status == "Selesai") "*Wajib" else "(Opsional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (status == "Selesai" && photoUser.isNullOrBlank()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!photoUser.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoUser, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoUser = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoUser = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = status == "Selesai" || photoBefore != null
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Bukti Bersama Unit")
                            }
                        }
                    }
                }
            }
        }

        // Form Submit Buttons
        item {
            val isFormValid = invNum.isNotBlank() && problem.isNotBlank() && technician.isNotBlank() &&
                    !photoBefore.isNullOrBlank() &&
                    (status != "Selesai" || (actionTaken.isNotBlank() && !photoAfter.isNullOrBlank() && !photoUser.isNullOrBlank())) &&
                    (status != "Hold" || (holdReason.isNotBlank() && holdEstimate.isNotBlank()))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onCancel()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_cancel_repair")
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        val repair = Repair(
                            inventoryNumber = invNum,
                            startTime = System.currentTimeMillis() - 2 * 3600 * 1000, // started 2 hours ago
                            endTime = if (status == "Selesai") System.currentTimeMillis() else null,
                            problem = problem,
                            cause = cause,
                            actionTaken = actionTaken,
                            status = status,
                            holdReason = if (status == "Hold") holdReason else null,
                            holdEstimate = if (status == "Hold") holdEstimate else null,
                            photoBefore = photoBefore,
                            photoAfter = photoAfter,
                            photoUser = photoUser,
                            technician = technician
                        )
                        onSave(repair)
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_save_repair")
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

// Repair Details sheet includes possibility to Resume Hold repairs to Completed!
@Composable
fun RepairDetailDialog(
    repair: Repair,
    onDismiss: () -> Unit,
    onResumeRepair: (Repair) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    userRole: String,
    onVerifyRepair: (Repair) -> Unit,
    viewModel: ITViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isUnfinished = repair.status == "Hold" || repair.status == "Dalam Pengerjaan"

    // Local mutable state for editing/resolving the repair
    var localCause by remember { mutableStateOf(repair.cause) }
    var localActionTaken by remember { mutableStateOf(repair.actionTaken) }
    var localPhotoAfter by remember { mutableStateOf<String?>(repair.photoAfter) }
    var localPhotoUser by remember { mutableStateOf<String?>(repair.photoUser) }

    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }

    var showingPinVerification by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val matchedAsset = viewModel.allAssets.collectAsStateWithLifecycle(emptyList()).value
        .find { it.inventoryNumber == repair.inventoryNumber }
    val assetLocation = matchedAsset?.location ?: "Gedung IT"

    if (showingPinVerification) {
        PinVerificationDialog(
            viewModel = viewModel,
            onDismiss = { showingPinVerification = false },
            onPinCorrect = {
                showingPinVerification = false
                val finalRepair = repair.copy(
                    status = "Selesai & Terverifikasi",
                    cause = localCause.ifBlank { repair.cause },
                    actionTaken = localActionTaken.ifBlank { repair.actionTaken },
                    photoAfter = localPhotoAfter ?: repair.photoAfter,
                    photoUser = localPhotoUser ?: repair.photoUser
                )
                onVerifyRepair(finalRepair)
            }
        )
    }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detail Laporan Perbaikan",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Aset yang Diperbaiki:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(matchedAsset?.name ?: "Perangkat Tidak Dikenal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                Text("No. Inventaris: ${repair.inventoryNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                RowValue("Waktu Mulai", dateFormatter.format(Date(repair.startTime)))
                                RowValue("Waktu Selesai", repair.endTime?.let { dateFormatter.format(Date(it)) } ?: "Masih Tertunda (${repair.status})")
                                RowValue("Teknisi", repair.technician)
                            }
                        }
                    }

                    item {
                        DetailSection("Status Kerusakan", repair.status) {
                            val color = when (repair.status) {
                                "Hold" -> Color(0xFFE65100)
                                "Dalam Pengerjaan" -> Color(0xFF0277BD)
                                else -> Color(0xFF2E7D32)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    repair.status,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        DetailTextSection("Masalah / Kendala", repair.problem)
                    }

                    if (repair.status == "Selesai" || repair.status == "Selesai & Terverifikasi") {
                        item {
                            DetailTextSection("Penyebab", repair.cause.ifBlank { "Belum dicatat / Belum diketahui" })
                        }

                        item {
                            DetailTextSection("Tindakan / Solusi", repair.actionTaken)
                        }
                    }

                    if (repair.status == "Hold") {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Informasi Hold / Pending:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                    Spacer(Modifier.height(6.dp))
                                    RowValue("Alasan Pending", repair.holdReason ?: "")
                                    RowValue("Estimasi Selesai", repair.holdEstimate ?: "")
                                }
                            }
                        }
                    }

                    // Display all 3 photos
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Laporan Bukti Foto Sesuai SOP:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                
                                // Before photo status
                                Text("📸 Foto Sebelum (Before)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                WatermarkedAsyncImage(
                                    photoStr = repair.photoBefore,
                                    defaultLocation = "IT Room",
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                )

                                if (repair.status == "Selesai" || repair.status == "Selesai & Terverifikasi") {
                                    Text("📸 Foto Sesudah (After)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = repair.photoAfter,
                                        defaultLocation = "IT Room",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )

                                    Text("📸 Foto Bersama Unit (Serah Terima)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = repair.photoUser,
                                        defaultLocation = "IT Room",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )
                                }
                            }
                        }
                    }

                    // INTERACTIVE COMPLETION SUB-FORM FOR UNFINISHED REPAIRS
                    if (isUnfinished) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Form Penyelesaian Pekerjaan",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Untuk menyelesaikan pekerjaan perbaikan ini, isi solusi akhir dan ambil 2 foto verifikasi penutup berikut secara langsung.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = localCause,
                                        onValueChange = { localCause = it },
                                        label = { Text("Penyebab Akhir *Wajib") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    OutlinedTextField(
                                        value = localActionTaken,
                                        onValueChange = { localActionTaken = it },
                                        label = { Text("Solusi / Tindak Lanjut *Wajib") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    // Capture Photo After Slot
                                    Text("📸 Ambil Foto Sesudah (After) *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoAfter.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoAfter, assetLocation, modifier = Modifier.fillMaxSize())
                                            IconButton(
                                                onClick = { localPhotoAfter = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(0.6f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenCamera { photo -> localPhotoAfter = photo } },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ambil Foto Sesudah", fontSize = 11.sp)
                                        }
                                    }

                                    // Capture Photo User Slot
                                    Text("📸 Ambil Foto Bersama Unit *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoUser.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoUser, assetLocation, modifier = Modifier.fillMaxSize())
                                            IconButton(
                                                onClick = { localPhotoUser = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(0.6f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenCamera { photo -> localPhotoUser = photo } },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ambil Foto Bersama Unit", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    // Validate and Submit
                                    val isResolveEnabled = localActionTaken.isNotBlank() &&
                                            localCause.isNotBlank() &&
                                            !localPhotoAfter.isNullOrBlank() &&
                                            !localPhotoUser.isNullOrBlank()

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            val updatedRepair = repair.copy(
                                                status = "Selesai",
                                                endTime = System.currentTimeMillis(),
                                                cause = localCause,
                                                actionTaken = localActionTaken,
                                                photoAfter = localPhotoAfter,
                                                photoUser = localPhotoUser,
                                                holdReason = null,
                                                holdEstimate = null
                                            )
                                            onResumeRepair(updatedRepair)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = isResolveEnabled,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_complete_unfinished_repair"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Kirim & Selesaikan Perbaikan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (repair.status == "Selesai") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            if (userRole == "Kepala Unit IT") {
                                Button(
                                    onClick = { showingPinVerification = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("btn_verify_repair_acc"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verifikasi & ACC Laporan Perbaikan", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Laporan perbaikan selesai. Menunggu ACC/Verifikasi oleh Kepala Unit IT.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else if (repair.status == "Selesai & Terverifikasi") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Laporan Perbaikan Selesai & Terverifikasi oleh Kepala Unit IT (ACC)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== PART 2: ROUTINE MAINTENANCE (PERAWATAN RUTIN) ====================

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
    onExportClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val startPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 0, 0, 0)
            onStartDateChange(cal.timeInMillis)
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    val endPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 23, 59, 59)
            onEndDateChange(cal.timeInMillis)
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    // Category filter state for maintenance list
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterAllSelected by remember { mutableStateOf(true) }

    val filteredMaintenances = remember(maintenances, assets, selectedCategories, filterAllSelected) {
        maintenances.filter { maint ->
            val matchedAsset = assets.find { it.inventoryNumber == maint.inventoryNumber }
            val assetType = matchedAsset?.type ?: "Lainnya"
            filterAllSelected || selectedCategories.contains(assetType)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("maintenances_screen")
    ) {
        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 210.dp // High bottom padding to scroll past floating card
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title (scrollable)
            item {
                Text(
                    text = "Laporan Perawatan Rutin",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Header with Filter button (scrollable)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Perawatan (${filteredMaintenances.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFilterDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Kategori",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Filter",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Routine Maintenance list logic
            if (filteredMaintenances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                "Tidak ada perawatan rutin pada rentang ini",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredMaintenances, key = { it.id }) { maint ->
                    MaintItemCard(maintenance = maint, assets = assets, onClick = { onMaintClick(maint) })
                }
            }
        }

        // Floating Gradient Overlay for bottom floating Card
        val themeBgColor = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            themeBgColor.copy(alpha = 0.15f),
                            themeBgColor.copy(alpha = 0.50f),
                            themeBgColor.copy(alpha = 0.85f),
                            themeBgColor.copy(alpha = 0.95f),
                            themeBgColor
                        )
                    )
                )
        )

        // Floating Indigo/Blue Styled Range picker and action Card at bottom
        val isDark = isSystemInDarkTheme()
        val bg = MaterialTheme.colorScheme.background
        val cardColor = if (isDark) {
            androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.25f)
        } else {
            androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.07f)
        }

        val cardBorderColor = if (isDark) {
            androidx.compose.ui.graphics.lerp(cardColor, Color.Black, 0.15f)
        } else {
            androidx.compose.ui.graphics.lerp(cardColor, Color.Black, 0.12f)
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Rentang Waktu Perawatan:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { startPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_start_maint"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(format.format(Date(startDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Text(" s.d ", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { endPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_end_maint"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(format.format(Date(endDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddMaintClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_add_maintenance"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tambah Perawatan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_export_maintenances"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        val availableCategories = listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Kategori Perangkat", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filterAllSelected = !filterAllSelected
                                    if (filterAllSelected) {
                                        selectedCategories = emptySet()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = filterAllSelected,
                                onCheckedChange = { checked ->
                                    filterAllSelected = checked
                                    if (checked) {
                                        selectedCategories = emptySet()
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Semua Kategori (All)")
                        }
                    }
                    items(availableCategories) { cat ->
                        val isChecked = !filterAllSelected && selectedCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selectedCategories.toMutableSet()
                                    if (current.contains(cat)) {
                                        current.remove(cat)
                                    } else {
                                        current.add(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val current = selectedCategories.toMutableSet()
                                    if (checked) {
                                        current.add(cat)
                                    } else {
                                        current.remove(cat)
                                    }
                                    selectedCategories = current
                                    filterAllSelected = current.isEmpty()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cat)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Terapkan", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun MaintItemCard(maintenance: Maintenance, assets: List<Asset>, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(maintenance.startTime))
    val matchedAsset = assets.find { it.inventoryNumber == maintenance.inventoryNumber }
    val assetName = matchedAsset?.name ?: "Perangkat Tidak Dikenal"
    val assetType = matchedAsset?.type ?: "Lainnya"

    val statusColor = when (maintenance.status) {
        "Dalam Pengerjaan" -> Color(0xFF1976D2) // Blue
        else -> Color(0xFF2E7D32) // Green (Selesai, Selesai & Terverifikasi)
    }

    // Dynamic blending color that guarantees slightly darker background dynamically in both themes
    val isDark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    val cardColor = if (isDark) {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.25f)
    } else {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.07f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("maint_card_${maintenance.id}"),
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
                        text = "Inventaris: ${maintenance.inventoryNumber}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Kategori: $assetType",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = maintenance.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thin Horizontal Divider Line like main assets card style
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Second Row: Tindakan (on the bottom left) and Teknisi & Date (on the bottom right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tindakan
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "Tindakan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = maintenance.actionTaken,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Teknisi
                Column(
                    modifier = Modifier.weight(0.8f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Teknisi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = maintenance.technician,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceForm(
    assetList: List<Asset>,
    onSave: (Maintenance) -> Unit,
    onCancel: () -> Unit,
    onOpenScanner: ((String) -> Unit) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    initialInventoryNumber: String? = null,
    currentUser: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var invNum by remember { mutableStateOf(initialInventoryNumber ?: "") }
    var actionTaken by remember { mutableStateOf("") }
    var issuesFound by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Kondisi Prima & Normal") }
    
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    val statusOptions = listOf("Dalam Pengerjaan", "Selesai")
    var statusExpanded by remember { mutableStateOf(false) }

    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf(currentUser) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("add_maint_form"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Input Perawatan Rutin Perangkat",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF0277BD)
            )
            Text(
                "Masukkan data aktivitas pembersihan, kalibrasi, cek berkala perangkat IT.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Device selection via typing or simulator scanning
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Kaitkan Inventaris Perangkat",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = invNum,
                        onValueChange = { invNum = it.trim().uppercase() },
                        label = { Text("Nomor Inventaris Perangkat") },
                        placeholder = { Text("No. Inventaris (Contoh: INV-PR-004)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onOpenScanner { code ->
                                        invNum = code.trim().uppercase()
                                    }
                                },
                                modifier = Modifier.testTag("maint_btn_inv_scan")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR/Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("maint_tf_inv"),
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) }
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = actionTaken,
                onValueChange = { actionTaken = it },
                label = { Text("Tindakan yang Dilakukan (Wajib jika selesai)") },
                placeholder = { Text("Contoh: Pembersihan debu fisik CPU, defrag hardisk, pengecekan tinta printer.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("maint_tf_action")
            )
        }

        item {
            OutlinedTextField(
                value = issuesFound,
                onValueChange = { issuesFound = it },
                label = { Text("Kendala yang Ditemukan (Jika Ada)") },
                placeholder = { Text("Contoh: Nozzle printer warna merah kotor buntu, fan berbunyi berisik.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("maint_tf_issue")
            )
        }

        item {
            OutlinedTextField(
                value = result,
                onValueChange = { result = it },
                label = { Text("Hasil Perawatan / Rekomendasi") },
                placeholder = { Text("Contoh: Lolos cetak test page kualifikasi normal, disarankan ganti kipas bulan depan.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("maint_tf_result")
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status Perawatan") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("maint_tf_status")
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statusOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                status = opt
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = technician,
                onValueChange = { technician = it },
                label = { Text("Teknisi Penanggung Jawab") },
                placeholder = { Text("Nama Lengkap") },
                singleLine = true,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("maint_tf_tech"),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
        }

        // Camera Simulated Photo Pickers (Maintenance 3 Photo Workflow)
        item {
            val matchedAsset = assetList.find { it.inventoryNumber == invNum }
            val assetLocation = matchedAsset?.location ?: "Gedung IT"
            val currentDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Watermarked Photo Verification Flow",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Semua foto dilengkapi tanda air berisi Lokasi perangkat ($assetLocation) dan Tanggal ($currentDateStr). Pengambilan dari galeri dinonaktifkan demi keaslian data Tiruan.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    // 1. Photo Before
                    Column {
                        Text("1. Foto Sebelum Perawatan (Before) *Wajib", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        if (!photoBefore.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoBefore, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoBefore = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoBefore = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sebelum / Mulai Perawatan")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 2. Photo After
                    Column {
                        Text(
                            text = "2. Foto Sesudah Perawatan (After) " + if (status == "Selesai") "*Wajib" else "(Opsional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (status == "Selesai" && photoAfter.isNullOrBlank()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!photoAfter.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoAfter, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoAfter = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoAfter = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                enabled = status == "Selesai" || photoBefore != null
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sesudah Perawatan Selesai")
                            }
                        }
                    }

                    HorizontalDivider()

                    // 3. Photo with User
                    Column {
                        Text(
                            text = "3. Foto Bersama Tim & Unit " + if (status == "Selesai") "*Wajib" else "(Opsional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (status == "Selesai" && photoUser.isNullOrBlank()) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                        Spacer(Modifier.height(4.dp))
                        if (!photoUser.isNullOrBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                WatermarkedAsyncImage(photoUser, assetLocation, modifier = Modifier.fillMaxSize())
                                IconButton(
                                    onClick = { photoUser = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        } else {
                            Button(
                                onClick = { onOpenCamera { photo -> photoUser = photo } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = status == "Selesai" || photoBefore != null
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Bukti Bersama Unit")
                            }
                        }
                    }
                }
            }
        }

        item {
            val isFormValid = invNum.isNotBlank() && technician.isNotBlank() &&
                    !photoBefore.isNullOrBlank() &&
                    (status != "Selesai" || (actionTaken.isNotBlank() && !photoAfter.isNullOrBlank() && !photoUser.isNullOrBlank()))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onCancel()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_cancel_maint")
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        val maint = Maintenance(
                            inventoryNumber = invNum,
                            startTime = System.currentTimeMillis() - 3600 * 1000,
                            endTime = if (status == "Selesai") System.currentTimeMillis() else null,
                            actionTaken = actionTaken,
                            issuesFound = issuesFound,
                            result = result,
                            status = status,
                            photoBefore = photoBefore,
                            photoAfter = photoAfter,
                            photoUser = photoUser,
                            technician = technician
                        )
                        onSave(maint)
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_save_maint")
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}

@Composable
fun MaintenanceDetailDialog(
    maintenance: Maintenance,
    onDismiss: () -> Unit,
    onUpdateMaintenance: (Maintenance) -> Unit,
    onOpenCamera: ((String) -> Unit) -> Unit,
    userRole: String,
    onVerifyMaintenance: (Maintenance) -> Unit,
    viewModel: ITViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isUnfinished = maintenance.status == "Dalam Pengerjaan"

    // Edit states for concluding the maintenance activity
    var localActionTaken by remember { mutableStateOf(maintenance.actionTaken) }
    var localIssuesFound by remember { mutableStateOf(maintenance.issuesFound) }
    var localResult by remember { mutableStateOf(maintenance.result.ifBlank { "Kondisi Prima & Normal" }) }
    var localPhotoAfter by remember { mutableStateOf<String?>(maintenance.photoAfter) }
    var localPhotoUser by remember { mutableStateOf<String?>(maintenance.photoUser) }

    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }

    var showingPinVerification by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val matchedAsset = viewModel.allAssets.collectAsStateWithLifecycle(emptyList()).value
        .find { it.inventoryNumber == maintenance.inventoryNumber }
    val assetLocation = matchedAsset?.location ?: "Gedung IT"

    if (showingPinVerification) {
        PinVerificationDialog(
            viewModel = viewModel,
            onDismiss = { showingPinVerification = false },
            onPinCorrect = {
                showingPinVerification = false
                val finalMaint = maintenance.copy(
                    status = "Selesai & Terverifikasi",
                    actionTaken = localActionTaken.ifBlank { maintenance.actionTaken },
                    issuesFound = localIssuesFound.ifBlank { maintenance.issuesFound },
                    result = localResult.ifBlank { maintenance.result },
                    photoAfter = localPhotoAfter ?: maintenance.photoAfter,
                    photoUser = localPhotoUser ?: maintenance.photoUser
                )
                onVerifyMaintenance(finalMaint)
            }
        )
    }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detail Perawatan Rutin",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF0277BD)
                    )
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Aset yang Dirawat:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(maintenance.inventoryNumber, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0277BD))
                                Spacer(Modifier.height(8.dp))
                                RowValue("Waktu Mulai", dateFormatter.format(Date(maintenance.startTime)))
                                RowValue("Waktu Selesai", maintenance.endTime?.let { dateFormatter.format(Date(it)) } ?: "Masih Tertunda (Dalam Pengerjaan)")
                                RowValue("Teknisi", maintenance.technician)
                            }
                        }
                    }

                    item {
                        DetailSection("Status Perawatan", maintenance.status) {
                            val color = if (isUnfinished) Color(0xFF0277BD) else Color(0xFF2E7D32)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    maintenance.status,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (maintenance.status == "Selesai" || maintenance.status == "Selesai & Terverifikasi") {
                        item {
                            DetailTextSection("Tindakan yang Dilakukan", maintenance.actionTaken)
                        }

                        item {
                            DetailTextSection("Kendala Temuan", maintenance.issuesFound.ifBlank { "Tidak Ada / Bersih" })
                        }

                        item {
                            DetailTextSection("Hasil Akhir", maintenance.result)
                        }
                    }

                    // Photo Display (Before / After / User)
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Laporan Bukti Foto Sesuai SOP:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                // Before Photo displays
                                Text("📸 Foto Sebelum (Before)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                WatermarkedAsyncImage(
                                    photoStr = maintenance.photoBefore,
                                    defaultLocation = "IT Department",
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                )

                                if (maintenance.status == "Selesai" || maintenance.status == "Selesai & Terverifikasi") {
                                    Text("📸 Foto Sesudah (After)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = maintenance.photoAfter,
                                        defaultLocation = "IT Department",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )

                                    Text("📸 Foto Bersama Unit (Serah Terima)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    WatermarkedAsyncImage(
                                        photoStr = maintenance.photoUser,
                                        defaultLocation = "IT Department",
                                        modifier = Modifier.fillMaxWidth().height(130.dp)
                                    )
                                }
                            }
                        }
                    }

                    // INTERACTIVE RESOLUTION FOR UNFINISHED MAINENANCE
                    if (isUnfinished) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Form Penyelesaian Perawatan",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0277BD)
                                    )
                                    Text(
                                        "Lengkapi data pemeriksaan akhir secara real-time dan tangkap foto hasil verifikasi.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = localActionTaken,
                                        onValueChange = { localActionTaken = it },
                                        label = { Text("Tindakan Akhir *Wajib") },
                                        placeholder = { Text("Tindakan pembersihan, reparasi kecil, dll.") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    OutlinedTextField(
                                        value = localIssuesFound,
                                        onValueChange = { localIssuesFound = it },
                                        label = { Text("Kendala Ditemukan (Opsional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    OutlinedTextField(
                                        value = localResult,
                                        onValueChange = { localResult = it },
                                        label = { Text("Hasil Perawatan / Hasil Akhir") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent)
                                    )

                                    // Capture photo after
                                    Text("📸 Ambil Foto Sesudah (After) *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoAfter.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoAfter, assetLocation, modifier = Modifier.fillMaxSize())
                                            IconButton(
                                                onClick = { localPhotoAfter = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(0.6f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenCamera { photo -> localPhotoAfter = photo } },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ambil Foto Sesudah", fontSize = 11.sp)
                                        }
                                    }

                                    // Capture photo user
                                    Text("📸 Ambil Foto Bersama Unit *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoUser.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoUser, assetLocation, modifier = Modifier.fillMaxSize())
                                            IconButton(
                                                onClick = { localPhotoUser = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(0.6f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { onOpenCamera { photo -> localPhotoUser = photo } },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Ambil Foto Bersama Unit", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    val isResolveEnabled = localActionTaken.isNotBlank() &&
                                            !localPhotoAfter.isNullOrBlank() &&
                                            !localPhotoUser.isNullOrBlank()

                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                            val updatedMaint = maintenance.copy(
                                                status = "Selesai",
                                                endTime = System.currentTimeMillis(),
                                                actionTaken = localActionTaken,
                                                issuesFound = localIssuesFound,
                                                result = localResult,
                                                photoAfter = localPhotoAfter,
                                                photoUser = localPhotoUser
                                            )
                                            onUpdateMaintenance(updatedMaint)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = isResolveEnabled,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("btn_complete_unfinished_maint"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Kirim & Selesaikan Perawatan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (maintenance.status == "Selesai") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            if (userRole == "Kepala Unit IT") {
                                Button(
                                    onClick = { showingPinVerification = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("btn_verify_maint_acc"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Verifikasi & ACC Laporan Perawatan", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Laporan perawatan selesai. Menunggu ACC/Verifikasi oleh Kepala Unit IT.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else if (maintenance.status == "Selesai & Terverifikasi") {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Laporan Perawatan Selesai & Terverifikasi oleh Kepala Unit IT (ACC)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== REUSABLE DIALOGS & SHARDS ====================

@Composable
fun DetailSection(label: String, valRaw: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
fun DetailTextSection(label: String, text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun circleShape(): RoundedCornerShape = RoundedCornerShape(50)

// Simulated Barcode Scanner with dynamic overlay lines, scan line and list of registerable device items to click & pick
@Composable
fun BarcodeScannerDialog(
    assetList: List<Asset>,
    onAssetSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }

    val qrScanner = remember {
        try {
            val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS)
                .build()
            com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val launchScanner = {
        if (qrScanner != null) {
            try {
                qrScanner.startScan()
                    .addOnSuccessListener { barcode ->
                        val result = barcode.rawValue
                        if (!result.isNullOrBlank()) {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onAssetSelected(result)
                        }
                    }
                    .addOnFailureListener { e ->
                        scanError = "Pindai gagal atau dibatalkan."
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                scanError = "Layanan pemindai tidak tersedia di perangkat ini."
            }
        } else {
            scanError = "Fitur scan tidak didukung di perangkat ini (GMS tidak tersedia)."
        }
    }

    // Proactively launch scanner when dialog first opens
    LaunchedEffect(Unit) {
        launchScanner()
    }

    Dialog(onDismissRequest = {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Pemindai Barcode & QR Code",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Menggunakan kamera internal Google Play Services untuk membaca kode UTF-8.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Beautiful Scan Visualizer Frame with laser line on top
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clickable { launchScanner() }
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Dotted corners & animated lasers
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = Color.Green,
                            size = size,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                        // Laser red horizontal line in center
                        drawLine(
                            color = Color.Red,
                            start = Offset(10f, size.height / 2),
                            end = Offset(size.width - 10f, size.height / 2),
                            strokeWidth = 6f
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White.copy(0.8f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "KETUK UNTUK PINDAI",
                            color = Color.Green,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (scanError != null) {
                    Text(
                        scanError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Button(
                    onClick = { launchScanner() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Kamera Scan QR", fontSize = 13.sp)
                }

                // Asset selection from registered DB to "simulate a successful scan"
                Text(
                    "Ketuk aset untuk menyelesaikan pemindaian:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(6.dp))

                if (assetList.isEmpty()) {
                    Text(
                        "Belum ada inventaris. Ketik kode manual di bawah ini.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                                RoundedCornerShape(8.dp)
                             )
                            .padding(4.dp)
                    ) {
                        items(assetList) { asset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        onAssetSelected(asset.inventoryNumber)
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(asset.inventoryNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                Text(asset.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Manual manual inputs backup
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it.uppercase(Locale.getDefault()) },
                    label = { Text("Input Nomor Inventaris Manual") },
                    placeholder = { Text("Contoh: INV-PC-102") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_tf_manual"),
                    trailingIcon = {
                        if (textInput.isNotBlank()) {
                            IconButton(onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                onAssetSelected(textInput)
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Simpan", tint = Color.Green)
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}

// Camera Simulation Dialog allowing tech to select from standard visual presets corresponding to standard IT maintenance / repair faults
@Composable
fun CameraSimulationDialog(
    onPhotoCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()) }

    // 1. Camera taking launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val compressedBase64 = compressAndWatermarkBitmap(
                bitmap = bitmap,
                location = "Kamera Perangkat IT Support",
                dateStr = currentDateStr
            )
            onPhotoCaptured(compressedBase64)
        }
    }

    // 2. Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                val compressedBase64 = compressAndWatermarkBitmap(
                    bitmap = bitmap,
                    location = "Galeri Perangkat IT Support",
                    dateStr = currentDateStr
                )
                onPhotoCaptured(compressedBase64)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ambil Foto Bukti IT Support",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Gunakan kamera asli, galeri HP, atau pilih preset visual di bawah untuk demonstrasi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Row of main actions for Camera / Gallery
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch() },
                        modifier = Modifier.weight(1f).testTag("btn_real_camera"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kamera", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("btn_real_gallery"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Galeri HP", fontSize = 12.sp)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(0.12f), modifier = Modifier.padding(bottom = 12.dp))

                Text(
                    "Preset Visual Simulasi (Emulator)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(MOCK_PHOTOS) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPhotoCaptured(item.url) }
                                .testTag("btn_capture_mock_${item.title.replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.url,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Preset: ${item.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                }
            }
        }
    }
}

@Composable
fun PinVerificationDialog(
    viewModel: ITViewModel,
    onDismiss: () -> Unit,
    onPinCorrect: () -> Unit
) {
    val usersState = viewModel.allUsers.collectAsStateWithLifecycle(emptyList())
    val kepalaUnit = usersState.value.find { it.role == "Kepala Unit IT" }

    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Check if biometric is enabled on Kepala Unit IT
    val isBiometricEnabled = kepalaUnit?.isBiometricEnabled == true

    LaunchedEffect(isBiometricEnabled, kepalaUnit) {
        if (isBiometricEnabled && activity != null) {
            showBiometricPrompt(
                activity = activity,
                title = "Verifikasi Sidik Jari",
                subtitle = "Verifikasi oleh Kepala Unit IT",
                description = "Sentuh sensor sidik jari perangkat Anda untuk memverifikasi dan meng-acc.",
                onSuccess = {
                    onPinCorrect()
                },
                onError = { err ->
                    // Fall back to manual PIN entry, no error state triggered unless they fail PIN
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verifikasi Kepala Unit IT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                Text("Masukkan PIN 6-digit untuk meng-acc/memvalidasi laporan ini.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 6) {
                            pin = newVal
                            showError = false
                        }
                    },
                    label = { Text("PIN Keamanan") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pin_verification_input"),
                    isError = showError
                )

                if (isBiometricEnabled && activity != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showBiometricPrompt(
                                activity = activity,
                                title = "Verifikasi Sidik Jari",
                                subtitle = "Verifikasi oleh Kepala Unit IT",
                                description = "Sentuh sensor sidik jari perangkat Anda.",
                                onSuccess = { onPinCorrect() },
                                onError = { valMsg -> Toast.makeText(context, valMsg, Toast.LENGTH_SHORT).show() }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Verifikasi dengan Sidik Jari")
                    }
                }

                if (showError) {
                    Text(
                        text = "PIN salah! Hanya Kepala Unit IT yang dapat memverifikasi.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val correctPin = kepalaUnit?.pin ?: "123456"
                    if (pin == correctPin) {
                        onPinCorrect()
                    } else {
                        showError = true
                    }
                },
                enabled = pin.length == 6,
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                Text("Verifikasi & ACC")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ITViewModel,
    onLoginSuccess: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val usersState = viewModel.allUsers.collectAsStateWithLifecycle(emptyList())

    // Find if there is any user with biometric enabled
    val biometricUsers = usersState.value.filter { it.isBiometricEnabled }
    val hasBiometricUser = biometricUsers.isNotEmpty()

    // Start biometric auth on tap/launch if enabled
    fun triggerBiometricLogin() {
        if (activity != null && hasBiometricUser) {
            showBiometricPrompt(
                activity = activity,
                title = "Login Sidik Jari",
                subtitle = "Masuk ke IT Support Service",
                description = "Sentuh sensor sidik jari perangkat Anda untuk login cepat.",
                onSuccess = {
                    // Log in as the first biometric user (or the one matching username if they entered one)
                    val targetUser = if (username.isNotBlank()) {
                        biometricUsers.find { it.username == username.trim().lowercase() } ?: biometricUsers.first()
                    } else {
                        biometricUsers.first()
                    }
                    onLoginSuccess(targetUser.username, targetUser.role)
                    Toast.makeText(context, "Selamat datang kembali, ${targetUser.name}!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    Toast.makeText(context, "Gagal sidik jari: $err", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    var hasAutoTriggeredBiometric by remember { mutableStateOf(false) }
    LaunchedEffect(usersState.value) {
        if (!hasAutoTriggeredBiometric && usersState.value.isNotEmpty() && hasBiometricUser) {
            hasAutoTriggeredBiometric = true
            triggerBiometricLogin()
        }
    }

    val isDark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    val cardColor = if (isDark) {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.15f)
    } else {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(56.dp)
                    )
                }

                Text(
                    text = "IT Support Service",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Silakan masuk dengan akun IT Anda",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        showError = false
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("login_username_input"),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 6) {
                            password = newVal
                            showError = false
                        }
                    },
                    label = { Text("PIN Keamanan (6 Digit)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = null)
                        }
                    }
                )

                if (showError) {
                    Text(
                        text = "Username atau PIN salah!",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val u = username.trim().lowercase()
                            val users = usersState.value
                            val match = users.find { it.username == u && it.pin == password }
                            if (match != null) {
                                onLoginSuccess(match.username, match.role)
                                Toast.makeText(context, "Selamat datang, ${match.name}!", Toast.LENGTH_SHORT).show()
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_login_submit"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = username.isNotBlank() && password.length == 6
                    ) {
                        Text("Masuk", fontWeight = FontWeight.Bold)
                    }

                    if (hasBiometricUser && activity != null) {
                        Button(
                            onClick = { triggerBiometricLogin() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("btn_login_biometric"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Masuk Sidik Jari")
                        }
                    }
                }
            }
        }
    }
}

fun showBiometricPrompt(
    activity: androidx.fragment.app.FragmentActivity,
    title: String,
    subtitle: String,
    description: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor: java.util.concurrent.Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Sidik jari tidak dikenali.")
            }
        })

    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setDescription(description)
        .setNegativeButtonText("Batal")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onError("Gagal memulai autentikasi sidik jari: ${e.message}")
    }
}

@Composable
fun UserManagementScreen(
    users: List<com.example.data.model.User>,
    onSaveUser: (com.example.data.model.User) -> Unit,
    onDeleteUser: (com.example.data.model.User) -> Unit,
    onExportAllLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    var editingUser by remember { mutableStateOf<com.example.data.model.User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    val cardColor = if (isDark) {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.15f)
    } else {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.05f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag("user_management_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Manajemen Akun IT",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            "Mengelola otentikasi PIN 6-digit dan login sidik jari unit/personal.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(users, key = { it.username }) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingUser = user },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = user.role,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (user.isBiometricEnabled) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Sidik Jari Aktif",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            IconButton(onClick = { editingUser = user }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary)
                            }
                            if (user.username != "sumayasa" && user.username != "deaget") {
                                IconButton(onClick = { onDeleteUser(user) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus User", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tambah User / Username Baru", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }

    if (showAddDialog) {
        UserEditorDialog(
            user = null,
            onDismiss = { showAddDialog = false },
            onSave = { savedUser ->
                onSaveUser(savedUser)
                showAddDialog = false
            },
            activity = activity
        )
    }

    editingUser?.let { user ->
        UserEditorDialog(
            user = user,
            onDismiss = { editingUser = null },
            onSave = { savedUser ->
                onSaveUser(savedUser)
                editingUser = null
            },
            activity = activity
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditorDialog(
    user: com.example.data.model.User?,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.User) -> Unit,
    activity: androidx.fragment.app.FragmentActivity?
) {
    var username by remember { mutableStateOf(user?.username ?: "") }
    var name by remember { mutableStateOf(user?.name ?: "") }
    var pin by remember { mutableStateOf(user?.pin ?: "") }
    var role by remember { mutableStateOf(user?.role ?: "Staff IT") }
    var isBiometricEnabled by remember { mutableStateOf(user?.isBiometricEnabled ?: false) }

    val roleOptions = listOf("Kepala Unit IT", "Staff IT")
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (user == null) "Tambah Akun IT Baru" else "Edit Akun IT",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { if (user == null) username = it.trim().lowercase() },
                    label = { Text("Username") },
                    enabled = user == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 6) {
                            pin = newVal
                        }
                    },
                    label = { Text("PIN Keamanan (6 Digit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Role Akses:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        roleOptions.forEach { opt ->
                            val selected = role == opt
                            FilterChip(
                                selected = selected,
                                onClick = { role = opt },
                                label = { Text(opt) }
                            )
                        }
                    }
                }

                if (activity != null) {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Akses Sidik Jari", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Izinkan login & validasi tanpa ketik PIN", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank() || name.isBlank() || pin.length != 6) {
                        Toast.makeText(context, "Harap lengkapi semua isian (PIN harus 6 digit)!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(
                            com.example.data.model.User(
                                username = username.trim().lowercase(),
                                name = name.trim(),
                                pin = pin,
                                role = role,
                                isBiometricEnabled = isBiometricEnabled
                            )
                        )
                    }
                },
                enabled = username.isNotBlank() && name.isNotBlank() && pin.length == 6
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun PopupMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String,
    isDestructive: Boolean = false,
    isSelected: Boolean = false
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconColor = if (isDestructive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    } else if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CategoryManagementScreen(
    categories: List<com.example.data.model.Category>,
    onSaveCategory: (com.example.data.model.Category) -> Unit,
    onDeleteCategory: (com.example.data.model.Category) -> Unit
) {
    var editingCategory by remember { mutableStateOf<com.example.data.model.Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    val cardColor = if (isDark) {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.15f)
    } else {
        androidx.compose.ui.graphics.lerp(bg, Color.Black, 0.05f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag("category_management_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Manajemen Kategori",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            "Mengelola kategori perangkat dan panduan perawatan dinamis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(categories, key = { it.name }) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingCategory = category },
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            val itemsCount = if (category.guidelines.isBlank()) 0 else category.guidelines.split("||~||").size
                            Text(
                                text = "$itemsCount panduan perawatan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editingCategory = category }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Kategori", tint = MaterialTheme.colorScheme.primary)
                            }
                            if (category.name != "Laptop" && category.name != "PC Desktop" && category.name != "Printer" && category.name != "Server" && category.name != "Network Device" && category.name != "Lainnya") {
                                IconButton(onClick = { onDeleteCategory(category) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Kategori", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tambah Kategori Baru", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }

    if (showAddDialog) {
        CategoryEditorDialog(
            category = null,
            onDismiss = { showAddDialog = false },
            onSave = { saved ->
                onSaveCategory(saved)
                showAddDialog = false
            }
        )
    }

    if (editingCategory != null) {
        CategoryEditorDialog(
            category = editingCategory,
            onDismiss = { editingCategory = null },
            onSave = { saved ->
                onSaveCategory(saved)
                editingCategory = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorDialog(
    category: com.example.data.model.Category?,
    onDismiss: () -> Unit,
    onSave: (com.example.data.model.Category) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    val items = remember {
        val loaded = category?.guidelines?.split("||~||")?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateListOf<String>().apply { 
            if (loaded.isNotEmpty()) addAll(loaded) else add("") 
        }
    }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (category == null) "Tambah Kategori Baru" else "Edit Kategori",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Hal-hal yang harus diperhatikan (Dinamis):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Limit height so dialog stays within screen bounds, with vertical scrolling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 240.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    val listState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(listState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEachIndexed { index, value ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Up and Down reorder buttons
                                Column(verticalArrangement = Arrangement.Center) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val temp = items[index]
                                                items[index] = items[index - 1]
                                                items[index - 1] = temp
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Naikkan posisi",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < items.size - 1) {
                                                val temp = items[index]
                                                items[index] = items[index + 1]
                                                items[index + 1] = temp
                                            }
                                        },
                                        enabled = index < items.size - 1,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Turunkan posisi",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { items[index] = it },
                                    placeholder = { Text("Masukkan poin panduan...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { 
                                        if (items.size > 1) {
                                            items.removeAt(index)
                                        } else {
                                            items[0] = ""
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus panduan",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { items.add("") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah Panduan Baru", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val filteredGuidelines = items.map { it.trim() }.filter { it.isNotBlank() }
                    if (name.isBlank()) {
                        Toast.makeText(context, "Harap isi nama kategori!", Toast.LENGTH_SHORT).show()
                    } else if (filteredGuidelines.isEmpty()) {
                        Toast.makeText(context, "Harap isi minimal 1 panduan / hal yang diperhatikan!", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(
                            com.example.data.model.Category(
                                name = name.trim(),
                                guidelines = filteredGuidelines.joinToString("||~||")
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
