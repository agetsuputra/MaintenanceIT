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
                OutlinedTextField(
                    value = invNum,
                    onValueChange = { invNum = capitalizeFirstLetter(it.trim().uppercase(Locale.getDefault())) },
                    label = { Text("Nomor Inventaris Aset *") },
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
                    label = { Text("Nama Perangkat *") },
                    placeholder = { Text("Contoh: iMac Pro Retina 2024") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { locationFocus.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateScrollToItem(2)
                                }
                            }
                        }
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
                            .clickable {
                                categoryExpanded = !categoryExpanded
                                scope.launch {
                                    listState.animateScrollToItem(3)
                                }
                            }
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
                    label = { Text("Lokasi Perangkat *") },
                    placeholder = { Text("Contoh: Ruang Meeting Lt. 2") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { priceFocus.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(locationFocus)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateScrollToItem(4)
                                }
                            }
                        }
                        .testTag("tf_asset_location")
                )
            }

            item {
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(5)
                            }
                            datePickerDialog.show()
                        }
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { descFocus.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(priceFocus)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateScrollToItem(6)
                                }
                            }
                        }
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
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .focusRequester(descFocus)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    listState.animateScrollToItem(7)
                                }
                            }
                        }
                        .testTag("tf_asset_desc")
                )
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
