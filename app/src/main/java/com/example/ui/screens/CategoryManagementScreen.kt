package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category

@Composable
fun CategoryManagementScreen(
    categories: List<Category>,
    onSaveCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val cardColor = com.example.ui.theme.AdaptiveColors.cardColorMedium()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 84.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
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
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
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
                            Category(
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
