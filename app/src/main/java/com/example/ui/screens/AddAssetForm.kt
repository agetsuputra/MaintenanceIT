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
    var type by remember { mutableStateOf("Kategori") }
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
            .background(themeBgColor)
            .testTag("add_asset_form")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight,
                bottom = slotMetrics.anchorHeight + 16.dp,
                start = 0.dp,
                end = 0.dp
            )
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_asset_card"),
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
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
                        // Row 1: Nomor Inventaris & Scan QR (Starts at 0.dp so internal text field starts at 16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 0.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    .weight(1f)
                                    .focusRequester(invFocus)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch {
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                    .testTag("tf_inv_number")
                            )
                            
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 2: Kategori Perangkat (Tepat di bawah Nomor Inventaris sesuai spesifikasi)
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
                                            listState.animateScrollToItem(0)
                                        }
                                    }
                                    .padding(start = 16.dp, end = 0.dp)
                                    .testTag("tf_asset_type"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                TextField(
                                    value = if (type == "Kategori") "" else type,
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = {
                                        Text(
                                            text = "Kategori",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 3: Nama Perangkat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
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
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_name")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 4: Lokasi Perangkat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
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
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_location")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
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
                                .padding(start = 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            val dateText = simpleDateFormat.format(java.util.Date(acquisitionDateLong))
                            TextField(
                                value = dateText,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                placeholder = {
                                    Text(
                                        text = "Tanggal Pengadaan Aset *",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tf_asset_acquisition_date")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 6: Harga Beli
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
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
                                                listState.animateScrollToItem(0)
                                            }
                                        }
                                    }
                                    .testTag("tf_asset_purchase_price")
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )

                        // Row 7: Deskripsi / Spesifikasi (TextArea / Multiline murni)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp),
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
                                singleLine = false,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Default
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
                                    .heightIn(min = 110.dp)
                                    .focusRequester(descFocus)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            scope.launch {
                                                listState.animateScrollToItem(0)
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

        // Floating Row of cancel and save buttons following the keyboard
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
                    type != "Kategori" && type.isNotBlank() &&
                    assetList.none { it.inventoryNumber.equals(invNum, ignoreCase = true) }

            // Tombol Batal: flat borderless
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                    onCancel()
                },
                shape = RoundedCornerShape(12.dp),
                border = null,
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
                Text("Batal", fontWeight = FontWeight.Bold)
            }

            // Tombol Simpan: flat borderless
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
                border = null,
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeBgColor,
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = themeBgColor,
                    disabledContentColor = if (isDark) Color(0xFF555555) else Color(0xFFB0B0B0)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("btn_save_asset")
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        }
    }
}
