package com.rexvit.rexprefs

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class RexPrefs private constructor(
    private val context: Context,
    val sharedPreferences: SharedPreferences,
    private val encryptedSharedPreferences: SharedPreferences?,
    private val prefName: String,
    private val enableEncryption: Boolean,
    private val enableInMemoryCache: Boolean
) {

    private val memoryCache = object : LinkedHashMap<String, Any?>(16, 0.75f, true) {
        private val MAX_SIZE = 50 // Adjust based on your needs

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any?>): Boolean {
            return size > MAX_SIZE
        }
    }

    private val gson = Gson()

    companion object {
        const val DEFAULT_PREFS_NAME = "rex_prefs_default"
        private val instances = mutableMapOf<String, RexPrefs>()

        @Synchronized
        fun getInstance(
            context: Context,
            prefName: String = DEFAULT_PREFS_NAME,
            enableEncryption: Boolean = true,
            enableInMemoryCache: Boolean = true,
            migrationFromPrefs: SharedPreferences? = null
        ): RexPrefs {
            return instances.getOrPut(prefName) {
                val (sharedPrefs, encryptedPrefs) = try {
                    if (enableEncryption) {
                        val masterKey = MasterKey.Builder(context)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()

                        val encryptedPrefs = EncryptedSharedPreferences.create(
                            context,
                            prefName,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )

                        Pair(encryptedPrefs, encryptedPrefs)
                    } else {
                        Pair(context.getSharedPreferences(prefName, Context.MODE_PRIVATE), null)
                    }
                } catch (e: Exception) {
                    // Fallback to regular SharedPreferences if encryption fails
                    Pair(context.getSharedPreferences(prefName, Context.MODE_PRIVATE), null)
                }

                val instance = RexPrefs(
                    context,
                    sharedPrefs,
                    encryptedPrefs,
                    prefName,
                    enableEncryption,
                    enableInMemoryCache
                )

                // Auto-migrate if old prefs provided
                migrationFromPrefs?.let { oldPrefs ->
                    instance.migrateFrom(oldPrefs)
                }

                instance
            }
        }
    }

    // Migration function
    fun migrateFrom(oldPrefs: SharedPreferences) {
        oldPrefs.all.forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                is Set<*> -> putStringSet(key, value as Set<String>)
            }
        }
        oldPrefs.edit().clear().apply()
    }

    // Clear all preferences
    fun clear() {
        sharedPreferences.edit().clear().apply()
        memoryCache.clear()
    }

    // Remove a specific key
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
        memoryCache.remove(key)
    }

    // Check if key exists
    fun contains(key: String): Boolean {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            true
        } else {
            sharedPreferences.contains(key)
        }
    }

    // Get all keys
    fun getAll(): Map<String, *> {
        return sharedPreferences.all
    }

    // Bulk operations
    fun putAll(values: Map<String, Any>) {
        val editor = sharedPreferences.edit()
        values.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(key, value as Set<String>)
                else -> throw IllegalArgumentException("Unsupported type: ${value.javaClass.name}")
            }
            if (enableInMemoryCache) memoryCache[key] = value
        }
        editor.apply()
    }

    // String operations
    fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
        if (enableInMemoryCache) memoryCache[key] = value
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            memoryCache[key] as? String ?: defaultValue
        } else {
            sharedPreferences.getString(key, defaultValue) ?: defaultValue
        }.also {
            if (enableInMemoryCache) memoryCache[key] = it
        }
    }

    // Int operations
    fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
        if (enableInMemoryCache) memoryCache[key] = value
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            (memoryCache[key] as? Int) ?: defaultValue
        } else {
            sharedPreferences.getInt(key, defaultValue)
        }.also {
            if (enableInMemoryCache) memoryCache[key] = it
        }
    }

    // Long operations
    fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
        if (enableInMemoryCache) memoryCache[key] = value
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            (memoryCache[key] as? Long) ?: defaultValue
        } else {
            sharedPreferences.getLong(key, defaultValue)
        }.also {
            if (enableInMemoryCache) memoryCache[key] = it
        }
    }

    // Float operations
    fun putFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
        if (enableInMemoryCache) memoryCache[key] = value
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            (memoryCache[key] as? Float) ?: defaultValue
        } else {
            sharedPreferences.getFloat(key, defaultValue)
        }.also {
            if (enableInMemoryCache) memoryCache[key] = it
        }
    }

    // Boolean operations
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
        if (enableInMemoryCache) memoryCache[key] = value
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            (memoryCache[key] as? Boolean) ?: defaultValue
        } else {
            sharedPreferences.getBoolean(key, defaultValue)
        }.also {
            if (enableInMemoryCache) memoryCache[key] = it
        }
    }

    // StringSet operations
    fun putStringSet(key: String, value: Set<String>) {
        sharedPreferences.edit().putStringSet(key, value).apply()
        if (enableInMemoryCache) memoryCache[key] = value
    }

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return if (enableInMemoryCache && memoryCache.containsKey(key)) {
            (memoryCache[key] as? Set<String>) ?: defaultValue
        } else {
            sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
        }.also {
            if (enableInMemoryCache) memoryCache[key] = it
        }
    }

    // Async operations
    fun putStringAsync(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            putString(key, value)
        }
    }

    fun putIntAsync(key: String, value: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            putInt(key, value)
        }
    }

    // GSON serialization (add GSON dependency)
    fun <T> putObject(key: String, value: T) {
        val json = Gson().toJson(value)
        putString(key, json)
    }

    inline fun <reified T> getObject(key: String): T? {
        val json = getString(key, "")
        return if (json.isNotEmpty()) {
            try {
                Gson().fromJson(json, T::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Kotlinx Serialization alternative
    @ExperimentalSerializationApi
    fun <T> putSerializable(key: String, value: T, serializer: KSerializer<T>) {
        val json = Json.encodeToString(serializer, value)
        putString(key, json)
    }

    @ExperimentalSerializationApi
    inline fun <reified T> getSerializable(key: String, serializer: KSerializer<T>): T? {
        val json = getString(key, "")
        return if (json.isNotEmpty()) {
            try {
                Json.decodeFromString(serializer, json)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }


    // Helper to observe preference changes
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun withLifecycle(lifecycle: Lifecycle): LifecycleAwareRexPrefs {
        return LifecycleAwareRexPrefs.from(this, lifecycle)
    }
}