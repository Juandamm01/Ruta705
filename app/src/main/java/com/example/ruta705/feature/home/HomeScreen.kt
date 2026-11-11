package com.example.ruta705.feature.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.ruta705.R
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.network.SavedTrip
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.feature.common.AnnouncementCard
import com.example.ruta705.feature.common.ReportCard
import com.example.ruta705.feature.common.RouteRow
import com.example.ruta705.feature.main.AuthGate
import com.example.ruta705.ui.common.Loadable
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.DatasetNotice
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.ErrorState
import com.example.ruta705.ui.components.IconCircle
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.SectionHeader
import com.example.ruta705.ui.components.SkeletonCards
import com.example.ruta705.ui.components.greeting
import com.example.ruta705.ui.components.pressScale
import com.example.ruta705.ui.theme.Bone
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.GoldSoft
import com.example.ruta705.ui.theme.InkDark
import com.example.ruta705.ui.theme.TealDark
import com.example.ruta705.ui.theme.TealMid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigator: AppNavigator,
    session: SessionState,
    gate: AuthGate,
    onSearch: () -> Unit,
    onOpenTrip: (SavedTrip) -> Unit,
    onOpenRoutes: () -> Unit,
) {
    val vm = rutaViewModel { HomeViewModel(it) }
    val meta by vm.meta.collectAsState()
    val unread by vm.unread.collectAsState()
    val context = LocalContext.current
    val loggedIn = session is SessionState.Authenticated
    LaunchedEffect(loggedIn) { vm.ensureLoaded(loggedIn) }
    LifecycleResumeEffect(Unit) {
        vm.refreshOnResume()
        onPauseOrDispose { }
    }

    val state = vm.state
    PullToRefreshBox(
        isRefreshing = (state as? Loadable.Ready)?.refreshing == true,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val scrolled by remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex > 0 } }
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "hero") {
                HomeHero(
                    name = (session as? SessionState.Authenticated)?.user?.firstName,
                    unread = unread,
                    showBell = loggedIn,
                    onBell = navigator::notifications,
                    onSearch = onSearch,
                )
            }
            item(key = "actions") {
                QuickActions(
                    onRoutes = onOpenRoutes,
                    onReport = { gate.require("Para reportar una novedad necesitas iniciar sesión. Así evitamos reportes falsos.") { navigator.report() } },
                    onFavorites = { gate.require("Inicia sesión para guardar tus rutas y trayectos frecuentes.") { navigator.favorites() } },
                )
            }
            meta?.dataset?.let { dataset ->
                if (dataset.notice != null) item(key = "notice") {
                    DatasetNotice(dataset.mode, dataset.notice, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }
            }
            when (state) {
                Loadable.Loading -> item(key = "loading") { SkeletonCards(3, Modifier.padding(20.dp)) }
                is Loadable.Failed -> item(key = "error") { ErrorState(state.error, onRetry = { vm.refresh() }) }
                is Loadable.Ready -> {
                    val data = state.value
                    if (data.trips.isNotEmpty()) {
                        item(key = "trips-header") { SectionHeader("Tus trayectos", Modifier.padding(horizontal = 20.dp)) }
                        item(key = "trips") {
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(data.trips, key = { it.id }) { trip -> TripChip(trip) { onOpenTrip(trip) } }
                            }
                        }
                    }

                    item(key = "fav-header") {
                        SectionHeader(
                            "Tus rutas", Modifier.padding(horizontal = 20.dp),
                            actionLabel = if (data.favoriteRoutes.isNotEmpty()) "Ver todas" else null,
                            onAction = navigator::favorites,
                        )
                    }
                    if (data.favoriteRoutes.isEmpty()) {
                        item(key = "fav-empty") {
                            RutaCard(Modifier.padding(horizontal = 20.dp), onClick = onOpenRoutes) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconCircle(Icons.Default.FavoriteBorder, GoldSoft, InkDark)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Guarda las rutas que usas", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            if (loggedIn) "Toca el corazón en cualquier ruta y verás aquí sus novedades."
                                            else "Crea una cuenta para tener tus rutas a un toque y recibir novedades.",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(data.favoriteRoutes, key = { "fav-${it.id}" }) { route ->
                            RouteRow(route, onClick = { navigator.route(route.id) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp).animateItem())
                        }
                    }

                    item(key = "news-header") { SectionHeader("Novedades oficiales", Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) }
                    if (data.announcements.isEmpty()) {
                        item(key = "news-empty") {
                            Text(
                                "No hay avisos oficiales vigentes. Aquí verás cierres de vías o cambios de recorrido publicados con su fuente.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        items(data.announcements, key = { "ann-${it.id}" }) {
                            AnnouncementCard(it, Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
                        }
                    }

                    item(key = "reports-header") {
                        SectionHeader(
                            if (data.reportsForFavorites) "Reportes en tus rutas" else "Reportes recientes de la comunidad",
                            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                        )
                    }
                    if (data.reports.isEmpty()) {
                        item(key = "reports-empty") {
                            EmptyState(
                                Icons.Default.Forum,
                                "Sin reportes vigentes",
                                "Cuando alguien reporte que vio pasar una ruta, un retraso o un desvío, lo verás aquí con su hora y vigencia.",
                            )
                        }
                    } else {
                        items(data.reports, key = { "rep-${it.id}" }) { report ->
                            ReportCard(
                                report,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp).animateItem(),
                                onOpenRoute = { navigator.route(report.route.id) },
                                onVote = { confirm ->
                                    gate.require("Inicia sesión para confirmar reportes de la comunidad.") {
                                        vm.vote(report, confirm) { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                                    }
                                },
                            )
                        }
                    }
                    item(key = "how") { HowItWorks(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) }
                }
            }
        }
        com.example.ruta705.ui.components.StatusBarScrim(scrolled)
    }
}

