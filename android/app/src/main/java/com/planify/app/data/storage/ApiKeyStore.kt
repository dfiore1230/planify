package com.planify.app.data.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.planify.app.data.model.InstanceProfile
import com.planify.app.util.DebugLogger

class ApiKeyStore(context: Context) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "planify_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(apiKey: String, instance: InstanceProfile) {
        prefs.edit().putString(storageKey(instance), apiKey).apply()
    }

    fun load(instance: InstanceProfile): String? {
        return prefs.getString(storageKey(instance), null)
    }

    fun clear(instance: InstanceProfile) {
        prefs.edit().remove(storageKey(instance)).apply()
    }

    private fun storageKey(instance: InstanceProfile): String = instance.baseUrl
}
