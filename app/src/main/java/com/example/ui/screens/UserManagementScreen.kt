package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.util.showBiometricPrompt

@Composable
fun UserManagementScreen(
    users: List<User>,
    onSaveUser: (User) -> Unit,
    onDeleteUser: (User) -> Unit,
    onExportAllLogs: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    var editingUser by remember { mutableStateOf<User?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val cardColor = com.example.ui.theme.AdaptiveColors.cardColorMedium()

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
    user: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
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
                            User(
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
