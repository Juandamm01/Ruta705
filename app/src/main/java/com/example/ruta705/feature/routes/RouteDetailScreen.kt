package com.example.ruta705.feature.routes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.network.LatLngDto
import com.example.ruta705.core.network.RouteDetail
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.feature.common.AnnouncementCard
import com.example.ruta705.feature.common.FavoriteButton
import com.example.ruta705.feature.common.ReportCard
import com.example.ruta705.feature.main.rememberAuthGate
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.MapLine
import com.example.ruta705.ui.components.MapMarker
import com.example.ruta705.ui.components.MarkerKind
import com.example.ruta705.ui.components.RouteCodeBadge
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.RutaMap
import com.example.ruta705.ui.components.SectionHeader
import com.example.ruta705.ui.components.SkeletonBlock
import com.example.ruta705.ui.components.SkeletonCards
import com.example.ruta705.ui.components.StatusBarScrim
import com.example.ruta705.ui.components.StatusPill
import com.example.ruta705.ui.components.VerificationBadge
import com.example.ruta705.ui.components.copText
import com.example.ruta705.ui.components.dateText
import com.example.ruta705.ui.components.parseHexColor

@Composable
fun RouteDetailScreen(routeId: String, navigator: AppNavigator, session: SessionState) {
    val vm = rutaViewModel(key = "route-$routeId") { RouteDetailViewModel(it, routeId) }
    val gate = rememberAuthGate(session, navigator)
    val context = LocalContext.current
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    // Al volver (p. ej. después de reportar) se recargan reportes y estado sin bloquear la pantalla.
    LifecycleResumeEffect(Unit) {
        vm.refreshOnResume()
        onPauseOrDispose { }
    }

    when (val state = vm.state) {
        Loadable.Loading -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            SkeletonBlock(height = 240.dp)
            Spacer(Modifier.height(16.dp))
            SkeletonCards(3)
        }
        is Loadable.Failed -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
            IconButton(onClick = navigator::back, modifier = Modifier.padding(8.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            ErrorState(state.error, onRetry = vm::load, modifier = Modifier.align(Alignment.Center))
        }
        is Loadable.Ready -> {
            val route = state.value
            Box(Modifier.fillMaxSize()) {
                RouteDetailContent(
                    route = route,
                    direction = vm.direction,
                    onDirection = { vm.direction = it },
                    onBack = navigator::back,
                    favoriteButton = {
                        FavoriteButton(route.isFavorite) {
                            gate.require("Inicia sesión para guardar esta ruta y recibir sus novedades.") { vm.toggleFavorite { msg -> msg?.let(toast) } }
                        }
                    },
                    reportCard = { report ->
                        ReportCard(
                            report,
                            showRoute = false,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                            onVote = { confirm -> gate.require("Inicia sesión para confirmar reportes.") { vm.vote(report, confirm, toast) } },
                            onFlag = { reason -> gate.require("Inicia sesión para denunciar reportes.") { vm.flag(report, reason, toast) } },
                            onDelete = { vm.delete(report, toast) },
                        )
                    },
                )
                Surface(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp,
                ) {
                    Row(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        RutaButton(
                            "Reportar novedad",
                            {
                                gate.require("Para reportar necesitas iniciar sesión. Así cuidamos la confiabilidad de la información.") {
                                    navigator.report(route.id)
                                }
                            },
                            Modifier.fillMaxWidth(),
                            icon = Icons.Default.Campaign,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteDetailContent(
    route: RouteDetail,
    direction: Int,
    onDirection: (Int) -> Unit,
    onBack: () -> Unit,
    favoriteButton: @Composable () -> Unit,
    reportCard: @Composable (com.example.ruta705.core.network.Report) -> Unit,
) {
    val selected = route.directions.firstOrNull { it.direction == direction } ?: route.directions.firstOrNull()
    val color = parseHexColor(route.color)
    val shape = remember(route.id, selected?.direction) { selected?.shape?.mapNotNull { p -> if (p.size >= 2) LatLngDto(p[0], p[1]) else null }.orEmpty() }
    val markers = remember(route.id, selected?.direction) {
        selected?.stops?.mapIndexed { index, stop ->
            MapMarker(
                LatLngDto(stop.lat, stop.lng), stop.name,
                when (index) { 0 -> MarkerKind.Origin; selected.stops.lastIndex -> MarkerKind.Destination; else -> MarkerKind.Stop },
            )
        }.orEmpty()
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrolled by remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 110.dp)) {
        item(key = "map") {
            Box {
                RutaMap(Modifier.fillMaxWidth().height(280.dp), lines = listOf(MapLine(shape, color, 7f)), markers = markers, fitPoints = shape)
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)) { favoriteButton() }
                }
            }
        }
        item(key = "header") {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RouteCodeBadge(route.code, route.color, large = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(route.name, style = MaterialTheme.typography.titleLarge)
                        route.operatorName?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerificationBadge(route.verification)
                    StatusPill(route.status)
                }
                route.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp)) }
            }
        }
        item(key = "facts") {
            FlowRow(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                Fact(Icons.Default.Timer, "Frecuencia", route.frequencyLabel, Modifier.weight(1f))
                Fact(
                    Icons.Default.Schedule, "Horario",
                    if (route.firstDeparture != null && route.lastDeparture != null) "${route.firstDeparture} a ${route.lastDeparture}" else "Sin dato verificado",
                    Modifier.weight(1f),
                )
                Fact(Icons.Default.AttachMoney, "Tarifa", route.fareCop?.let(::copText) ?: "Sin dato verificado", Modifier.weight(1f))
                Fact(Icons.Default.DateRange, "Días", route.serviceDays ?: "Sin dato verificado", Modifier.weight(1f))
            }
            Text(
                "Fuente: ${route.dataSource}" + (route.updatedAt.takeIf { it.isNotBlank() }?.let { " · Actualizado ${dateText(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        if (route.announcements.isNotEmpty()) {
            item(key = "ann-header") { SectionHeader("Avisos oficiales", Modifier.padding(horizontal = 20.dp)) }
            items(route.announcements, key = { "ann-${it.id}" }) { AnnouncementCard(it, Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) }
        }
        item(key = "reports-header") {
            SectionHeader("Reportes de la comunidad", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
        }
        if (route.reports.isEmpty()) {
            item(key = "reports-empty") {
                EmptyState(Icons.Default.Forum, "Sin reportes vigentes", "¿Viste pasar esta ruta o notas algo raro? Repórtalo y ayuda a otros pasajeros.")
            }
        } else {
            items(route.reports, key = { "rep-${it.id}" }) { reportCard(it) }
        }
        item(key = "stops-header") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                SectionHeader("Recorrido y paraderos")
                if (route.directions.size > 1) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        route.directions.forEachIndexed { index, item ->
                            SegmentedButton(
                                selected = item.direction == selected?.direction,
                                onClick = { onDirection(item.direction) },
                                shape = SegmentedButtonDefaults.itemShape(index, route.directions.size),
                            ) { Text(if (item.direction == 0) "Ida" else "Regreso") }
                        }
                    }
                }
                selected?.let {
                    Text(it.headsign, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        selected?.stops?.let { stops ->
            items(stops.size, key = { "stop-${selected.direction}-$it" }) { index ->
                StopRow(stops[index].name, stops[index].landmark, color, isFirst = index == 0, isLast = index == stops.lastIndex)
            }
        }
    }
    StatusBarScrim(scrolled)
    }
}

@Composable
private fun Fact(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    RutaCard(modifier) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StopRow(name: String, landmark: String?, color: androidx.compose.ui.graphics.Color, isFirst: Boolean, isLast: Boolean) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 20.dp)) {
        Column(Modifier.width(24.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.width(4.dp).weight(1f).background(if (isFirst) androidx.compose.ui.graphics.Color.Transparent else color))
            Box(Modifier.size(if (isFirst || isLast) 16.dp else 12.dp).background(MaterialTheme.colorScheme.surface, CircleShape).padding(3.dp).background(color, CircleShape))
            Box(Modifier.width(4.dp).weight(1f).background(if (isLast) androidx.compose.ui.graphics.Color.Transparent else color))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(vertical = 10.dp)) {
            Text(name, style = if (isFirst || isLast) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
            landmark?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
