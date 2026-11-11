package com.example.ruta705.feature.reports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.location.LocationProvider
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.LatLngDto
import com.example.ruta705.core.network.ReportTypeInfo
import com.example.ruta705.core.network.RouteDetail
import com.example.ruta705.core.network.RouteSummary
import com.example.ruta705.core.network.getOrNull
import com.example.ruta705.ui.common.Loadable
import kotlinx.coroutines.launch

/** Tipos por defecto si /meta aún no cargó (coinciden con el backend). */
private val FALLBACK_TYPES = listOf(
    ReportTypeInfo("vi_pasar", "Vi pasar esta ruta", "Una buseta de esta ruta acaba de pasar por donde estás.", 20),
    ReportTypeInfo("retraso", "Esta ruta presenta retrasos", "Llevas mucho más tiempo esperando de lo habitual.", 45),
    ReportTypeInfo("no_pasa", "Esta ruta no está pasando", "No ha pasado ninguna buseta de esta ruta en un tiempo inusual.", 90),
    ReportTypeInfo("desvio", "Hay un desvío", "La ruta está tomando un recorrido distinto al habitual.", 240),
    ReportTypeInfo("otro", "Otra novedad del servicio", "Cualquier otra situación útil para quienes usan esta ruta.", 120),
)

class ReportViewModel(private val container: AppContainer, initialRouteId: String?) : ViewModel() {
    var routes by mutableStateOf<Loadable<List<RouteSummary>>>(Loadable.Loading)
        private set
    var selectedRoute by mutableStateOf<RouteSummary?>(null)
        private set
    var selectedDetail by mutableStateOf<RouteDetail?>(null)
        private set
    var type by mutableStateOf<String?>(null)
    var direction by mutableStateOf<Int?>(null)
    var stopId by mutableStateOf<String?>(null)
    var description by mutableStateOf("")
    var shareLocation by mutableStateOf(false)
        private set
    var locationStatus by mutableStateOf<String?>(null)
        private set
    private var location: LatLngDto? = null

    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var success by mutableStateOf<String?>(null)
        private set

    val types: List<ReportTypeInfo> get() = container.meta.meta.value?.reportTypes?.takeIf { it.isNotEmpty() } ?: FALLBACK_TYPES

    val canSubmit: Boolean get() = selectedRoute != null && type != null && !submitting

    private var pendingRouteId = initialRouteId

    init {
        loadRoutes()
    }

    fun loadRoutes() {
        viewModelScope.launch {
            routes = Loadable.Loading
            routes = when (val result = container.transit.loadRoutes()) {
                is ApiResult.Success -> Loadable.Ready(result.value)
                is ApiResult.Failure -> Loadable.Failed(result.error)
            }
            pendingRouteId?.let { selectRoute(it) }
            pendingRouteId = null
        }
    }

    fun selectRoute(id: String) {
        val route = (routes as? Loadable.Ready)?.value?.firstOrNull { it.id == id } ?: return
        selectedRoute = route
        selectedDetail = null
        direction = null
        stopId = null
        viewModelScope.launch { selectedDetail = container.transit.route(id).getOrNull() }
    }

    fun enableLocation(requestPermission: () -> Unit) {
        if (!container.location.hasPermission()) {
            requestPermission()
            return
        }
        fetchLocation()
    }

    fun onLocationPermission(granted: Boolean) {
        if (granted) fetchLocation() else locationStatus = "Sin permiso de ubicación: el reporte se publicará sin ubicación."
    }

    fun disableLocation() {
        shareLocation = false
        location = null
        locationStatus = null
    }

    private fun fetchLocation() {
        shareLocation = true
        locationStatus = "Obteniendo tu ubicación…"
        viewModelScope.launch {
            when (val result = container.location.current()) {
                is LocationProvider.Result.Found -> {
                    location = LatLngDto(result.lat, result.lng)
                    locationStatus = "Ubicación lista (precisión ~${result.accuracyMeters.toInt()} m)"
                }
                LocationProvider.Result.Disabled -> fail("Activa la ubicación del teléfono para compartirla.")
                LocationProvider.Result.NoPermission -> fail("Sin permiso de ubicación.")
                LocationProvider.Result.Unavailable -> fail("No pudimos obtener tu ubicación. Puedes reportar sin ella.")
            }
        }
    }

    private fun fail(message: String) {
        shareLocation = false
        location = null
        locationStatus = message
    }

    fun submit() {
        val route = selectedRoute ?: return
        val selectedType = type ?: return
        if (selectedType == "otro" && description.trim().length < 10) {
            fieldErrors = mapOf("description" to "Describe la novedad en al menos 10 caracteres")
            return
        }
        if (shareLocation && location == null) {
            error = "Espera a que tengamos tu ubicación o desactiva la opción."
            return
        }
        fieldErrors = emptyMap()
        error = null
        viewModelScope.launch {
            submitting = true
            when (val result = container.reports.create(route.id, selectedType, direction, description, stopId, location)) {
                is ApiResult.Success -> success = result.value.meta.message
                is ApiResult.Failure -> {
                    error = result.error.message
                    fieldErrors = result.error.fieldErrors
                }
            }
            submitting = false
        }
    }
}
