package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ITViewModel
import com.example.util.showBiometricPrompt

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
