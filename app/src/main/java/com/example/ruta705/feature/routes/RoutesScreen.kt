package com.example.ruta705.feature.routes

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.feature.common.RouteRow
import com.example.ruta705.feature.main.AuthGate
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.DatasetNotice
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.RutaTextField
import com.example.ruta705.ui.components.SkeletonCards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(navigator: AppNavigator, gate: AuthGate) {
    val vm = rutaViewModel { RoutesViewModel(it) }
    val meta by vm.meta.collectAsState()
    val context = LocalContext.current
    val state = vm.state

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Rutas de Villavicencio", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Consulta recorridos, paraderos, frecuencias y novedades",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RutaTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                label = "Buscar por número, nombre o paradero",
                leadingIcon = Icons.Default.Search,
                imeAction = ImeAction.Search,
                maxLength = 60,
                modifier = Modifier.padding(top = 12.dp),
                trailing = if (vm.query.isNotEmpty()) {
                    { IconButton(onClick = { vm.query = "" }) { Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda") } }
                } else null,
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !vm.onlyFavorites, onClick = { vm.onlyFavorites = false }, label = { Text("Todas") })
                FilterChip(
                    selected = vm.onlyFavorites,
                    onClick = { gate.require("Inicia sesión para guardar y filtrar tus rutas favoritas.") { vm.onlyFavorites = true } },
                    label = { Text("Mis favoritas") },
                )
            }
        }
        PullToRefreshBox(
            isRefreshing = (state as? Loadable.Ready)?.refreshing == true,
            onRefresh = { vm.load() },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                meta?.dataset?.notice?.let { notice -> item { DatasetNotice(meta!!.dataset.mode, notice, Modifier.padding(bottom = 10.dp)) } }
                when (state) {
                    Loadable.Loading -> item { SkeletonCards(5) }
                    is Loadable.Failed -> item { ErrorState(state.error, onRetry = { vm.load() }) }
                    is Loadable.Ready -> {
                        val routes = vm.filtered(state.value)
                        when {
                            state.value.isEmpty() -> item {
                                EmptyState(
                                    Icons.Default.DirectionsBus,
                                    "Aún no hay rutas cargadas",
                                    "Estamos recopilando y verificando la información de las rutas de Villavicencio. Vuelve pronto.",
                                )
                            }
                            routes.isEmpty() -> item {
                                EmptyState(
                                    Icons.Default.SearchOff,
                                    if (vm.onlyFavorites) "Sin rutas favoritas" else "Sin resultados",
                                    if (vm.onlyFavorites) "Toca el corazón en una ruta para guardarla." else "No hay rutas que coincidan con \"${vm.query}\".",
                                )
                            }
                            else -> items(routes, key = { it.id }) { route ->
                                RouteRow(
                                    route = route,
                                    onClick = { navigator.route(route.id) },
                                    modifier = Modifier.padding(vertical = 5.dp).animateItem(),
                                    onToggleFavorite = {
                                        gate.require("Inicia sesión para guardar rutas y recibir sus novedades.") {
                                            vm.toggleFavorite(route) { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
