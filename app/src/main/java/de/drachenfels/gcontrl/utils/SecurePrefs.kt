package de.drachenfels.gcontrl.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

private const val SECURE_PREFS_NAME = "GContrlSecurePrefs"

/**
 * Returns an EncryptedSharedPreferences instance backed by AES256-GCM key in the Android Keystore.
 * All keys are encrypted with AES256-SIV, all values with AES256-GCM.
 */
fun getSecurePrefs(context: Context): SharedPreferences {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    return EncryptedSharedPreferences.create(
        SECURE_PREFS_NAME,
        masterKeyAlias,
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
