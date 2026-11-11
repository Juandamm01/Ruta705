package com.example.ruta705.core

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.ruta705.core.data.AuthRepository
import com.example.ruta705.core.data.FavoritesRepository
import com.example.ruta705.core.data.MetaRepository
import com.example.ruta705.core.data.NotificationsRepository
import com.example.ruta705.core.data.ReportsRepository
import com.example.ruta705.core.data.TransitRepository
import com.example.ruta705.core.location.LocationProvider
import com.example.ruta705.core.network.ApiClient
import com.example.ruta705.core.session.SessionStore
import com.example.ruta705.core.session.SettingsStore
import kotlinx.serialization.json.Json

/** Contenedor de dependencias de la app (inyección manual, sin frameworks adicionales). */
class AppContainer(context: Context) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    val sessionStore = SessionStore(context, json)
    val settings = SettingsStore(context, json)
    val apiClient = ApiClient(sessionStore, json)
    private val api = apiClient.api

    val meta = MetaRepository(api)
    val auth = AuthRepository(api, sessionStore, settings)
    val transit = TransitRepository(api)
    val reports = ReportsRepository(api)
    val favorites = FavoritesRepository(api, transit)
    val notifications = NotificationsRepository(api)
    val location = LocationProvider(context)

    /** Limpia cachés ligadas a la cuenta al cerrar sesión. */
    fun onSignedOut() {
        favorites.clear()
        notifications.clear()
        transit.clearCache()
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer no disponible") }
