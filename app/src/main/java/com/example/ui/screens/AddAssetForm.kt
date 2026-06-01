package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Asset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val invFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val locationFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }
    val descFocus = remember { FocusRequester() }

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
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val slotMetrics = com.example.util.rememberFloatingSlotMetrics(floatingElementHeight = 56.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_asset_form")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 84.dp,
                bottom = slotMetrics.anchorHeight + 16.dp,
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_asset_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Header Row: Nomor Inventaris & Scan QR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TextField(
                                    value = invNum,
                                    onValueChange = { invNum = capitalizeFirstLetter(it.trim().uppercase(Locale.getDefault())) },
                                    placeholder = {
                                        Text(
                                            text = "Nomor Inventaris *",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { nameFocus.requestFocus() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(invFocus)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                scope.launch {
                                                    listState.animateScrollToItem(1)
                                                }
                                            }
                                        }
                                        .testTag("tf_inv_number")
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    onOpenScanner { code ->
                                        invNum = capitalizeFirstLetter(code.trim().uppercase(Locale.getDefault()))
                                    }
                                },
                                modifier = Modifier
                                    .testTag("btn_inv_number_scan")
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR/Barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (assetList.any { it.inventoryNumber.equals(invNum, ignoreCase = true) }) {
                            Text(
                                "Nomor inventaris ini sudah terdaftar!",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 2: Nama Perangkat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            TextField(
                                value = name,
                                onValueChange = { name = capitalizeFirstLetter(it) },
                                placeholder = {
                                    Text(
                                        text = "Nama Perangkat *",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { locationFocus.requestFocus() }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(nameFocus)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch {
                                                listState.animateScrollToItem(1)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_name")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 3: Kategori Perangkat
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .clickable {
                                        categoryExpanded = !categoryExpanded
                                        scope.launch {
                                            listState.animateScrollToItem(1)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("tf_asset_type"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (type.isEmpty()) {
                                        Text(
                                            text = "Kategori Perangkat",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    } else {
                                        Text(
                                            text = type,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 4: Lokasi Perangkat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            TextField(
                                value = location,
                                onValueChange = { location = capitalizeFirstLetter(it) },
                                placeholder = {
                                    Text(
                                        text = "Lokasi Perangkat *",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { priceFocus.requestFocus() }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(locationFocus)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch {
                                                listState.animateScrollToItem(1)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_location")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 5: Tanggal Pengadaan
                        val calendar = remember(acquisitionDateLong) { Calendar.getInstance().apply { timeInMillis = acquisitionDateLong } }
                        val startYear = calendar.get(Calendar.YEAR)
                        val startMonth = calendar.get(Calendar.MONTH)
                        val startDay = calendar.get(Calendar.DAY_OF_MONTH)

                        val datePickerDialog = remember(context, startYear, startMonth, startDay) {
                            DatePickerDialog(
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
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    datePickerDialog.show()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("tf_asset_acquisition_date"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                val dateText = simpleDateFormat.format(java.util.Date(acquisitionDateLong))
                                if (dateText.isEmpty()) {
                                    Text(
                                        text = "Tanggal Pengadaan Aset *",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                } else {
                                    Text(
                                        text = dateText,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 6: Harga Beli
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            TextField(
                                value = purchasePriceInput,
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }
                                    purchasePriceInput = formatThousandSeparator(clean)
                                },
                                placeholder = {
                                    Text(
                                        text = "Harga Beli (Rp) - Opsional",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { descFocus.requestFocus() }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(priceFocus)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch {
                                                listState.animateScrollToItem(1)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_purchase_price")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 7: Deskripsi / Spesifikasi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            TextField(
                                value = description,
                                onValueChange = { description = capitalizeFirstLetter(it) },
                                placeholder = {
                                    Text(
                                        text = "Spesifikasi & Keterangan Tambahan",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .focusRequester(descFocus)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch {
                                                listState.animateScrollToItem(1)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_desc")
                            )
                        }
                    }
                }
            }
        }

        // Top Gradient (starts from floating profile area and fades upward)
        com.example.ui.components.TopFadeOverlay(
            height = statusBarHeight + 72.dp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Gradient Overlay (anchored directly to the top edge of the action buttons)
        val bottomEdge = (slotMetrics.bottomOffset - 16.dp).coerceAtLeast(0.dp)
        val gradientHeight = 56.dp + 16.dp
        com.example.ui.components.BottomFadeOverlay(
            height = gradientHeight,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomEdge)
        )

        // Floating Row of cancel and save buttons following the keyboard
        // Hilangkan card induk (no background, no border, no padding/shape decoration card)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = slotMetrics.bottomOffset)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isValid = invNum.isNotBlank() && name.isNotBlank() && location.isNotBlank() &&
                    assetList.none { it.inventoryNumber.equals(invNum, ignoreCase = true) }

            // Tombol Batal: Berwarna solid non-transparan
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    onCancel()
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFD1D5DB)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeBgColor,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("btn_cancel_asset")
            ) {
                Text("Batal")
            }

            // Tombol Simpan: Berwarna solid non-transparan
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = if (isDark) Color(0xFF333333) else Color(0xFFE2E8F0),
                    disabledContentColor = if (isDark) Color(0xFF757575) else Color(0xFF94A3B8)
                ),
                elevation = null,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("btn_save_asset")
            ) {
                Text("Simpan")
            }
        }
    }
}
