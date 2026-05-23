package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.data.model.Maintenance
import com.example.data.model.Repair
import com.example.ui.viewmodel.ITViewModel
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
    Perawatan
}

enum class SubScreen {
    List,
    AddAsset,
    AddRepair,
    AddMaintenance
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ITViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.Dashboard) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.List) }

    // Seed preview database if empty
    LaunchedEffect(Unit) {
        viewModel.seedSampleDataIfEmpty()
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
    var showingCameraSimulation by remember { mutableStateOf<((String) -> Unit)?>(null) } // callback function

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.testTag("main_screen_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "IT Support Service",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Record & Maintenance Hub",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.seedSampleDataIfEmpty()
                            Toast.makeText(context, "Data sample berhasil ditambahkan jika kosong!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_seed")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Seed Sample Data")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.Dashboard,
                    onClick = {
                        currentTab = AppTab.Dashboard
                        currentSubScreen = SubScreen.List
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dasbor") },
                    label = { Text("Dasbor & Aset", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.Perbaikan,
                    onClick = {
                        currentTab = AppTab.Perbaikan
                        currentSubScreen = SubScreen.List
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Perbaikan") },
                    label = { Text("Perbaikan", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav_tab_perbaikan")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.Perawatan,
                    onClick = {
                        currentTab = AppTab.Perawatan
                        currentSubScreen = SubScreen.List
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Perawatan") },
                    label = { Text("Rutin", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.testTag("nav_tab_perawatan")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                AppTab.Dashboard -> {
                    when (currentSubScreen) {
                        SubScreen.List -> {
                            DashboardScreen(
                                assets = assets,
                                repairs = repairs,
                                isFirebaseEnabled = viewModel.isFirebaseEnabled,
                                onAddAssetClick = { currentSubScreen = SubScreen.AddAsset },
                                onAssetClick = { asset -> showingAssetDetail = asset },
                                onExportClick = {
                                    val f = viewModel.exportToExcel(context, "assets")
                                    if (f != null) {
                                        viewModel.shareExportFile(context, f)
                                    } else {
                                        Toast.makeText(context, "Ekspor gagal!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        SubScreen.AddAsset -> {
                            AddAssetForm(
                                onSave = { asset ->
                                    viewModel.saveAsset(asset) {
                                        currentSubScreen = SubScreen.List
                                        Toast.makeText(context, "Aset berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCancel = { currentSubScreen = SubScreen.List },
                                assetList = assets
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
                                startDate = startDate,
                                endDate = endDate,
                                onStartDateChange = { viewModel.filterStartDate.value = it },
                                onEndDateChange = { viewModel.filterEndDate.value = it },
                                onRepairClick = { showingRepairDetail = it },
                                onAddRepairClick = { currentSubScreen = SubScreen.AddRepair },
                                onExportClick = {
                                    val f = viewModel.exportToExcel(context, "repairs")
                                    if (f != null) {
                                        viewModel.shareExportFile(context, f)
                                    } else {
                                        Toast.makeText(context, "Ekspor gagal atau rentang tanggal kosong!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        SubScreen.AddRepair -> {
                            AddRepairForm(
                                assetList = assets,
                                onSave = { repair ->
                                    viewModel.saveRepair(repair) {
                                        currentSubScreen = SubScreen.List
                                        Toast.makeText(context, "Perbaikan berhasil didaftarkan!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCancel = { currentSubScreen = SubScreen.List },
                                onOpenScanner = { callback -> showingBarcodeScanner = callback },
                                onOpenCamera = { callback -> showingCameraSimulation = callback }
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
                                startDate = startDate,
                                endDate = endDate,
                                onStartDateChange = { viewModel.filterStartDate.value = it },
                                onEndDateChange = { viewModel.filterEndDate.value = it },
                                onMaintClick = { showingMaintenanceDetail = it },
                                onAddMaintClick = { currentSubScreen = SubScreen.AddMaintenance },
                                onExportClick = {
                                    val f = viewModel.exportToExcel(context, "maintenances")
                                    if (f != null) {
                                        viewModel.shareExportFile(context, f)
                                    } else {
                                        Toast.makeText(context, "Ekspor gagal atau rentang tanggal kosong!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        SubScreen.AddMaintenance -> {
                            AddMaintenanceForm(
                                assetList = assets,
                                onSave = { maint ->
                                    viewModel.saveMaintenance(maint) {
                                        currentSubScreen = SubScreen.List
                                        Toast.makeText(context, "Pencatatan perawatan rutin disimpan!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onCancel = { currentSubScreen = SubScreen.List },
                                onOpenScanner = { callback -> showingBarcodeScanner = callback },
                                onOpenCamera = { callback -> showingCameraSimulation = callback }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // Modal Dialogs & Sheets
    showingAssetDetail?.let { asset ->
        val assetRepairsState = viewModel.getRepairsForAsset(asset.inventoryNumber).collectAsStateWithLifecycle(emptyList())
        val assetMaintsState = viewModel.getMaintenancesForAsset(asset.inventoryNumber).collectAsStateWithLifecycle(emptyList())

        AssetDetailDialog(
            asset = asset,
            repairs = assetRepairsState.value,
            maintenances = assetMaintsState.value,
            onDismiss = { showingAssetDetail = null },
            onAddRepairDirectly = {
                showingAssetDetail = null
                currentTab = AppTab.Perbaikan
                currentSubScreen = SubScreen.AddRepair
            },
            onAddMaintDirectly = {
                showingAssetDetail = null
                currentTab = AppTab.Perawatan
                currentSubScreen = SubScreen.AddMaintenance
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
            onOpenCamera = { callback -> showingCameraSimulation = callback }
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
            onOpenCamera = { callback -> showingCameraSimulation = callback }
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

    // Photo Capture Simulator Modal
    showingCameraSimulation?.let { callback ->
        CameraSimulationDialog(
            onPhotoCaptured = { photoUrl ->
                callback(photoUrl)
                showingCameraSimulation = null
            },
            onDismiss = { showingCameraSimulation = null }
        )
    }
}

// ==================== DASHBOARD & INVENTORY ====================

@Composable
fun DashboardScreen(
    assets: List<Asset>,
    repairs: List<Repair>,
    isFirebaseEnabled: Boolean,
    onAddAssetClick: () -> Unit,
    onAssetClick: (Asset) -> Unit,
    onExportClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredAssets = assets.filter {
        it.inventoryNumber.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.location.contains(searchQuery, ignoreCase = true)
    }

    // Count statistics
    val totalAssets = assets.size
    val activeAssets = assets.count { it.status == "Aktif" }
    val brokenAssets = assets.count { it.status == "Rusak" }
    val holdRepairs = assets.count { it.status == "Hold" }
    val maintenanceAssets = assets.count { it.status == "Perawatan" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_column"),
        contentPadding = PaddingValues(16.dp),
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
                        Box(
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
                            Text(
                                text = if (isFirebaseEnabled) "🌐 Cloud Sync Aktif" else "💾 Database Lokal",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFirebaseEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        "Kelola inventaris, rekam perbaikan kerusakan, dan pelihara status perangkat dengan ringkas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    // Grid stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Total Aset",
                            value = totalAssets.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Aktif",
                            value = activeAssets.toString(),
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Rusak",
                            value = brokenAssets.toString(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Hold / Maint",
                            value = (holdRepairs + maintenanceAssets).toString(),
                            color = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddAssetClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_asset"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Aset")
                }

                OutlinedButton(
                    onClick = onExportClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_export_assets"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Ekspor")
                    Spacer(Modifier.width(8.dp))
                    Text("Ekspor Excel")
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari aset berdasarkan kode, nama, lokasi...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tf_search_asset")
                    .clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        // Heading
        item {
            Text(
                "Daftar Seluruh Perangkat (${filteredAssets.size})",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
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
                            "Ketuk tombol 'Tambah Aset' di atas untuk mendaftarkan aset.",
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
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("asset_card_${asset.inventoryNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon representing asset type with matching theme color
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.12f), circleShape())
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (asset.type) {
                    "Laptop" -> Icons.Default.Computer
                    "PC Desktop" -> Icons.Default.Computer
                    "Printer" -> Icons.Default.Print
                    "Network Device" -> Icons.Default.Router
                    else -> Icons.Default.Build
                }
                Icon(icon, contentDescription = null, tint = statusColor)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: ${asset.inventoryNumber}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = asset.location,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status Badge
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = asset.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Details",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Dialog to input new Asset
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetForm(onSave: (Asset) -> Unit, onCancel: () -> Unit, assetList: List<Asset>) {
    var invNum by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Laptop") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Laptop", "PC Desktop", "Printer", "Network Device", "Server", "Lainnya")
    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("add_asset_form")
    ) {
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
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = invNum,
                    onValueChange = { invNum = it.trim().uppercase(Locale.getDefault()) },
                    label = { Text("Nomor Inventaris Aset") },
                    placeholder = { Text("Contoh: INV-LP-025") },
                    singleLine = true,
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
                    onValueChange = { name = it },
                    label = { Text("Nama Perangkat") },
                    placeholder = { Text("Contoh: iMac Pro Retina 2024") },
                    singleLine = true,
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
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("tf_asset_type")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { selectionOption ->
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
                    onValueChange = { location = it },
                    label = { Text("Lokasi Perangkat") },
                    placeholder = { Text("Contoh: Ruang Meeting Lt. 2") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tf_asset_location")
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Spesifikasi & Keterangan Tambahan") },
                    placeholder = { Text("Prosesor, RAM, Penyimpanan, dll...") },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("tf_asset_desc")
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
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
                    onSave(Asset(invNum, name, type, location, "Aktif", description))
                },
                shape = RoundedCornerShape(12.dp),
                enabled = isValid,
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
    onDismiss: () -> Unit,
    onAddRepairDirectly: () -> Unit,
    onAddMaintDirectly: () -> Unit
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
                                RowValue("Kategori", asset.type)
                                RowValue("Lokasi", asset.location)
                                RowValue("Status", asset.status)
                                RowValue("Spesifikasi", asset.description ?: "-")
                            }
                        }
                    }

                    // Direct action options
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAddRepairDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lapor Rusak", fontSize = 11.sp, maxLines = 1)
                            }
                            Button(
                                onClick = onAddMaintDirectly,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rawat Rutin", fontSize = 11.sp, maxLines = 1)
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

                    if (repairs.isEmpty() && maintenances.isEmpty()) {
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
    startDate: Long,
    endDate: Long,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onRepairClick: (Repair) -> Unit,
    onAddRepairClick: () -> Unit,
    onExportClick: () -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("repairs_screen")
    ) {
        Text(
            "Laporan Perbaikan Kerusakan",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Date Picker Range Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Rentang Waktu Laporan:",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
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
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { startPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_start_date"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(format.format(Date(startDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(" s.d ", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { endPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_end_date"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(format.format(Date(endDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Daftar Perbaikan", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onExportClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_export_repairs")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ekspor CSV", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Repair list logic
        if (repairs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                    Text("Tidak ada perbaikan pada rentang ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(repairs, key = { it.id }) { repair ->
                    RepairItemCard(repair = repair, onClick = { onRepairClick(repair) })
                }
            }
        }
    }
}

@Composable
fun RepairItemCard(repair: Repair, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(repair.startTime))
    val isHold = repair.status == "Hold"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("repair_card_${repair.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repair.inventoryNumber,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isHold) Color(0xFFE65100) else Color(0xFF2E7D32)).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isHold) Color(0xFFE65100).copy(alpha = 0.4f) else Color(0xFF2E7D32).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = repair.status,
                        color = if (isHold) Color(0xFFE65100) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Kendala: ${repair.problem}",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))
            if (isHold && !repair.holdReason.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Alasan Hold: ${repair.holdReason}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(2.dp))
                    Text(repair.technician, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onOpenCamera: ((String) -> Unit) -> Unit
) {
    var invNum by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var cause by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    var holdReason by remember { mutableStateOf("") }
    var holdEstimate by remember { mutableStateOf("") }
    
    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf("") }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = invNum,
                            onValueChange = { invNum = it.trim().uppercase() },
                            placeholder = { Text("No. Inventaris (Contoh: INV-PC-001)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("repair_tf_inv"),
                            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) }
                        )

                        Button(
                            onClick = {
                                onOpenScanner { code -> invNum = code }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                                .testTag("btn_scan_barcode"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Scan", fontSize = 11.sp)
                        }
                    }
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
                                onClick = { onOpenCamera { photo -> photoBefore = "$photo|||$assetLocation|||$currentDateStr" } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sebelum / Mulai Kerja")
                            }
                        }
                    }

                    Divider()

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
                                onClick = { onOpenCamera { photo -> photoAfter = "$photo|||$assetLocation|||$currentDateStr" } },
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

                    Divider()

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
                                onClick = { onOpenCamera { photo -> photoUser = "$photo|||$assetLocation|||$currentDateStr" } },
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
                    onClick = onCancel,
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
    onOpenCamera: ((String) -> Unit) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isUnfinished = repair.status == "Hold" || repair.status == "Dalam Pengerjaan"

    // Local mutable state for editing/resolving the repair
    var localCause by remember { mutableStateOf(repair.cause) }
    var localActionTaken by remember { mutableStateOf(repair.actionTaken) }
    var localPhotoAfter by remember { mutableStateOf<String?>(repair.photoAfter) }
    var localPhotoUser by remember { mutableStateOf<String?>(repair.photoUser) }

    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }

    Dialog(onDismissRequest = onDismiss) {
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
                    IconButton(onClick = onDismiss) {
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
                                Text(repair.inventoryNumber, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
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

                    if (repair.status == "Selesai") {
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

                                if (repair.status == "Selesai") {
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
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = localActionTaken,
                                        onValueChange = { localActionTaken = it },
                                        label = { Text("Solusi / Tindak Lanjut *Wajib") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                    )

                                    // Capture Photo After Slot
                                    Text("📸 Ambil Foto Sesudah (After) *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoAfter.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoAfter, "IT Desk", modifier = Modifier.fillMaxSize())
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
                                            onClick = { onOpenCamera { photo -> localPhotoAfter = "$photo|||Pekerjaan Selesai|||$currentDateStr" } },
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
                                            WatermarkedAsyncImage(localPhotoUser, "IT Desk", modifier = Modifier.fillMaxSize())
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
                                            onClick = { onOpenCamera { photo -> localPhotoUser = "$photo|||Verifikasi Unit|||$currentDateStr" } },
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
                }
            }
        }
    }
}


// ==================== PART 2: ROUTINE MAINTENANCE (PERAWATAN RUTIN) ====================

@Composable
fun MaintenanceScreen(
    maintenances: List<Maintenance>,
    startDate: Long,
    endDate: Long,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onMaintClick: (Maintenance) -> Unit,
    onAddMaintClick: () -> Unit,
    onExportClick: () -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("maintenances_screen")
    ) {
        Text(
            "Jadwal Perawatan Rutin Perangkat",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF0277BD)
        )

        // Date Picker Range Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Rentang Waktu Laporan:",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
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
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { startPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_start_maint"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(format.format(Date(startDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(" s.d ", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { endPicker.show() }
                            .padding(10.dp)
                            .testTag("btn_filter_end_maint"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(format.format(Date(endDate)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Tambah Perawatan", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onExportClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_export_maintenances")
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ekspor CSV", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (maintenances.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(54.dp))
                    Text("Tidak ada perawatan rutin pada rentang ini", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(maintenances, key = { it.id }) { maint ->
                    MaintItemCard(maintenance = maint, onClick = { onMaintClick(maint) })
                }
            }
        }
    }
}

@Composable
fun MaintItemCard(maintenance: Maintenance, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(maintenance.startTime))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("maint_card_${maintenance.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = maintenance.inventoryNumber,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0277BD)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Selesai Pemeriksaan",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Tindakan: ${maintenance.actionTaken}",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(2.dp))
                    Text(maintenance.technician, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onOpenCamera: ((String) -> Unit) -> Unit
) {
    var invNum by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("") }
    var issuesFound by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("Kondisi Prima & Normal") }
    
    var status by remember { mutableStateOf("Dalam Pengerjaan") }
    val statusOptions = listOf("Dalam Pengerjaan", "Selesai")
    var statusExpanded by remember { mutableStateOf(false) }

    var photoBefore by remember { mutableStateOf<String?>(null) }
    var photoAfter by remember { mutableStateOf<String?>(null) }
    var photoUser by remember { mutableStateOf<String?>(null) }
    
    var technician by remember { mutableStateOf("") }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = invNum,
                            onValueChange = { invNum = it.trim().uppercase() },
                            placeholder = { Text("No. Inventaris (Contoh: INV-PR-004)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("maint_tf_inv"),
                            leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) }
                        )

                        Button(
                            onClick = {
                                onOpenScanner { code -> invNum = code }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                        .weight(1.2f)
                                        .height(56.dp)
                                        .testTag("btn_maint_scan"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Scan", fontSize = 11.sp)
                        }
                    }
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
                                onClick = { onOpenCamera { photo -> photoBefore = "$photo|||$assetLocation|||$currentDateStr" } },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Ambil Foto Sebelum / Mulai Perawatan")
                            }
                        }
                    }

                    Divider()

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
                                onClick = { onOpenCamera { photo -> photoAfter = "$photo|||$assetLocation|||$currentDateStr" } },
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

                    Divider()

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
                                onClick = { onOpenCamera { photo -> photoUser = "$photo|||$assetLocation|||$currentDateStr" } },
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
                    onClick = onCancel,
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
    onOpenCamera: ((String) -> Unit) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isUnfinished = maintenance.status == "Dalam Pengerjaan"

    // Edit states for concluding the maintenance activity
    var localActionTaken by remember { mutableStateOf(maintenance.actionTaken) }
    var localIssuesFound by remember { mutableStateOf(maintenance.issuesFound) }
    var localResult by remember { mutableStateOf(maintenance.result.ifBlank { "Kondisi Prima & Normal" }) }
    var localPhotoAfter by remember { mutableStateOf<String?>(maintenance.photoAfter) }
    var localPhotoUser by remember { mutableStateOf<String?>(maintenance.photoUser) }

    val currentDateStr = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }

    Dialog(onDismissRequest = onDismiss) {
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
                    IconButton(onClick = onDismiss) {
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

                    if (maintenance.status == "Selesai") {
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

                                if (maintenance.status == "Selesai") {
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
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = localIssuesFound,
                                        onValueChange = { localIssuesFound = it },
                                        label = { Text("Kendala Ditemukan (Opsional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = localResult,
                                        onValueChange = { localResult = it },
                                        label = { Text("Hasil Perawatan / Hasil Akhir") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                    )

                                    // Capture photo after
                                    Text("📸 Ambil Foto Sesudah (After) *Wajib", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    if (!localPhotoAfter.isNullOrBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                            WatermarkedAsyncImage(localPhotoAfter, "IT Desk", modifier = Modifier.fillMaxSize())
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
                                            onClick = { onOpenCamera { photo -> localPhotoAfter = "$photo|||Perawatan Selesai|||$currentDateStr" } },
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
                                            WatermarkedAsyncImage(localPhotoUser, "IT Desk", modifier = Modifier.fillMaxSize())
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
                                            onClick = { onOpenCamera { photo -> localPhotoUser = "$photo|||Verifikasi Unit|||$currentDateStr" } },
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
    var textInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
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
                    "Simulasi Pemindai Barcode (IT)",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Menstimulasi kamera perangkat pembaca kode inventaris / QR.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Beautiful Scan Visualizer Frame with laser line on top
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .background(Color.Black.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Dotted corners & animated lasers
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
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
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(0.8f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "SCANNING...",
                            color = Color.Green,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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
                                    .clickable { onAssetSelected(asset.inventoryNumber) }
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
                            IconButton(onClick = { onAssetSelected(textInput) }) {
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
                        onClick = onDismiss,
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
