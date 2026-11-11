package com.example.ruta705.feature.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.feature.home.HomeScreen
import com.example.ruta705.feature.profile.ProfileScreen
import com.example.ruta705.feature.routes.RoutesScreen
import com.example.ruta705.feature.search.SearchScreen
import com.example.ruta705.feature.search.SearchViewModel
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.theme.GoldSoft
import com.example.ruta705.ui.theme.InkDark

private data class Tab(val label: String, val selected: ImageVector, val unselected: ImageVector)

private val tabs = listOf(
    Tab("Inicio", Icons.Filled.Home, Icons.Outlined.Home),
    Tab("Buscar", Icons.Filled.Search, Icons.Outlined.Search),
    Tab("Rutas", Icons.Filled.DirectionsBus, Icons.Outlined.DirectionsBus),
    Tab("Perfil", Icons.Filled.Person, Icons.Outlined.Person),
)

/** Permite a cualquier pantalla pedir inicio de sesión antes de una acción que lo requiere. */
class AuthGate(private val session: SessionState, private val show: (String) -> Unit) {
    val isLoggedIn: Boolean get() = session is SessionState.Authenticated
    fun require(reason: String, action: () -> Unit) = if (isLoggedIn) action() else show(reason)
}

@Composable
fun rememberAuthGate(session: SessionState, navigator: AppNavigator): AuthGate {
    var reason by remember { mutableStateOf<String?>(null) }
    reason?.let { text ->
        LoginRequiredDialog(
            reason = text,
            onLogin = { reason = null; navigator.login() },
            onRegister = { reason = null; navigator.register() },
            onDismiss = { reason = null },
        )
    }
    return AuthGate(session) { reason = it }
}

@Composable
fun LoginRequiredDialog(reason: String, onLogin: () -> Unit, onRegister: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Necesitas una cuenta") },
        text = { Text(reason) },
        confirmButton = { TextButton(onClick = onLogin) { Text("Iniciar sesión") } },
        dismissButton = { TextButton(onClick = onRegister) { Text("Crear cuenta") } },
    )
}

@Composable
fun MainScreen(navigator: AppNavigator, session: SessionState) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val searchVm = rutaViewModel { SearchViewModel(it) }
    val gate = rememberAuthGate(session, navigator)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(if (selected == index) tab.selected else tab.unselected, contentDescription = null) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = InkDark,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = GoldSoft,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "tabs",
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        navigator = navigator,
                        session = session,
                        gate = gate,
                        onSearch = { selected = 1 },
                        onOpenTrip = { trip -> searchVm.loadTrip(trip); selected = 1 },
                        onOpenRoutes = { selected = 2 },
                    )
                    1 -> SearchScreen(vm = searchVm, navigator = navigator, gate = gate)
                    2 -> RoutesScreen(navigator = navigator, gate = gate)
                    else -> ProfileScreen(navigator = navigator, session = session)
                }
            }
        }
    }
}

