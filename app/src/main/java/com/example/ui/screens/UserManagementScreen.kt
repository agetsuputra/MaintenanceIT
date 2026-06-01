package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen List Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 84.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .testTag("user_management_screen")
        ) {
            // Box Header Atas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "user",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Baris Kontrol Kedua (Daftar User)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar User",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah User",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onExportAllLogs() }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Lainnya",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scroll boundary with Modifier.clipToBounds()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(users, key = { it.username }) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingUser = user }
                                .testTag("user_card_${user.username}"),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${user.username} • ${user.role}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
    var role by remember { mutableStateOf(user?.role ?: "Staff IT") }
    var isBiometricEnabled by remember { mutableStateOf(user?.isBiometricEnabled ?: false) }

    val roleOptions = listOf("Kepala Unit IT", "Staff IT", "Teknisi")
    val context = LocalContext.current

    var roleExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Content Area inside the large single Card
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    // Baris 1: Username & Header Area (No Icon, Aligned Horizontally)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(start = 66.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = username,
                            onValueChange = { if (user == null) username = it.trim().lowercase() },
                            placeholder = {
                                Text(
                                    "Username *",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            enabled = user == null,
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

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    // Baris 2: Nama Lengkap Input with Outlined.Badge Icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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
                            value = name,
                            onValueChange = { name = it },
                            placeholder = {
                                Text(
                                    "Nama Lengkap *",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
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

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    // Baris 3: PIN Input with Outlined.Lock Icon & Number/Digits Keyboard
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    // Baris 4: Hak Akses Selector with Outlined.ManageAccounts Icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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

                    // Inline Expandable Dropdown with sliding transitions
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (role == opt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (role == opt) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    // Baris 5: Login Biometrik with Outlined.Fingerprint Icon & Switch Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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

        // Flat Borderless Buttons pinned seamlessly at the bottom edge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
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
