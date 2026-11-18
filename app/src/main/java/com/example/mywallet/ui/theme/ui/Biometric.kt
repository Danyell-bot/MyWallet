package com.example.mywallet.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val AUTH = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

fun findActivity(ctx: Context): FragmentActivity? {
    var c: Context? = ctx
    while (c is ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }
    return null
}

/** true when this phone has a fingerprint / face enrolled that apps can use */
fun canUseBiometric(ctx: Context): Boolean =
    try { BiometricManager.from(ctx).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS } catch (e: Exception) { false }

fun showBiometric(act: FragmentActivity, title: String, onOk: () -> Unit) {
    try {
        val prompt = BiometricPrompt(act, ContextCompat.getMainExecutor(act), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onOk() }
        })
        val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).setNegativeButtonText("Use passcode").setAllowedAuthenticators(AUTH).build()
        prompt.authenticate(info)
    } catch (e: Exception) { }
}
