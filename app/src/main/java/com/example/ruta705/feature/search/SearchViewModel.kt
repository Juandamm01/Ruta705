package com.example.ruta705.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.location.LocationProvider
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppError
import com.example.ruta705.core.network.GeoPlace
import com.example.ruta705.core.network.NamedPlace
import com.example.ruta705.core.network.PlanOption
import com.example.ruta705.core.network.PlanResult
import com.example.ruta705.core.network.SavedTrip
import com.example.ruta705.core.network.StopItem
import com.example.ruta705.core.network.getOrNull
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.toLoadable
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PickTarget { Origin, Destination }

data class PlaceSearchResults(
    val stops: List<StopItem>,
    val addresses: List<GeoPlace>,
    val addressError: AppError?,
    val geocodingEnabled: Boolean,
)

class SearchViewModel(private val container: AppContainer) : ViewModel() {
    var origin by mutableStateOf<NamedPlace?>(null)
        private set
    var destination by mutableStateOf<NamedPlace?>(null)
        private set
    var picking by mutableStateOf<PickTarget?>(null)
    var result by mutableStateOf<Loadable<PlanResult>?>(null)
        private set
    var selected by mutableStateOf<PlanOption?>(null)
    var message by mutableStateOf<String?>(null)

    // Selector de lugares
    var query by mutableStateOf("")
    var placeResults by mutableStateOf<Loadable<PlaceSearchResults>?>(null)
        private set
    var locating by mutableStateOf(false)
        private set
    var mapPicking by mutableStateOf(false)
    var resolvingMapPoint by mutableStateOf(false)
        private set

    val recent = container.settings.recent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val maxWalk = container.settings.maxWalkMeters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 600)
    val meta = container.meta.meta

    private var searchJob: Job? = null

    fun openPicker(target: PickTarget) {
        picking = target
        query = ""
        placeResults = null
        mapPicking = false
    }

    fun closePicker() {
        picking = null
        mapPicking = false
    }

    fun loadTrip(trip: SavedTrip) {
        origin = trip.origin
        destination = trip.destination
        search()
    }

    fun swap() {
        val previous = origin
        origin = destination
        destination = previous
        if (origin != null && destination != null) search()
    }

    fun choose(place: NamedPlace) {
        when (picking) {
            PickTarget.Origin -> origin = place
            PickTarget.Destination -> destination = place
            null -> return
        }
        viewModelScope.launch { container.settings.addRecent(place) }
        closePicker()
        if (origin != null && destination != null) search()
    }

    /** Busca paraderos (siempre) y direcciones (si el geocodificador está habilitado). Solo al confirmar. */
    fun searchPlaces() {
        val text = query.trim()
        if (text.length < 3) {
            message = "Escribe al menos 3 letras"
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            placeResults = Loadable.Loading
            val geocodingEnabled = container.meta.meta.value?.features?.geocoding == true
            val stopsCall = async { container.transit.searchStops(text) }
            val geoCall = async { if (geocodingEnabled) container.transit.geocode(text) else null }
            val stops = stopsCall.await()
            val geo = geoCall.await()
            placeResults = when {
                stops is ApiResult.Failure && (geo == null || geo is ApiResult.Failure) -> Loadable.Failed(stops.error)
                else -> Loadable.Ready(
                    PlaceSearchResults(
                        stops = stops.getOrNull().orEmpty(),
                        addresses = geo?.getOrNull().orEmpty(),
                        addressError = (geo as? ApiResult.Failure)?.error,
                        geocodingEnabled = geocodingEnabled,
                    )
                )
            }
        }
    }

    fun useMyLocation(onNeedsPermission: () -> Unit) {
        if (!container.location.hasPermission()) {
            onNeedsPermission()
            return
        }
        viewModelScope.launch {
            locating = true
            when (val location = container.location.current()) {
                is LocationProvider.Result.Found -> {
                    val area = container.meta.meta.value?.serviceArea
                    if (area != null && !area.contains(location.lat, location.lng)) {
                        message = "Estás fuera de Villavicencio. Elige un punto dentro de la ciudad."
                    } else {
                        choose(NamedPlace("Mi ubicación", location.lat, location.lng))
                    }
                }
                LocationProvider.Result.Disabled -> message = "Activa la ubicación del teléfono para usar tu posición."
                LocationProvider.Result.NoPermission -> onNeedsPermission()
                LocationProvider.Result.Unavailable -> message = "No pudimos obtener tu ubicación. Intenta al aire libre o elige el punto en el mapa."
            }
            locating = false
        }
    }

    fun confirmMapPoint(lat: Double, lng: Double) {
        val area = container.meta.meta.value?.serviceArea
        if (area != null && !area.contains(lat, lng)) {
            message = "Ese punto está fuera de Villavicencio."
            return
        }
        viewModelScope.launch {
            resolvingMapPoint = true
            val label = if (container.meta.meta.value?.features?.geocoding == true) {
                container.transit.reverseGeocode(lat, lng).getOrNull()?.label
            } else null
            resolvingMapPoint = false
            choose(NamedPlace(label ?: "Punto en el mapa", lat, lng))
        }
    }

    fun search() {
        val from = origin ?: return
        val to = destination ?: return
        selected = null
        viewModelScope.launch {
            result = Loadable.Loading
            result = container.transit.plan(from, to, maxWalk.value).toLoadable()
        }
    }

    fun setMaxWalk(meters: Int) {
        viewModelScope.launch {
            container.settings.setMaxWalkMeters(meters)
            if (origin != null && destination != null) {
                result = Loadable.Loading
                result = container.transit.plan(origin!!, destination!!, meters).toLoadable()
            }
        }
    }

    fun saveTrip(name: String, onDone: (String) -> Unit) {
        val from = origin ?: return
        val to = destination ?: return
        viewModelScope.launch {
            when (val saved = container.favorites.saveTrip(name, from, to)) {
                is ApiResult.Success -> onDone("Trayecto guardado. Lo verás en Inicio.")
                is ApiResult.Failure -> onDone(saved.error.fieldErrors.values.firstOrNull() ?: saved.error.message)
            }
        }
    }
}
