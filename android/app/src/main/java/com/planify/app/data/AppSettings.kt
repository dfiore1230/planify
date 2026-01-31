package com.planify.app.data

import com.planify.app.data.storage.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone

class AppSettings(private val store: SettingsStore) {
    private val timeZoneKey = "app_settings.time_zone_identifier"

    private val _timeZoneIdentifier = MutableStateFlow(
        store.getString(timeZoneKey, TimeZone.getDefault().id)
    )
    val timeZoneIdentifier: StateFlow<String> = _timeZoneIdentifier.asStateFlow()

    var timeZoneId: String
        get() = _timeZoneIdentifier.value
        set(value) {
            _timeZoneIdentifier.value = value
            store.putString(timeZoneKey, value)
        }

    fun resetTimeZoneToCurrent() {
        timeZoneId = TimeZone.getDefault().id
    }

    fun timeZone(): TimeZone = TimeZone.getTimeZone(timeZoneId)
}
