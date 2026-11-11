package com.example.ruta705.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.network.LatLngDto
import com.example.ruta705.core.network.NamedPlace
import com.example.ruta705.core.network.PlanLeg
import com.example.ruta705.core.network.PlanOption
import com.example.ruta705.ui.components.ButtonStyle
import com.example.ruta705.ui.components.MapLine
import com.example.ruta705.ui.components.MapMarker
import com.example.ruta705.ui.components.MarkerKind
import com.example.ruta705.ui.components.RouteCodeBadge
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.RutaMap
import com.example.ruta705.ui.components.RutaTextField
import com.example.ruta705.ui.components.VerificationBadge
import com.example.ruta705.ui.components.distanceText
import com.example.ruta705.ui.components.durationText
import com.example.ruta705.ui.components.parseHexColor
import com.example.ruta705.ui.theme.Stone
import com.example.ruta705.ui.theme.WarningAmber

@Composable
fun TripDetail(
    option: PlanOption,
    origin: NamedPlace?,
    destination: NamedPlace?,
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onSave: (String) -> Unit,
) {
    var saving by rememberSaveable { mutableStateOf(false) }
    val lines = remember(option.id) {
        option.legs.mapNotNull { leg ->
            if (leg.isBus) MapLine(leg.path, parseHexColor(leg.route?.color), 7f)
            else if (leg.from != null && leg.to != null) MapLine(listOf(leg.from, leg.to), Stone, 4f, dashed = true)
            else null
        }
    }
    val markers = remember(option.id) {
        buildList {
            origin?.let { add(MapMarker(LatLngDto(it.lat, it.lng), it.label, MarkerKind.Origin)) }
            destination?.let { add(MapMarker(LatLngDto(it.lat, it.lng), it.label, MarkerKind.Destination)) }
            option.legs.filter { it.isBus }.forEach { leg ->
                leg.board?.let { add(MapMarker(LatLngDto(it.lat, it.lng), "Sube: ${it.label}", MarkerKind.Transfer)) }
                leg.alight?.let { add(MapMarker(LatLngDto(it.lat, it.lng), "Baja: ${it.label}", MarkerKind.Transfer)) }
            }
        }
    }
    val fit = remember(option.id) { lines.flatMap { it.points } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Box {
                RutaMap(Modifier.fillMaxWidth().height(300.dp), lines = lines, markers = markers, fitPoints = fit)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.statusBarsPadding().padding(12.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a las opciones") }
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                Text("~${durationText(option.totalMinutes)}", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${if (option.type == "directa") "Viaje directo" else "Con 1 transbordo"} · caminas ${distanceText(option.walkMeters)}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (option.verification != "verificada") {
                    Spacer(Modifier.height(8.dp))
                    VerificationBadge(option.verification)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                option.legs.forEachIndexed { index, leg ->
                    LegRow(leg, isLast = index == option.legs.lastIndex, onOpenRoute = onOpenRoute)
                }
                FinalStop(destination?.label ?: "Destino")
            }
        }
        if (option.notes.isNotEmpty()) {
            item {
                RutaCard(Modifier.padding(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(14.dp)) {
                        option.notes.forEach { note ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = WarningAmber)
                                Spacer(Modifier.width(8.dp))
                                Text(note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        item {
            RutaButton(
                "Guardar trayecto", { saving = true },
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                style = ButtonStyle.Outline, icon = Icons.Default.BookmarkAdd,
            )
        }
    }

    if (saving) {
        var name by remember { mutableStateOf("${origin?.label.orEmpty()} → ${destination?.label.orEmpty()}".take(60)) }
        AlertDialog(
            onDismissRequest = { saving = false },
            title = { Text("Guardar trayecto") },
            text = {
                Column {
                    Text("Ponle un nombre para encontrarlo rápido, por ejemplo \"Casa → Trabajo\".", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    RutaTextField(name, { name = it }, "Nombre", maxLength = 60)
                }
            },
            confirmButton = { TextButton(onClick = { saving = false; onSave(name) }, enabled = name.trim().length >= 2) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { saving = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LegRow(leg: PlanLeg, isLast: Boolean, onOpenRoute: (String) -> Unit) {
    val color = if (leg.isBus) parseHexColor(leg.route?.color) else MaterialTheme.colorScheme.outline
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(28.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(14.dp).background(color, CircleShape))
            Box(Modifier.width(if (leg.isBus) 5.dp else 2.dp).weight(1f).background(color))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = 18.dp)) {
            if (leg.isBus && leg.route != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RouteCodeBadge(leg.route.code, leg.route.color)
                    Spacer(Modifier.width(8.dp))
                    Text("Toma la buseta", style = MaterialTheme.typography.titleSmall)
                }
                Text(leg.route.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                Text(leg.headsign.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                DetailLine("Sube en", leg.board?.label.orEmpty())
                DetailLine("Bájate en", leg.alight?.label.orEmpty())
                DetailLine(
                    "Recorrido",
                    "~${durationText(leg.rideMinutes ?: 0)} · ${distanceText(leg.rideMeters ?: 0)}" +
                        (leg.stopsBetween?.takeIf { it > 0 }?.let { " · $it paraderos intermedios" } ?: ""),
                )
                DetailLine("Frecuencia", leg.frequencyLabel.orEmpty())
                leg.waitMinutes?.let { DetailLine("Espera estimada", "~$it min") }
                leg.serviceHoursLabel?.let { DetailLine("Horario registrado", it) }
                TextButton(onClick = { onOpenRoute(leg.route.id) }, contentPadding = PaddingValues(0.dp)) { Text("Ver recorrido completo y reportes") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text("Camina ${durationText(leg.minutes ?: 0)} (${distanceText(leg.meters ?: 0)})", style = MaterialTheme.typography.titleSmall)
                }
                Text("Hasta ${leg.toLabel.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
    if (isLast) Unit
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FinalStop(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.tertiary)
        }
        Spacer(Modifier.width(12.dp))
        Text("Llegas a $label", style = MaterialTheme.typography.titleSmall)
    }
}