@Composable
private fun HomeHero(name: String?, unread: Int, showBell: Boolean, onBell: () -> Unit, onSearch: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(TealDark)
    ) {
        Image(
            painterResource(R.drawable.paisaje_llanero), null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().alpha(0.35f),
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(TealDark.copy(alpha = 0.55f), TealDark.copy(alpha = 0.95f)))))
        Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_logo_ruta705), "Ruta705", Modifier.size(40.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (name != null) "${greeting()}, $name" else greeting(), color = Bone, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Villavicencio, Meta", color = Bone.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                }
                if (showBell) {
                    IconButton(onClick = onBell) {
                        BadgedBox(badge = { if (unread > 0) Badge(containerColor = GoldAccent, contentColor = InkDark) { Text(if (unread > 9) "9+" else "$unread") } }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = Bone)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(appeared, enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }) {
                SearchCard(onSearch)
            }
        }
    }
}

@Composable
private fun SearchCard(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clip(RoundedCornerShape(18.dp))
            .background(Bone)
            .clickable(source, null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GoldAccent), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Search, null, tint = InkDark)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("¿A dónde vas?", style = MaterialTheme.typography.titleMedium, color = InkDark)
            Text("Te decimos qué buseta tomar", style = MaterialTheme.typography.bodySmall, color = com.example.ruta705.ui.theme.Stone)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TealMid)
    }
}

@Composable
private fun QuickActions(onRoutes: () -> Unit, onReport: () -> Unit, onFavorites: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(Icons.Default.DirectionsBus, "Rutas", Modifier.weight(1f), onRoutes)
        QuickAction(Icons.Default.Campaign, "Reportar", Modifier.weight(1f), onReport)
        QuickAction(Icons.Default.FavoriteBorder, "Favoritos", Modifier.weight(1f), onFavorites)
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    RutaCard(modifier, onClick = onClick) {
        Column(Modifier.padding(vertical = 14.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconCircle(icon, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary, size = 42)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TripChip(trip: SavedTrip, onClick: () -> Unit) {
    RutaCard(Modifier.widthIn(min = 180.dp, max = 240.dp), onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(trip.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${trip.origin.label} → ${trip.destination.label}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun HowItWorks(modifier: Modifier) {
    RutaCard(modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Default.TipsAndUpdates, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("¿Cómo funciona Ruta705?", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Las busetas no tienen GPS conectado a la app. Te mostramos recorridos, paraderos y frecuencias estimadas, " +
                        "y lo que la comunidad reporta en el momento: cuándo vio pasar una ruta, retrasos o desvíos. " +
                        "Cada reporte muestra su hora, su vigencia y cuántas personas lo confirman.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

