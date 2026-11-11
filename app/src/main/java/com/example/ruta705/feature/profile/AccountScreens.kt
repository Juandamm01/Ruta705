package com.example.ruta705.feature.profile

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ruta705.BuildConfig
import com.example.ruta705.core.LocalAppContainer
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.notifications.NotificationSync
import com.example.ruta705.core.network.AppNotification
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.IconCircle
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RouteCodeBadge
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.RutaTextField
import com.example.ruta705.ui.components.RutaTopBar
import com.example.ruta705.ui.components.SectionHeader
import com.example.ruta705.ui.components.SkeletonCards
import com.example.ruta705.ui.components.VerificationBadge
import com.example.ruta705.ui.components.dateText
import com.example.ruta705.ui.components.minutesAgoText
import com.example.ruta705.ui.components.minutesSince
import com.example.ruta705.ui.theme.GoldAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navigator: AppNavigator) {
    val vm = rutaViewModel { FavoritesViewModel(it) }
    val context = LocalContext.current
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(topBar = { RutaTopBar("Guardados", onBack = navigator::back) }) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Rutas") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Trayectos") })
            }
            when (val state = vm.state) {
                Loadable.Loading -> SkeletonCards(3, Modifier.padding(20.dp))
                is Loadable.Failed -> ErrorState(state.error, onRetry = vm::load)
                is Loadable.Ready -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tab == 0) {
                        if (state.value.routes.isEmpty()) item {
                            EmptyState(Icons.Default.FavoriteBorder, "Aún no guardas rutas", "En el detalle de una ruta toca el corazón para guardarla y recibir sus novedades.")
                        }
                        items(state.value.routes, key = { it.routeId }) { route ->
                            RutaCard(Modifier.animateItem(), onClick = { navigator.route(route.routeId) }) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RouteCodeBadge(route.code, route.color)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(route.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(if (route.notify) "Recibes avisos" else "Sin avisos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (route.verification != "verificada") VerificationBadge(route.verification, Modifier.padding(top = 4.dp))
                                    }
                                    Switch(checked = route.notify, onCheckedChange = { vm.setNotify(route, it, toast) })
                                    IconButton(onClick = { vm.removeRoute(route, toast) }) { Icon(Icons.Default.Delete, contentDescription = "Quitar ${route.code} de favoritas") }
                                }
                            }
                        }
                    } else {
                        if (state.value.trips.isEmpty()) item {
                            EmptyState(Icons.Default.Place, "Sin trayectos guardados", "Después de buscar un viaje, toca \"Guardar trayecto\" para tenerlo a un toque desde Inicio.")
                        }
                        items(state.value.trips, key = { it.id }) { trip ->
                            RutaCard(Modifier.animateItem()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconCircle(Icons.Default.Place, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.tertiary, size = 40)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(trip.name, style = MaterialTheme.typography.titleSmall)
                                        Text("${trip.origin.label} → ${trip.destination.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { vm.removeTrip(trip, toast) }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar trayecto ${trip.name}") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(navigator: AppNavigator) {
    val vm = rutaViewModel { NotificationsViewModel(it) }
    Scaffold(
        topBar = {
            RutaTopBar("Notificaciones", onBack = navigator::back) {
                if ((vm.state as? Loadable.Ready)?.value?.any { !it.read } == true) {
                    TextButton(onClick = vm::markAll) { Text("Marcar leídas") }
                }
            }
        },
    ) { padding ->
        when (val state = vm.state) {
            Loadable.Loading -> SkeletonCards(4, Modifier.padding(padding).padding(20.dp))
            is Loadable.Failed -> ErrorState(state.error, onRetry = vm::load, modifier = Modifier.padding(padding))
            is Loadable.Ready -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.value.isEmpty()) item {
                    EmptyState(
                        Icons.Default.NotificationsNone, "No tienes notificaciones",
                        "Te avisaremos aquí cuando la comunidad reporte retrasos, desvíos o interrupciones en tus rutas guardadas, o cuando haya avisos oficiales.",
                    )
                }
                items(state.value, key = { it.id }) { item ->
                    NotificationRow(item) {
                        vm.open(item)
                        item.routeId?.let(navigator::route)
                    }
                }
                if (vm.nextCursor != null) item {
                    TextButton(onClick = vm::loadMore, modifier = Modifier.fillMaxWidth()) { Text(if (vm.loadingMore) "Cargando…" else "Ver más") }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: AppNotification, onClick: () -> Unit) {
    RutaCard(onClick = onClick, color = if (item.read) MaterialTheme.colorScheme.surface else GoldAccent.copy(alpha = 0.12f)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (item.kind == "announcement") "Aviso oficial" else "Reporte de la comunidad",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f),
                )
                Text(minutesAgoText(minutesSince(item.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold), modifier = Modifier.padding(top = 4.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun NotificationSettingsScreen(navigator: AppNavigator) {
    val vm = rutaViewModel { NotificationSettingsViewModel(it) }
    val context = LocalContext.current
    var systemAllowed by remember { mutableStateOf(NotificationSync.canPost(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        systemAllowed = granted && NotificationSync.canPost(context)
        if (!granted) Toast.makeText(context, "Puedes activarlas luego desde los ajustes del teléfono.", Toast.LENGTH_LONG).show()
    }
    Scaffold(topBar = { RutaTopBar("Preferencias de avisos", onBack = navigator::back) }) { padding ->
        when (val state = vm.state) {
            Loadable.Loading -> SkeletonCards(3, Modifier.padding(padding).padding(20.dp))
            is Loadable.Failed -> ErrorState(state.error, onRetry = vm::load, modifier = Modifier.padding(padding))
            is Loadable.Ready -> {
                val prefs = state.value
                val save: (com.example.ruta705.core.network.NotificationPrefs) -> Unit = { vm.save(it) { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } }
                Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
                    if (!systemAllowed) {
                        InlineMessage("Las notificaciones están desactivadas en este teléfono. Actívalas para recibir avisos fuera de la app.", isError = false)
                        Spacer(Modifier.height(8.dp))
                        RutaButton(
                            "Permitir notificaciones",
                            {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            },
                            Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    SwitchRow("Novedades de mis rutas", "Reportes de retrasos, rutas que no pasan o desvíos en rutas que guardaste", prefs.favoriteReports) {
                        save(prefs.copy(favoriteReports = it))
                    }
                    SwitchRow("Avisos oficiales", "Cierres de vía o cambios publicados con fuente oficial", prefs.serviceAlerts) {
                        save(prefs.copy(serviceAlerts = it))
                    }
                    SwitchRow("Horas de silencio (10 p.m. – 6 a.m.)", "No mostramos notificaciones en ese horario; las verás en la bandeja", prefs.quietStart != null) { on ->
                        save(prefs.copy(quietStart = if (on) "22:00" else null, quietEnd = if (on) "06:00" else null))
                    }
                    Text(
                        "Cómo funcionan: revisamos tus novedades aproximadamente cada 15 minutos mientras tengas internet. " +
                            "Solo te avisamos de eventos reales registrados en Ruta705, y como máximo una vez cada 30 minutos por ruta.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun EditProfileScreen(navigator: AppNavigator) {
    val vm = rutaViewModel { AccountFormViewModel(it) }
    val context = LocalContext.current
    Scaffold(topBar = { RutaTopBar("Editar perfil", onBack = navigator::back) }) { padding ->
        Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            RutaTextField(vm.fullName, { vm.fullName = it }, "Nombre", leadingIcon = Icons.Default.Badge, error = vm.fieldErrors["fullName"], maxLength = 80)
            Spacer(Modifier.height(10.dp))
            RutaTextField(
                vm.phone, { vm.phone = it }, "Celular (opcional)", leadingIcon = Icons.Default.PhoneAndroid,
                keyboardType = KeyboardType.Phone, error = vm.fieldErrors["phone"], maxLength = 16,
            )
            Text("Correo: ${vm.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            vm.error?.let { InlineMessage(it, Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(20.dp))
            RutaButton("Guardar cambios", {
                vm.saveProfile { Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show(); navigator.back() }
            }, Modifier.fillMaxWidth(), loading = vm.loading)
        }
    }
}

@Composable
fun ChangePasswordScreen(navigator: AppNavigator) {
    val vm = rutaViewModel { AccountFormViewModel(it) }
    val context = LocalContext.current
    Scaffold(topBar = { RutaTopBar(if (vm.hasPassword) "Cambiar contraseña" else "Crear contraseña", onBack = navigator::back) }) { padding ->
        Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (vm.hasPassword) {
                RutaTextField(vm.currentPassword, { vm.currentPassword = it }, "Contraseña actual", leadingIcon = Icons.Default.Lock, isPassword = true, error = vm.fieldErrors["currentPassword"])
                Spacer(Modifier.height(10.dp))
            }
            RutaTextField(
                vm.newPassword, { vm.newPassword = it }, "Nueva contraseña", leadingIcon = Icons.Default.Lock, isPassword = true,
                error = vm.fieldErrors["newPassword"], supporting = "Mínimo 8 caracteres, con letras y números",
            )
            Text(
                "Por seguridad cerraremos tus sesiones en otros teléfonos.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
            )
            vm.error?.let { InlineMessage(it, Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(20.dp))
            RutaButton("Guardar contraseña", {
                vm.changePassword { Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show(); navigator.back() }
            }, Modifier.fillMaxWidth(), loading = vm.loading)
        }
    }
}

@Composable
fun AboutScreen(navigator: AppNavigator) {
    val meta by LocalAppContainer.current.meta.meta.collectAsState()
    Scaffold(topBar = { RutaTopBar("Acerca de Ruta705", onBack = navigator::back) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AboutBlock(
                "Qué es Ruta705",
                "Una app ciudadana para moverse en el transporte público de Villavicencio: qué buseta tomar, recorridos, paraderos, " +
                    "frecuencias estimadas y lo que reporta la comunidad. No es un canal oficial de las empresas de transporte " +
                    "ni de la Alcaldía, y no rastrea las busetas por GPS.",
            )
            meta?.dataset?.let { dataset ->
                AboutBlock(
                    "Estado de la información de rutas",
                    buildString {
                        append("Rutas cargadas: ${dataset.routes} (verificadas: ${dataset.verified}, por verificar: ${dataset.pending}, demostración: ${dataset.demo}).")
                        dataset.lastImportSource?.let { append("\nÚltima fuente: $it") }
                        dataset.lastImportAt?.let { append("\nActualizado: ${dateText(it)}") }
                    },
                )
            }
            AboutBlock(
                "Reportes de la comunidad",
                "Los publican personas usuarias. Cada reporte muestra su hora, su vigencia (un reporte viejo deja de mostrarse como actual) " +
                    "y cuántas personas lo confirman o desmienten. Úsalos como referencia, no como información oficial.",
            )
            AboutBlock(
                "Mapas y direcciones",
                "Mapas y búsqueda de direcciones: © OpenStreetMap contributors (licencia ODbL). Tu ubicación solo se usa cuando la pides " +
                    "y no se guarda, salvo que decidas adjuntarla a un reporte.",
            )
            Text(
                "Versión ${BuildConfig.VERSION_NAME} · Servidor: ${BuildConfig.API_BASE_URL.ifBlank { "no configurado" }}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AboutBlock(title: String, body: String) {
    RutaCard {
        Column(Modifier.padding(16.dp)) {
            SectionHeader(title)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

