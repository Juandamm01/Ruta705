package com.example.ruta705.core.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ruta705.core.network.NamedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Preferencias locales del dispositivo (no sensibles). */
class SettingsStore(private val context: Context, private val json: Json) {
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val lastNotifiedId = stringPreferencesKey("last_notified_id")
    private val maxWalk = intPreferencesKey("max_walk_meters")
    private val recentPlaces = stringPreferencesKey("recent_places")
    private val quietStart = stringPreferencesKey("quiet_start")
    private val quietEnd = stringPreferencesKey("quiet_end")

    private val placesSerializer = ListSerializer(NamedPlace.serializer())

    suspend fun isOnboardingDone() = context.settingsDataStore.data.first()[onboardingDone] ?: false

    suspend fun setOnboardingDone() {
        context.settingsDataStore.edit { it[onboardingDone] = true }
    }

    val maxWalkMeters: Flow<Int> = context.settingsDataStore.data.map { it[maxWalk] ?: 600 }

    suspend fun setMaxWalkMeters(meters: Int) {
        context.settingsDataStore.edit { it[maxWalk] = meters }
    }

    val recent: Flow<List<NamedPlace>> = context.settingsDataStore.data.map { prefs ->
        prefs[recentPlaces]?.let { runCatching { json.decodeFromString(placesSerializer, it) }.getOrNull() }.orEmpty()
    }

    suspend fun addRecent(place: NamedPlace) {
        context.settingsDataStore.edit { prefs ->
            val existing = prefs[recentPlaces]?.let { runCatching { json.decodeFromString(placesSerializer, it) }.getOrNull() }.orEmpty()
            val updated = (listOf(place) + existing.filterNot { it.label == place.label }).take(6)
            prefs[recentPlaces] = json.encodeToString(placesSerializer, updated)
        }
    }

    suspend fun lastNotifiedId(): String? = context.settingsDataStore.data.first()[lastNotifiedId]

    suspend fun setLastNotifiedId(id: String) {
        context.settingsDataStore.edit { it[lastNotifiedId] = id }
    }

    suspend fun quietHours(): Pair<String, String>? {
        val prefs = context.settingsDataStore.data.first()
        val start = prefs[quietStart] ?: return null
        val end = prefs[quietEnd] ?: return null
        return start to end
    }

    suspend fun setQuietHours(start: String?, end: String?) {
        context.settingsDataStore.edit {
            if (start != null && end != null) {
                it[quietStart] = start
                it[quietEnd] = end
            } else {
                it.remove(quietStart)
                it.remove(quietEnd)
            }
        }
    }

    suspend fun clearUserData() {
        context.settingsDataStore.edit {
            it.remove(lastNotifiedId)
            it.remove(quietStart)
            it.remove(quietEnd)
        }
    }
}
