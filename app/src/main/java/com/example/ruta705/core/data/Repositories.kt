package com.example.ruta705.core.data

import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppMeta
import com.example.ruta705.core.network.AuthSession
import com.example.ruta705.core.network.ChangePasswordRequest
import com.example.ruta705.core.network.CreateReportRequest
import com.example.ruta705.core.network.CreateReportResponse
import com.example.ruta705.core.network.FavoriteRoute
import com.example.ruta705.core.network.FlagRequest
import com.example.ruta705.core.network.ForgotRequest
import com.example.ruta705.core.network.GeoPlace
import com.example.ruta705.core.network.GoogleRequest
import com.example.ruta705.core.network.LatLngDto
import com.example.ruta705.core.network.LoginRequest
import com.example.ruta705.core.network.NamedPlace
import com.example.ruta705.core.network.NotificationPrefs
import com.example.ruta705.core.network.NotifyRequest
import com.example.ruta705.core.network.PlaceDto
import com.example.ruta705.core.network.PlanRequest
import com.example.ruta705.core.network.PlanResult
import com.example.ruta705.core.network.RefreshRequest
import com.example.ruta705.core.network.RegisterRequest
import com.example.ruta705.core.network.Report
import com.example.ruta705.core.network.ResetRequest
import com.example.ruta705.core.network.RouteDetail
import com.example.ruta705.core.network.RouteSummary
import com.example.ruta705.core.network.Ruta705Api
import com.example.ruta705.core.network.SaveTripRequest
import com.example.ruta705.core.network.SavedTrip
import com.example.ruta705.core.network.StopItem
import com.example.ruta705.core.network.UpdateProfileRequest
import com.example.ruta705.core.network.User
import com.example.ruta705.core.network.VoteRequest
import com.example.ruta705.core.network.apiCall
import com.example.ruta705.core.network.apiCallUnit
import com.example.ruta705.core.network.onSuccess
import com.example.ruta705.core.session.SessionStore
import com.example.ruta705.core.session.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MetaRepository(private val api: Ruta705Api) {
    private val _meta = MutableStateFlow<AppMeta?>(null)
    val meta: StateFlow<AppMeta?> = _meta.asStateFlow()
    private val mutex = Mutex()

    suspend fun load(force: Boolean = false): ApiResult<AppMeta> = mutex.withLock {
        val cached = _meta.value
        if (cached != null && !force) return ApiResult.Success(cached)
        apiCall { api.meta().data }.onSuccess { _meta.value = it }
    }
}

class AuthRepository(
    private val api: Ruta705Api,
    private val session: SessionStore,
    private val settings: SettingsStore,
) {
    private suspend fun persist(result: ApiResult<AuthSession>) = result.onSuccess {
        session.save(it)
        settings.setOnboardingDone()
    }

    suspend fun login(identifier: String, password: String) =
        persist(apiCall { api.login(LoginRequest(identifier.trim(), password)).data })

    suspend fun register(fullName: String, email: String, phone: String?, password: String) =
        persist(apiCall { api.register(RegisterRequest(fullName.trim(), email.trim(), phone?.trim()?.ifBlank { null }, password)).data })

    suspend fun loginWithGoogle(idToken: String) = persist(apiCall { api.google(GoogleRequest(idToken)).data })

    suspend fun forgotPassword(email: String) = apiCall { api.forgotPassword(ForgotRequest(email.trim())).data.message }

    suspend fun resetPassword(email: String, code: String, newPassword: String) =
        apiCallUnit { api.resetPassword(ResetRequest(email.trim(), code.trim(), newPassword)) }

    suspend fun refreshProfile(): ApiResult<User> = apiCall { api.me().data }.onSuccess { session.updateUser(it) }

    suspend fun updateProfile(fullName: String, phone: String?) =
        apiCall { api.updateProfile(UpdateProfileRequest(fullName.trim(), phone?.trim() ?: "")).data }.onSuccess { session.updateUser(it) }

    suspend fun changePassword(current: String?, new: String) =
        persist(apiCall { api.changePassword(ChangePasswordRequest(current?.ifBlank { null }, new)).data })

    suspend fun continueAsGuest() = settings.setOnboardingDone()

    suspend fun logout() {
        session.session?.tokens?.refreshToken?.let { token -> apiCallUnit { api.logout(RefreshRequest(token)) } }
        session.clear()
        settings.clearUserData()
    }

    suspend fun deleteAccount(): ApiResult<Unit> = apiCallUnit { api.deleteAccount() }.onSuccess {
        session.clear()
        settings.clearUserData()
    }
}

class TransitRepository(private val api: Ruta705Api) {
    private val _routes = MutableStateFlow<List<RouteSummary>?>(null)
    val routes: StateFlow<List<RouteSummary>?> = _routes.asStateFlow()
    private val routesMutex = Mutex()

