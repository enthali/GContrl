package de.drachenfels.gcontrl.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

private const val SECURE_PREFS_NAME = "GContrlSecurePrefs"
private const val PLAIN_PREFS_NAME = "GContrlPrefs"

/**
 * Returns an EncryptedSharedPreferences instance backed by AES256-GCM key in the Android Keystore.
 * All keys are encrypted with AES256-SIV, all values with AES256-GCM.
 *
 * On first call, automatically migrates all existing values from the old plaintext
 * GContrlPrefs file and then clears it so credentials are no longer stored in plaintext.
 */
fun getSecurePrefs(context: Context): SharedPreferences {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val securePrefs = EncryptedSharedPreferences.create(
        SECURE_PREFS_NAME,
        masterKeyAlias,
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    migrateFromPlaintext(context.applicationContext, securePrefs)
    return securePrefs
}

/**
 * One-time migration: copies all entries from the old plaintext SharedPreferences
 * into the encrypted store, then clears the plaintext file.
 * No-op if the plaintext file is already empty (migration already done).
 */
@Suppress("UNCHECKED_CAST")
private fun migrateFromPlaintext(context: Context, securePrefs: SharedPreferences) {
    val oldPrefs = context.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
    val oldEntries = oldPrefs.all
    if (oldEntries.isEmpty()) return

    val editor = securePrefs.edit()
    for ((key, value) in oldEntries) {
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
            is Set<*> -> editor.putStringSet(key, value as Set<String>)
        }
    }
    editor.apply()

    // Wipe plaintext credentials so they no longer exist unencrypted
    oldPrefs.edit().clear().apply()
}
