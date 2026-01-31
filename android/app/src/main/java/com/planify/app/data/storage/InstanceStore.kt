package com.planify.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.planify.app.data.model.InstanceProfile
import com.planify.app.util.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InstanceStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("planify_instances", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val storageKey = "instances_v1"
    private val activeKey = "activeInstance_v1"

    private val _instances = MutableStateFlow<List<InstanceProfile>>(emptyList())
    val instances: StateFlow<List<InstanceProfile>> = _instances.asStateFlow()

    private val _activeInstanceId = MutableStateFlow<String?>(null)
    val activeInstanceId: StateFlow<String?> = _activeInstanceId.asStateFlow()

    init {
        load()
    }

    fun activeInstance(): InstanceProfile? {
        val id = _activeInstanceId.value ?: return null
        return _instances.value.firstOrNull { it.id == id }
    }

    fun setInstances(instances: List<InstanceProfile>) {
        _instances.value = instances
        if (activeInstance() == null) {
            _activeInstanceId.value = instances.firstOrNull()?.id
        }
        persist()
    }

    fun addInstance(instance: InstanceProfile) {
        _instances.value = _instances.value + instance
        _activeInstanceId.value = instance.id
        persist()
    }

    fun removeInstance(instanceId: String) {
        _instances.value = _instances.value.filterNot { it.id == instanceId }
        if (_activeInstanceId.value == instanceId) {
            _activeInstanceId.value = _instances.value.firstOrNull()?.id
        }
        persist()
    }

    fun setActiveInstance(instanceId: String) {
        if (_instances.value.any { it.id == instanceId }) {
            _activeInstanceId.value = instanceId
            persist()
        }
    }

    private fun load() {
        val data = prefs.getString(storageKey, null)
        if (!data.isNullOrBlank()) {
            try {
                val decoded = json.decodeFromString<List<InstanceProfile>>(data)
                _instances.value = decoded
            } catch (e: Exception) {
                DebugLogger.error("InstanceStore: failed to decode instances", e)
            }
        }

        val active = prefs.getString(activeKey, null)
        if (!active.isNullOrBlank() && _instances.value.any { it.id == active }) {
            _activeInstanceId.value = active
        } else {
            _activeInstanceId.value = _instances.value.firstOrNull()?.id
        }
    }

    private fun persist() {
        try {
            val data = json.encodeToString(_instances.value)
            prefs.edit().putString(storageKey, data).apply()
        } catch (e: Exception) {
            DebugLogger.error("InstanceStore: failed to encode instances", e)
        }

        val active = _activeInstanceId.value
        if (active == null) {
            prefs.edit().remove(activeKey).apply()
        } else {
            prefs.edit().putString(activeKey, active).apply()
        }
    }
}
