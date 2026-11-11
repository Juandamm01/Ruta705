package com.example.ruta705.feature.reports

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.feature.common.reportVisual
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.IconCircle
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RouteCodeBadge
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaTextField
import com.example.ruta705.ui.components.RutaTopBar
import com.example.ruta705.ui.components.SectionHeader
import com.example.ruta705.ui.components.SkeletonCards
import com.example.ruta705.ui.components.durationText
import com.example.ruta705.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(routeId: String?, navigator: AppNavigator, session: SessionState) {
    val vm = rutaViewModel(key = "report-$routeId") { ReportViewModel(it, routeId) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        vm.onLocationPermission(granted.values.any { it })
    }

    if (session !is SessionState.Authenticated) {
        Scaffold(topBar = { RutaTopBar("Reportar novedad", onBack = navigator::back) }) { padding ->
            EmptyState(
                Icons.Default.CheckCircle, "Inicia sesión para reportar",
                "Los reportes requieren una cuenta para evitar abusos y mostrar su confiabilidad.",
                Modifier.padding(padding), actionLabel = "Iniciar sesión", onAction = navigator::login,
            )
        }
        return
    }

    vm.success?.let { message ->
        AlertDialog(
            onDismissRequest = navigator::back,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text("¡Gracias por reportar!") },
            text = { Text("$message\n\nTu reporte se mostrará como información de la comunidad, con su hora de publicación y vigencia.") },
            confirmButton = { TextButton(onClick = navigator::back) { Text("Listo") } },
        )
    }

    Scaffold(
        topBar = { RutaTopBar("Reportar novedad", onBack = navigator::back, subtitle = "Información de la comunidad, no oficial") },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    AnimatedVisibility(vm.error != null) { InlineMessage(vm.error.orEmpty(), Modifier.padding(bottom = 10.dp)) }
                    RutaButton("Publicar reporte", vm::submit, Modifier.fillMaxWidth(), loading = vm.submitting, enabled = vm.canSubmit)
                }
            }
        },
    ) { padding ->
        when (val routes = vm.routes) {
            Loadable.Loading -> SkeletonCards(4, Modifier.padding(padding).padding(20.dp))
            is Loadable.Failed -> ErrorState(routes.error, onRetry = vm::loadRoutes, modifier = Modifier.padding(padding))
            is Loadable.Ready -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                item { SectionHeader("1. ¿Qué ruta?") }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = vm.selectedRoute?.let { "${it.code} · ${it.name}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ruta") },
                            placeholder = { Text("Elige la ruta") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = MaterialTheme.shapes.medium,
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            routes.value.forEach { route ->
                                DropdownMenuItem(
                                    text = { Text("${route.code} · ${route.name}") },
                                    leadingIcon = { RouteCodeBadge(route.code, route.color) },
                                    onClick = { vm.selectRoute(route.id); expanded = false },
                                )
                            }
                        }
                    }
                }
                item { SectionHeader("2. ¿Qué está pasando?", Modifier.padding(top = 12.dp)) }
                items(vm.types, key = { it.key }) { type ->
                    val selected = vm.type == type.key
                    val (icon, background, tint) = reportVisual(type.key)
                    val border by animateColorAsState(if (selected) GoldAccent else MaterialTheme.colorScheme.outlineVariant, label = "type-border")
                    Surface(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(MaterialTheme.shapes.large).clickable { vm.type = type.key },
                        shape = MaterialTheme.shapes.large,
                        color = if (selected) GoldAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconCircle(icon, background, tint, size = 40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(type.label, style = MaterialTheme.typography.titleSmall)
                                Text(type.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Visible durante ${durationText(type.validityMinutes)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
                vm.selectedDetail?.let { detail ->
                    if (detail.directions.size > 1) {
                        item { SectionHeader("3. Sentido (opcional)", Modifier.padding(top = 12.dp)) }
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                detail.directions.forEach { direction ->
                                    FilterChip(
                                        selected = vm.direction == direction.direction,
                                        onClick = { vm.direction = if (vm.direction == direction.direction) null else direction.direction; vm.stopId = null },
                                        label = { Text(direction.headsign) },
                                    )
                                }
                            }
                        }
                    }
                    val stops = detail.directions.firstOrNull { it.direction == (vm.direction ?: 0) }?.stops.orEmpty()
                    if (stops.isNotEmpty()) {
                        item { SectionHeader("Paradero cercano (opcional)", Modifier.padding(top = 12.dp)) }
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                                OutlinedTextField(
                                    value = stops.firstOrNull { it.id == vm.stopId }?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Paradero") },
                                    placeholder = { Text("Sin especificar") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(text = { Text("Sin especificar") }, onClick = { vm.stopId = null; expanded = false })
                                    stops.forEach { stop ->
                                        DropdownMenuItem(text = { Text(stop.name) }, onClick = { vm.stopId = stop.id; expanded = false })
                                    }
                                }
                            }
                        }
                    }
                }
                item { SectionHeader("Detalles (opcional)", Modifier.padding(top = 12.dp)) }
                item {
                    RutaTextField(
                        vm.description, { vm.description = it },
                        label = if (vm.type == "otro") "Describe la novedad (obligatorio)" else "Agrega un detalle útil",
                        placeholder = "Ej.: Desviada por obras en la Av. 40",
                        singleLine = false, minLines = 3, maxLength = 280,
                        error = vm.fieldErrors["description"],
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Compartir mi ubicación", style = MaterialTheme.typography.titleSmall)
                            Text(
                                vm.locationStatus ?: "Solo la ubicación de este momento, para ubicar el reporte. Es opcional.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = vm.shareLocation,
                            onCheckedChange = { checked ->
                                if (checked) vm.enableLocation { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                                else vm.disableLocation()
                            },
                        )
                    }
                }
                item {
                    Text(
                        "Reporta solo lo que veas tú. Los reportes falsos pueden ser ocultados por la comunidad y afectar tu reputación.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
