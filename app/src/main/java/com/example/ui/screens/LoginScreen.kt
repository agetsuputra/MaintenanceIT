package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.ITViewModel
import com.example.util.showBiometricPrompt

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

    val cardColor = com.example.ui.theme.AdaptiveColors.cardColorMedium()

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
