package com.example.bdrowclient.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyPreferences(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    fun getApiKey(): String? {
        return encryptedPrefs.getString(KEY_API_KEY, null)
    }
    
    fun hasApiKey(): Boolean {
        return getApiKey() != null
    }
    
    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
    }
    
    fun isUsingApiKey(): Boolean {
        return encryptedPrefs.getBoolean(KEY_USE_API_KEY, false)
    }
    
    fun setUseApiKey(use: Boolean) {
        encryptedPrefs.edit().putBoolean(KEY_USE_API_KEY, use).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "bdrow_api_key_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_USE_API_KEY = "use_api_key"
    }
}
