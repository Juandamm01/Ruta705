package com.example.ruta705.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.network.LatLngDto
import com.example.ruta705.core.network.NamedPlace
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.IconCircle
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaMap
import com.example.ruta705.ui.components.RutaTextField
import com.example.ruta705.ui.components.SectionHeader
import com.example.ruta705.ui.components.SkeletonCards
import com.example.ruta705.ui.components.VILLAVICENCIO_CENTER
import com.example.ruta705.ui.theme.DangerRed
import com.example.ruta705.ui.theme.InfoBlue

@Composable
fun PlacePicker(vm: SearchViewModel, onRequestLocation: () -> Unit) {
    if (vm.mapPicking) {
        MapPointPicker(
            initial = (if (vm.picking == PickTarget.Origin) vm.origin else vm.destination)?.let { LatLngDto(it.lat, it.lng) },
            resolving = vm.resolvingMapPoint,
            onBack = { vm.mapPicking = false },
            onConfirm = { vm.confirmMapPoint(it.lat, it.lng) },
        )
        return
    }
    val recent by vm.recent.collectAsState()
    val title = if (vm.picking == PickTarget.Origin) "¿Desde dónde sales?" else "¿A dónde vas?"
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::closePicker) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        RutaTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            label = "Buscar paradero o lugar",
            placeholder = "Ej.: Terminal, Unicentro, Calle 37",
            leadingIcon = Icons.Default.Search,
            imeAction = ImeAction.Search,
            onImeAction = vm::searchPlaces,
            maxLength = 120,
            modifier = Modifier.padding(horizontal = 20.dp).focusRequester(focus),
            trailing = {
                IconButton(onClick = vm::searchPlaces) { Icon(Icons.Default.Search, contentDescription = "Buscar") }
            },
        )
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
            item {
                PickerOption(
                    Icons.Default.MyLocation, "Usar mi ubicación actual", "Solo se usa para esta búsqueda",
                    trailingLoading = vm.locating, onClick = onRequestLocation,
                )
                PickerOption(Icons.Default.Map, "Elegir en el mapa", "Mueve el mapa hasta el punto exacto") { vm.mapPicking = true }
            }
            when (val results = vm.placeResults) {
                null -> if (recent.isNotEmpty()) {
                    item { SectionHeader("Recientes") }
                    items(recent, key = { "recent-${it.label}-${it.lat}" }) { place ->
                        PickerOption(Icons.Default.History, place.label, null) { vm.choose(place) }
                    }
                }
                Loadable.Loading -> item { SkeletonCards(2, Modifier.padding(top = 12.dp)) }
                is Loadable.Failed -> item { ErrorState(results.error, onRetry = vm::searchPlaces) }
                is Loadable.Ready -> {
                    val data = results.value
                    item { SectionHeader("Paraderos") }
                    if (data.stops.isEmpty()) {
                        item { Text("No hay paraderos registrados con ese nombre.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(data.stops, key = { "stop-${it.id}" }) { stop ->
                        PickerOption(
                            Icons.Default.DirectionsBus,
                            stop.name,
                            "Rutas: ${stop.routes.joinToString(", ")}",
                        ) { vm.choose(NamedPlace(stop.name, stop.lat, stop.lng)) }
                    }
                    item { SectionHeader("Direcciones y lugares") }
                    when {
                        !data.geocodingEnabled -> item {
                            InlineMessage(
                                "La búsqueda de direcciones no está activada en este servidor. Usa un paradero, tu ubicación o el mapa.",
                                isError = false,
                            )
                        }
                        data.addressError != null -> item { InlineMessage(data.addressError.message, onRetry = vm::searchPlaces) }
                        data.addresses.isEmpty() -> item {
                            Text("No encontramos direcciones en Villavicencio con ese texto.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> {
                            items(data.addresses, key = { "geo-${it.lat}-${it.lng}" }) { place ->
                                PickerOption(Icons.Default.Place, place.label, place.secondary) { vm.choose(NamedPlace(place.label, place.lat, place.lng)) }
                            }
                            item {
                                Text("Direcciones: © OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerOption(icon: ImageVector, title: String, subtitle: String?, trailingLoading: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !trailingLoading, onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary, size = 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        if (trailingLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun MapPointPicker(initial: LatLngDto?, resolving: Boolean, onBack: () -> Unit, onConfirm: (LatLngDto) -> Unit) {
    var center by remember { mutableStateOf(initial ?: VILLAVICENCIO_CENTER) }
    Box(Modifier.fillMaxSize()) {
        RutaMap(
            modifier = Modifier.fillMaxSize(),
            initialCenter = initial ?: VILLAVICENCIO_CENTER,
            initialZoom = if (initial != null) 16.0 else 14.0,
            onCenterChanged = { center = it },
        )
        // Pin fijo en el centro: el usuario mueve el mapa debajo.
        Icon(
            Icons.Default.Place, contentDescription = null, tint = DangerRed,
            modifier = Modifier.align(Alignment.Center).size(44.dp).offset(y = (-20).dp),
        )
        Row(Modifier.statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape),
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .navigationBarsPadding().padding(20.dp),
        ) {
            Text("Mueve el mapa para ubicar el punto", style = MaterialTheme.typography.titleMedium)
            Text(
                "Coordenadas: ${"%.5f".format(center.lat)}, ${"%.5f".format(center.lng)}",
                style = MaterialTheme.typography.bodySmall, color = InfoBlue, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            RutaButton("Usar este punto", { onConfirm(center) }, Modifier.fillMaxWidth(), loading = resolving)
        }
    }
}

