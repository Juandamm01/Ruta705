package com.example.ruta705.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppNotification
import com.example.ruta705.core.network.FavoriteRoute
import com.example.ruta705.core.network.NotificationPrefs
import com.example.ruta705.core.network.SavedTrip
import com.example.ruta705.feature.auth.normalizePhone
import com.example.ruta705.feature.auth.passwordProblems
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.toLoadable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    val maxWalk = container.settings.maxWalkMeters
    val meta = container.meta.meta

    fun refreshProfile() {
        if (container.sessionStore.session == null) return
        viewModelScope.launch { container.auth.refreshProfile() }
    }

    fun setMaxWalk(meters: Int) = viewModelScope.launch { container.settings.setMaxWalkMeters(meters) }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            busy = true
            container.auth.logout()
            busy = false
            onDone()
        }
    }

    fun deleteAccount(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            busy = true
            when (val result = container.auth.deleteAccount()) {
                is ApiResult.Success -> onResult(null)
                is ApiResult.Failure -> onResult(result.error.message)
            }
            busy = false
        }
    }
}

data class FavoritesData(val routes: List<FavoriteRoute>, val trips: List<SavedTrip>)

class FavoritesViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf<Loadable<FavoritesData>>(Loadable.Loading)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state = Loadable.Loading
            val routes = async { container.favorites.routes() }
            val trips = async { container.favorites.trips() }
            val r = routes.await()
            val t = trips.await()
            state = when {
                r is ApiResult.Failure -> Loadable.Failed(r.error)
                t is ApiResult.Failure -> Loadable.Failed(t.error)
                else -> Loadable.Ready(FavoritesData((r as ApiResult.Success).value, (t as ApiResult.Success).value))
            }
        }
    }

    private fun update(transform: (FavoritesData) -> FavoritesData) {
        val current = state as? Loadable.Ready ?: return
        state = current.copy(value = transform(current.value))
    }

    fun setNotify(route: FavoriteRoute, notify: Boolean, onError: (String) -> Unit) {
        update { data -> data.copy(routes = data.routes.map { if (it.routeId == route.routeId) it.copy(notify = notify) else it }) }
        viewModelScope.launch {
            val result = container.favorites.setNotify(route.routeId, notify)
            if (result is ApiResult.Failure) {
                update { data -> data.copy(routes = data.routes.map { if (it.routeId == route.routeId) it.copy(notify = !notify) else it }) }
                onError(result.error.message)
            }
        }
    }

    fun removeRoute(route: FavoriteRoute, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = container.favorites.setFavorite(route.routeId, false)) {
                is ApiResult.Success -> update { data -> data.copy(routes = data.routes.filterNot { it.routeId == route.routeId }) }
                is ApiResult.Failure -> onError(result.error.message)
            }
        }
    }

    fun removeTrip(trip: SavedTrip, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = container.favorites.deleteTrip(trip.id)) {
                is ApiResult.Success -> update { data -> data.copy(trips = data.trips.filterNot { it.id == trip.id }) }
                is ApiResult.Failure -> onError(result.error.message)
            }
        }
    }
}

class NotificationsViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf<Loadable<List<AppNotification>>>(Loadable.Loading)
        private set
    var nextCursor by mutableStateOf<String?>(null)
        private set
    var loadingMore by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state = Loadable.Loading
            when (val result = container.notifications.list()) {
                is ApiResult.Success -> {
                    state = Loadable.Ready(result.value.data)
                    nextCursor = result.value.meta.nextCursor
                    result.value.data.maxOfOrNull { it.id }?.let { container.settings.setLastNotifiedId(it) }
                }
                is ApiResult.Failure -> state = Loadable.Failed(result.error)
            }
            container.notifications.refreshUnread()
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore) return
        viewModelScope.launch {
            loadingMore = true
            val result = container.notifications.list(cursor)
            if (result is ApiResult.Success) {
                val current = (state as? Loadable.Ready)?.value.orEmpty()
                state = Loadable.Ready(current + result.value.data)
                nextCursor = result.value.meta.nextCursor
            }
            loadingMore = false
        }
    }

    fun open(item: AppNotification) {
        if (item.read) return
        markLocal(item.id)
        viewModelScope.launch { container.notifications.markRead(item.id) }
    }

    fun markAll() {
        val current = (state as? Loadable.Ready)?.value ?: return
        state = Loadable.Ready(current.map { it.copy(read = true) })
        viewModelScope.launch { container.notifications.markAllRead() }
    }

    private fun markLocal(id: String) {
        val current = (state as? Loadable.Ready)?.value ?: return
        state = Loadable.Ready(current.map { if (it.id == id) it.copy(read = true) else it })
    }
}

class NotificationSettingsViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf<Loadable<NotificationPrefs>>(Loadable.Loading)
        private set
    var saving by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            state = Loadable.Loading
            state = container.notifications.prefs().toLoadable()
        }
    }

    fun save(prefs: NotificationPrefs, onError: (String) -> Unit) {
        val previous = state
        state = Loadable.Ready(prefs)
        viewModelScope.launch {
            saving = true
            when (val result = container.notifications.updatePrefs(prefs)) {
                is ApiResult.Success -> {
                    state = Loadable.Ready(result.value)
                    container.settings.setQuietHours(result.value.quietStart, result.value.quietEnd)
                }
                is ApiResult.Failure -> {
                    state = previous
                    onError(result.error.message)
                }
            }
            saving = false
        }
    }
}

class AccountFormViewModel(private val container: AppContainer) : ViewModel() {
    private val user = container.sessionStore.session?.user
    var fullName by mutableStateOf(user?.fullName.orEmpty())
    var phone by mutableStateOf(user?.phone.orEmpty())
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    val hasPassword: Boolean = user?.hasPassword ?: true
    val email: String = user?.email.orEmpty()

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    fun saveProfile(onDone: () -> Unit) {
        val normalized = normalizePhone(phone)
        val errors = buildMap {
            if (fullName.trim().length < 2) put("fullName", "Escribe tu nombre")
            if (phone.isNotBlank() && !Regex("^3\\d{9}$").matches(normalized)) put("phone", "Celular colombiano de 10 dígitos (inicia en 3)")
        }
        fieldErrors = errors
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            loading = true
            error = null
            when (val result = container.auth.updateProfile(fullName, normalized.ifBlank { null })) {
                is ApiResult.Success -> onDone()
                is ApiResult.Failure -> {
                    error = result.error.message
                    fieldErrors = result.error.fieldErrors
                }
            }
            loading = false
        }
    }

    fun changePassword(onDone: () -> Unit) {
        val errors = buildMap {
            if (hasPassword && currentPassword.isBlank()) put("currentPassword", "Escribe tu contraseña actual")
            passwordProblems(newPassword).takeIf { it.isNotEmpty() }?.let { put("newPassword", it.joinToString(" · ")) }
        }
        fieldErrors = errors
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            loading = true
            error = null
            when (val result = container.auth.changePassword(currentPassword, newPassword)) {
                is ApiResult.Success -> onDone()
                is ApiResult.Failure -> {
                    error = result.error.message
                    fieldErrors = result.error.fieldErrors
                }
            }
            loading = false
        }
    }
}
