package com.example.ruta705.feature.routes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppError
import com.example.ruta705.core.network.Report
import com.example.ruta705.core.network.RouteDetail
import com.example.ruta705.core.network.RouteSummary
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.toLoadable
import kotlinx.coroutines.launch
import java.text.Normalizer

private fun normalize(value: String) =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

class RoutesViewModel(private val container: AppContainer) : ViewModel() {
    var state by mutableStateOf<Loadable<List<RouteSummary>>>(Loadable.Loading)
        private set
    var query by mutableStateOf("")
    var onlyFavorites by mutableStateOf(false)
    val meta = container.meta.meta

    init {
        viewModelScope.launch { container.transit.routes.collect { list -> if (list != null) state = Loadable.Ready(list) } }
        load(force = false)
    }

    fun load(force: Boolean = true) {
        viewModelScope.launch {
            val current = state
            state = if (current is Loadable.Ready) current.copy(refreshing = true) else Loadable.Loading
            state = when (val result = container.transit.loadRoutes(force)) {
                is ApiResult.Success -> Loadable.Ready(result.value)
                is ApiResult.Failure -> if (current is Loadable.Ready) current.copy(refreshing = false) else Loadable.Failed(result.error)
            }
        }
    }

    fun filtered(routes: List<RouteSummary>): List<RouteSummary> {
        val term = normalize(query.trim())
        return routes.filter { route ->
            (!onlyFavorites || route.isFavorite) &&
                (term.isEmpty() || normalize("${route.code} ${route.name} ${route.from.orEmpty()} ${route.to.orEmpty()} ${route.operatorName.orEmpty()}").contains(term))
        }
    }

    fun toggleFavorite(route: RouteSummary, onError: (AppError) -> Unit) {
        viewModelScope.launch {
            container.favorites.setFavorite(route.id, !route.isFavorite).let { if (it is ApiResult.Failure) onError(it.error) }
        }
    }
}

class RouteDetailViewModel(private val container: AppContainer, private val routeId: String) : ViewModel() {
    var state by mutableStateOf<Loadable<RouteDetail>>(Loadable.Loading)
        private set
    var direction by mutableIntStateOf(0)
    var busy by mutableStateOf(false)
        private set

    private var resumes = 0

    init {
        load()
    }

    fun refreshOnResume() {
        if (resumes++ > 0 && state is Loadable.Ready) load()
    }

    fun load() {
        viewModelScope.launch {
            val current = state
            state = if (current is Loadable.Ready) current.copy(refreshing = true) else Loadable.Loading
            val result = container.transit.route(routeId).toLoadable()
            state = if (result is Loadable.Failed && current is Loadable.Ready) current.copy(refreshing = false) else result
            (state as? Loadable.Ready)?.value?.directions?.firstOrNull()?.let { first ->
                if ((state as Loadable.Ready).value.directions.none { it.direction == direction }) direction = first.direction
            }
        }
    }

    private fun update(transform: (RouteDetail) -> RouteDetail) {
        val current = state as? Loadable.Ready ?: return
        state = current.copy(value = transform(current.value))
    }

    fun toggleFavorite(onResult: (String?) -> Unit) {
        val route = (state as? Loadable.Ready)?.value ?: return
        if (busy) return
        viewModelScope.launch {
            busy = true
            when (val result = container.favorites.setFavorite(route.id, !route.isFavorite)) {
                is ApiResult.Success -> {
                    update { it.copy(isFavorite = !route.isFavorite) }
                    onResult(if (!route.isFavorite) "Ruta guardada. Te avisaremos de reportes importantes." else "Ruta quitada de tus favoritas.")
                }
                is ApiResult.Failure -> onResult(result.error.message)
            }
            busy = false
        }
    }

    fun vote(report: Report, confirm: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = container.reports.vote(report.id, confirm)) {
                is ApiResult.Success -> {
                    update { detail -> detail.copy(reports = detail.reports.map { if (it.id == report.id) result.value else it }) }
                    load() // el estado de la ruta depende de las confirmaciones
                }
                is ApiResult.Failure -> onError(result.error.message)
            }
        }
    }

    fun flag(report: Report, reason: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = container.reports.flag(report.id, reason)) {
                is ApiResult.Success -> onDone("Gracias. Revisaremos este reporte.")
                is ApiResult.Failure -> onDone(result.error.message)
            }
        }
    }

    fun delete(report: Report, onDone: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = container.reports.delete(report.id)) {
                is ApiResult.Success -> {
                    update { detail -> detail.copy(reports = detail.reports.filterNot { it.id == report.id }) }
                    onDone("Reporte eliminado.")
                }
                is ApiResult.Failure -> onDone(result.error.message)
            }
        }
    }
}