    /** Lista de rutas con caché en memoria; `force` la vuelve a pedir (pull-to-refresh). */
    suspend fun loadRoutes(force: Boolean = false): ApiResult<List<RouteSummary>> = routesMutex.withLock {
        val cached = _routes.value
        if (cached != null && !force) return ApiResult.Success(cached)
        apiCall { api.routes().data }.onSuccess { _routes.value = it }
    }

    fun markFavorite(routeId: String, favorite: Boolean) {
        _routes.update { list -> list?.map { if (it.id == routeId) it.copy(isFavorite = favorite) else it } }
    }

    fun clearCache() {
        _routes.value = null
    }

    suspend fun route(id: String): ApiResult<RouteDetail> = apiCall { api.route(id).data }

    suspend fun searchStops(query: String) = apiCall { api.stops(query = query, limit = 15).data }

    suspend fun nearbyStops(lat: Double, lng: Double, radius: Int = 800): ApiResult<List<StopItem>> =
        apiCall { api.stops(lat = lat, lng = lng, radius = radius, limit = 10).data }

    suspend fun plan(origin: NamedPlace, destination: NamedPlace, maxWalkMeters: Int): ApiResult<PlanResult> = apiCall {
        api.plan(
            PlanRequest(
                origin = PlaceDto(origin.lat, origin.lng, origin.label),
                destination = PlaceDto(destination.lat, destination.lng, destination.label),
                maxWalkMeters = maxWalkMeters,
            )
        ).data
    }

    suspend fun geocode(query: String): ApiResult<List<GeoPlace>> = apiCall { api.geocode(query.trim()).data }

    suspend fun reverseGeocode(lat: Double, lng: Double): ApiResult<GeoPlace?> = apiCall { api.reverseGeocode(lat, lng).data }

    suspend fun announcements() = apiCall { api.announcements().data }
}

class ReportsRepository(private val api: Ruta705Api) {
    suspend fun recent(routeIds: List<String>? = null, limit: Int = 10, cursor: String? = null) =
        apiCall { api.reports(routeIds = routeIds?.takeIf { it.isNotEmpty() }?.joinToString(","), cursor = cursor, limit = limit) }

    suspend fun create(
        routeId: String,
        type: String,
        direction: Int?,
        description: String?,
        stopId: String?,
        location: LatLngDto?,
    ): ApiResult<CreateReportResponse> = apiCall {
        api.createReport(CreateReportRequest(routeId, type, direction, description?.trim()?.ifBlank { null }, stopId, location))
    }

    suspend fun vote(reportId: String, confirm: Boolean): ApiResult<Report> =
        apiCall { api.vote(reportId, VoteRequest(if (confirm) "confirma" else "desmiente")).data }

    suspend fun flag(reportId: String, reason: String) = apiCallUnit { api.flag(reportId, FlagRequest(reason)) }

    suspend fun delete(reportId: String) = apiCallUnit { api.deleteReport(reportId) }
}

class FavoritesRepository(private val api: Ruta705Api, private val transit: TransitRepository) {
    private val _routeIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteRouteIds: StateFlow<Set<String>> = _routeIds.asStateFlow()

    suspend fun routes(): ApiResult<List<FavoriteRoute>> =
        apiCall { api.favoriteRoutes().data }.onSuccess { list -> _routeIds.value = list.map { it.routeId }.toSet() }

    suspend fun setFavorite(routeId: String, favorite: Boolean): ApiResult<Unit> {
        val result = if (favorite) apiCallUnit { api.addFavoriteRoute(routeId) } else apiCallUnit { api.removeFavoriteRoute(routeId) }
        return result.onSuccess {
            _routeIds.update { if (favorite) it + routeId else it - routeId }
            transit.markFavorite(routeId, favorite)
        }
    }

    suspend fun setNotify(routeId: String, notify: Boolean) = apiCallUnit { api.setFavoriteNotify(routeId, NotifyRequest(notify)) }

    suspend fun trips(): ApiResult<List<SavedTrip>> = apiCall { api.trips().data }

    suspend fun saveTrip(name: String, origin: NamedPlace, destination: NamedPlace) =
        apiCall { api.saveTrip(SaveTripRequest(name.trim(), origin, destination)).data }

    suspend fun deleteTrip(id: String) = apiCallUnit { api.deleteTrip(id) }

    fun clear() {
        _routeIds.value = emptySet()
    }
}

class NotificationsRepository(private val api: Ruta705Api) {
    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    suspend fun refreshUnread() = apiCall { api.unreadCount().data.unread }.onSuccess { _unread.value = it }

    suspend fun list(cursor: String? = null) = apiCall { api.notifications(cursor = cursor, limit = 20) }

    suspend fun markRead(id: String) = apiCallUnit { api.markRead(id) }.onSuccess { _unread.update { (it - 1).coerceAtLeast(0) } }

    suspend fun markAllRead() = apiCallUnit { api.markAllRead() }.onSuccess { _unread.value = 0 }

    suspend fun prefs() = apiCall { api.notificationPrefs().data }

    suspend fun updatePrefs(prefs: NotificationPrefs) = apiCall { api.updateNotificationPrefs(prefs).data }

    fun clear() {
        _unread.value = 0
    }
}
