package com.example.ruta705.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.network.Announcement
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppError
import com.example.ruta705.core.network.Report
import com.example.ruta705.core.network.RouteSummary
import com.example.ruta705.core.network.SavedTrip
import com.example.ruta705.core.network.getOrNull
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.ui.common.Loadable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class HomeData(
    val favoriteRoutes: List<RouteSummary>,
    val trips: List<SavedTrip>,
    val reports: List<Report>,
    val reportsForFavorites: Boolean,
    val announcements: List<Announcement>,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf<Loadable<HomeData>>(Loadable.Loading)
        private set
    val meta = container.meta.meta
    val unread = container.notifications.unread
    private var loadedFor: Boolean? = null
    private var resumes = 0

    /** Al volver a la pantalla se actualizan reportes y favoritos sin mostrar el esqueleto. */
    fun refreshOnResume() {
        if (resumes++ > 0 && state is Loadable.Ready) refresh(force = true)
    }

    fun ensureLoaded(loggedIn: Boolean) {
        if (loadedFor == loggedIn && state !is Loadable.Failed) return
        loadedFor = loggedIn
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val current = state
            state = if (current is Loadable.Ready) current.copy(refreshing = true) else Loadable.Loading
            val loggedIn = container.sessionStore.state.value is SessionState.Authenticated
            container.meta.load(force)

            val routesCall = async { container.transit.loadRoutes(force) }
            val announcementsCall = async { container.transit.announcements() }
            val favoritesCall = async { if (loggedIn) container.favorites.routes() else null }
            val tripsCall = async { if (loggedIn) container.favorites.trips() else null }
            if (loggedIn) launch { container.notifications.refreshUnread() }

            val routes = when (val result = routesCall.await()) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> {
                    state = if (current is Loadable.Ready) current.copy(refreshing = false) else Loadable.Failed(result.error)
                    return@launch
                }
            }
            val favoriteIds = favoritesCall.await()?.getOrNull()?.map { it.routeId }.orEmpty()
            val favoriteRoutes = routes.filter { it.id in favoriteIds }
            val reports = container.reports.recent(routeIds = favoriteIds.ifEmpty { null }, limit = 6).getOrNull()?.data.orEmpty()
            state = Loadable.Ready(
                HomeData(
                    favoriteRoutes = favoriteRoutes,
                    trips = tripsCall.await()?.getOrNull().orEmpty(),
                    reports = reports,
                    reportsForFavorites = favoriteIds.isNotEmpty(),
                    announcements = announcementsCall.await().getOrNull().orEmpty(),
                )
            )
        }
    }

    fun vote(report: Report, confirm: Boolean, onError: (AppError) -> Unit) {
        viewModelScope.launch {
            when (val result = container.reports.vote(report.id, confirm)) {
                is ApiResult.Success -> replaceReport(result.value)
                is ApiResult.Failure -> onError(result.error)
            }
        }
    }

    private fun replaceReport(updated: Report) {
        val current = state as? Loadable.Ready ?: return
        state = current.copy(value = current.value.copy(reports = current.value.reports.map { if (it.id == updated.id) updated else it }))
    }
}
