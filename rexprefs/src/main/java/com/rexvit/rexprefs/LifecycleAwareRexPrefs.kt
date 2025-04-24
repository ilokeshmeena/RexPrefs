package com.rexvit.rexprefs

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleAwareRexPrefs(
    private val rexPrefs: RexPrefs,
    private val lifecycle: Lifecycle
) : LifecycleEventObserver {

    private val pendingWrites = mutableMapOf<String, Any>()
    private var isStarted = false

    init {
        lifecycle.addObserver(this)
    }

    fun putString(key: String, value: String) {
        writeOrQueue(key, value)
    }

    fun putInt(key: String, value: Int) {
        writeOrQueue(key, value)
    }

    fun putLong(key: String, value: Long) {
        writeOrQueue(key, value)
    }

    fun putFloat(key: String, value: Float) {
        writeOrQueue(key, value)
    }

    fun putBoolean(key: String, value: Boolean) {
        writeOrQueue(key, value)
    }

    fun putStringSet(key: String, value: Set<String>) {
        writeOrQueue(key, value)
    }

    private fun writeOrQueue(key: String, value: Any) {
        if (isStarted) {
            when (value) {
                is String -> rexPrefs.putString(key, value)
                is Int -> rexPrefs.putInt(key, value)
                is Long -> rexPrefs.putLong(key, value)
                is Float -> rexPrefs.putFloat(key, value)
                is Boolean -> rexPrefs.putBoolean(key, value)
                is Set<*> -> rexPrefs.putStringSet(key, value as Set<String>)
                else -> throw IllegalArgumentException("Unsupported type: ${value.javaClass.name}")
            }
        } else {
            pendingWrites[key] = value
        }
    }

    private fun flushPendingWrites() {
        if (pendingWrites.isNotEmpty()) {
            rexPrefs.putAll(pendingWrites)
            pendingWrites.clear()
        }
    }

    fun clearPendingWrites() {
        pendingWrites.clear()
    }

    companion object {
        fun from(rexPrefs: RexPrefs, lifecycle: Lifecycle): LifecycleAwareRexPrefs {
            return LifecycleAwareRexPrefs(rexPrefs, lifecycle)
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                isStarted = true
                flushPendingWrites()
            }
            Lifecycle.Event.ON_STOP -> {
                isStarted = false
            }
            else -> {}
        }
    }
}