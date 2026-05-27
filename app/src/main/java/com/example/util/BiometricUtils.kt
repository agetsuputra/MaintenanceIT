package com.example.util

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    description: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor: Executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Sidik jari tidak dikenali.")
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
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
