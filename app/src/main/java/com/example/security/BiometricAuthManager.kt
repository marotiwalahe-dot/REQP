package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

data class BiometricCapability(
    val isHardwareAvailable: Boolean,
    val isEnrolled: Boolean,
    val hasFingerprintHardware: Boolean,
    val hasFaceHardware: Boolean,
    val supportsDeviceCredential: Boolean,
    val statusMessage: String,
    val allowedAuthenticators: Int
)

object BiometricAuthManager {

    fun checkCapability(context: Context): BiometricCapability {
        val biometricManager = BiometricManager.from(context)
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = pm.hasSystemFeature(PackageManager.FEATURE_FACE)

        val strongOrCred = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        val weakOrCred = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

        val strongResult = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        val weakResult = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        val combinedResult = biometricManager.canAuthenticate(strongOrCred)
        val weakCombinedResult = biometricManager.canAuthenticate(weakOrCred)
        val deviceCredResult = biometricManager.canAuthenticate(DEVICE_CREDENTIAL)

        val supportsCred = deviceCredResult == BiometricManager.BIOMETRIC_SUCCESS

        return when {
            strongResult == BiometricManager.BIOMETRIC_SUCCESS ||
                combinedResult == BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricCapability(
                    isHardwareAvailable = true,
                    isEnrolled = true,
                    hasFingerprintHardware = hasFingerprint,
                    hasFaceHardware = hasFace,
                    supportsDeviceCredential = supportsCred,
                    statusMessage = "Biometric & Device Lock Ready",
                    allowedAuthenticators = strongOrCred
                )
            }
            weakResult == BiometricManager.BIOMETRIC_SUCCESS ||
                weakCombinedResult == BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricCapability(
                    isHardwareAvailable = true,
                    isEnrolled = true,
                    hasFingerprintHardware = hasFingerprint,
                    hasFaceHardware = hasFace,
                    supportsDeviceCredential = supportsCred,
                    statusMessage = "Biometric (Weak/Face) & Device Lock Ready",
                    allowedAuthenticators = weakOrCred
                )
            }
            strongResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                weakResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricCapability(
                    isHardwareAvailable = true,
                    isEnrolled = false,
                    hasFingerprintHardware = hasFingerprint,
                    hasFaceHardware = hasFace,
                    supportsDeviceCredential = supportsCred,
                    statusMessage = if (supportsCred) {
                        "No biometrics enrolled — PIN/Pattern fallback available"
                    } else {
                        "Biometric hardware detected, but no fingerprint/face is enrolled in Android Settings"
                    },
                    allowedAuthenticators = if (supportsCred) DEVICE_CREDENTIAL else strongOrCred
                )
            }
            supportsCred -> {
                BiometricCapability(
                    isHardwareAvailable = false,
                    isEnrolled = true,
                    hasFingerprintHardware = hasFingerprint,
                    hasFaceHardware = hasFace,
                    supportsDeviceCredential = true,
                    statusMessage = "Device PIN/Pattern/Password lock available",
                    allowedAuthenticators = DEVICE_CREDENTIAL
                )
            }
            else -> {
                BiometricCapability(
                    isHardwareAvailable = false,
                    isEnrolled = false,
                    hasFingerprintHardware = hasFingerprint,
                    hasFaceHardware = hasFace,
                    supportsDeviceCredential = false,
                    statusMessage = "No biometric or device screen lock configured on this device",
                    allowedAuthenticators = strongOrCred
                )
            }
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Secure Study Vault",
        subtitle: String = "Authenticate with your device fingerprint, face, or screen lock",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val capability = checkCapability(activity)
        if (!capability.isEnrolled && !capability.supportsDeviceCredential) {
            onError(capability.statusMessage)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancelled()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // BiometricPrompt handles retry UI internally; keep user informed
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(capability.allowedAuthenticators)
            .build()

        prompt.authenticate(promptInfo)
    }
}
