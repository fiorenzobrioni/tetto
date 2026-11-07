package com.callbackdev.tetto.core.data.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The phone's own lock, as Tetto asks for it (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): the
 * platform's [BiometricPrompt], a strong fingerprint or face, or the screen lock's PIN, pattern or
 * password, whichever the reader uses. No dependency: Android's own prompt, since API 30 able to
 * offer both, and Tetto stores no secret of its own: the phone checks, Tetto hears yes or no.
 */
@Singleton
class DeviceLock
@Inject
constructor(@ApplicationContext private val context: Context) {
    /**
     * Whether the phone has a lock Tetto can ask for: a screen lock at least. Without one there is
     * nothing to ask, and Settings says why the switch cannot be turned on.
     */
    fun available(): Boolean {
        val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return false
        val biometrics = context.getSystemService(BiometricManager::class.java) ?: return false
        return keyguard.isDeviceSecure &&
            biometrics.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Asks for the phone's lock over [activity], with [title] and [subtitle]; true once it says
     * yes, false when the reader dismisses it or the phone cannot ask. A finger not recognised is
     * not an answer: the prompt asks again by itself.
     */
    suspend fun ask(activity: Activity, title: String, subtitle: String? = null): Boolean =
        suspendCancellableCoroutine { continuation ->
            val cancel = CancellationSignal()
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .apply { if (subtitle != null) setSubtitle(subtitle) }
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setConfirmationRequired(false)
                .build()
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
            continuation.invokeOnCancellation { cancel.cancel() }
            try {
                prompt.authenticate(cancel, activity.mainExecutor, callback)
            } catch (_: SecurityException) {
                if (continuation.isActive) continuation.resume(false)
            }
        }

    companion object {
        /** A strong biometric, or the screen lock itself: never a weak one alone. */
        const val AUTHENTICATORS: Int = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }
}
