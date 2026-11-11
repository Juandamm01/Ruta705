package com.example.ruta705.feature.search

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.network.NamedPlace
import com.example.ruta705.core.network.PlanOption
import com.example.ruta705.core.network.PlanResult
import com.example.ruta705.feature.main.AuthGate
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.components.DatasetNotice
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RouteCodeBadge
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.SkeletonCards
import com.example.ruta705.ui.components.VerificationBadge
import com.example.ruta705.ui.components.distanceText
import com.example.ruta705.ui.components.durationText
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.SuccessGreen

@Composable
fun SearchScreen(vm: SearchViewModel, navigator: AppNavigator, gate: AuthGate) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) vm.useMyLocation {} else vm.message = "Sin permiso de ubicación. Puedes buscar un paradero o elegir el punto en el mapa."
    }
    LaunchedEffect(vm.message) {
        vm.message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.message = null }
    }

    val screen = when {
        vm.picking != null -> 1
        vm.selected != null -> 2
        else -> 0
    }
    BackHandler(enabled = screen != 0) {
        if (vm.picking != null) vm.closePicker() else vm.selected = null
    }
    AnimatedContent(
        screen,
        transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 }) togetherWith fadeOut(tween(120)) },
        label = "search-screen",
    ) { current ->
        when (current) {
            1 -> PlacePicker(
                vm = vm,
                onRequestLocation = {
                    vm.useMyLocation {
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
            )
            2 -> vm.selected?.let { option ->
                TripDetail(
                    option = option,
                    origin = vm.origin,
                    destination = vm.destination,
                    onBack = { vm.selected = null },
                    onOpenRoute = navigator::route,
                    onSave = { name ->
                        gate.require("Inicia sesión para guardar tus trayectos frecuentes.") {
                            vm.saveTrip(name) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        }
                    },
                )
            }
            else -> PlannerHome(vm)
        }
    }
}

@Composable
private fun PlannerHome(vm: SearchViewModel) {
    val meta by vm.meta.collectAsState()
    val maxWalk by vm.maxWalk.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Text("Planea tu viaje", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    "Elige de dónde sales y a dónde vas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(16.dp))
                PlaceFields(vm.origin, vm.destination, onOrigin = { vm.openPicker(PickTarget.Origin) }, onDestination = { vm.openPicker(PickTarget.Destination) }, onSwap = vm::swap)
            }
        }
        item {
            WalkChips(maxWalk, vm::setMaxWalk)
        }
        meta?.dataset?.notice?.let { notice ->
            item { DatasetNotice(meta!!.dataset.mode, notice, Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        }
        when (val result = vm.result) {
            null -> item {
                EmptyState(
                    Icons.Default.DirectionsBus,
                    "¿Qué buseta me sirve?",
                    "Elige tu origen y tu destino. Buscaremos las rutas registradas que pasan cerca de ambos puntos, en el sentido correcto.",
                )
            }
            Loadable.Loading -> item { SkeletonCards(3, Modifier.padding(20.dp)) }
            is Loadable.Failed -> item { ErrorState(result.error, onRetry = vm::search) }
            is Loadable.Ready -> planResultItems(result.value, onSelect = { vm.selected = it })
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.planResultItems(plan: PlanResult, onSelect: (PlanOption) -> Unit) {
    plan.walkingOnly?.let { walk ->
        item {
            InlineMessage(
                "Tu destino está cerca: caminando son unos ${durationText(walk.minutes)} (${distanceText(walk.meters)}).",
                Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                isError = false,
            )
        }
    }
    if (plan.options.isEmpty()) {
        item {
            EmptyState(Icons.Default.WrongLocation, "Sin rutas para este trayecto", plan.message ?: "No encontramos rutas registradas para este trayecto.")
        }
    } else {
        item {
            Text(
                if (plan.options.size == 1) "1 opción encontrada" else "${plan.options.size} opciones encontradas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        items(plan.options, key = { it.id }) { option ->
            OptionCard(option, Modifier.padding(horizontal = 20.dp, vertical = 5.dp).animateItem()) { onSelect(option) }
        }
    }
    item {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(plan.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlaceFields(
    origin: NamedPlace?,
    destination: NamedPlace?,
    onOrigin: () -> Unit,
    onDestination: () -> Unit,
    onSwap: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            PlaceField(Icons.Default.TripOrigin, SuccessGreen, "Origen", origin?.label, "¿Desde dónde sales?", onOrigin)
            Box(Modifier.padding(start = 46.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            PlaceField(Icons.Default.Place, MaterialTheme.colorScheme.tertiary, "Destino", destination?.label, "¿A dónde vas?", onDestination)
        }
        IconButton(onClick = onSwap) { Icon(Icons.Default.SwapVert, contentDescription = "Intercambiar origen y destino", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun PlaceField(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, label: String, value: String?, placeholder: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WalkChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Caminar hasta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(400, 600, 900).forEach { meters ->
            FilterChip(
                selected = selected == meters,
                onClick = { onSelect(meters) },
                label = { Text("$meters m") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldAccent.copy(alpha = 0.35f)),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionCard(option: PlanOption, modifier: Modifier, onClick: () -> Unit) {
    val busLegs = option.legs.filter { it.isBus }
    val hasUnknownWait = busLegs.any { it.waitMinutes == null }
    RutaCard(modifier, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "~" + durationText(option.totalMinutes) + if (hasUnknownWait) " + espera" else "",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        buildString {
                            append(if (option.type == "directa") "Directo" else "Con 1 transbordo")
                            append(" · caminas ${distanceText(option.walkMeters)}")
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                option.legs.forEachIndexed { index, leg ->
                    if (leg.isBus && leg.route != null) {
                        RouteCodeBadge(leg.route.code, leg.route.color)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${leg.minutes ?: 0}′", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index < option.legs.lastIndex) Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
            busLegs.firstOrNull()?.let { first ->
                Text(
                    "Toma ${first.route?.code} en ${first.board?.label} · ${first.frequencyLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            if (option.verification != "verificada") {
                Spacer(Modifier.height(8.dp))
                VerificationBadge(option.verification)
            }
        }
    }
}
