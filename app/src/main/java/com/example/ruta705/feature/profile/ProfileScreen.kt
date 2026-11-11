package com.example.ruta705.feature.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ruta705.BuildConfig
import com.example.ruta705.core.LocalAppContainer
import com.example.ruta705.core.navigation.AppNavigator
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.ButtonStyle
import com.example.ruta705.ui.components.IconCircle
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.SectionHeader
import com.example.ruta705.ui.theme.Bone
import com.example.ruta705.ui.theme.DangerRed
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.InkDark
import com.example.ruta705.ui.theme.TealDark

@Composable
fun ProfileScreen(navigator: AppNavigator, session: SessionState) {
    val vm = rutaViewModel { ProfileViewModel(it) }
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val maxWalk by vm.maxWalk.collectAsState(initial = 600)
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val user = (session as? SessionState.Authenticated)?.user
    LaunchedEffect(user?.id) { vm.refreshProfile() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().background(TealDark).statusBarsPadding().padding(20.dp),
            ) {
                if (user != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(60.dp).background(GoldAccent, CircleShape), contentAlignment = Alignment.Center) {
                            Text(
                                user.fullName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                                style = MaterialTheme.typography.titleLarge, color = InkDark,
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.fullName, style = MaterialTheme.typography.titleLarge, color = Bone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = Bone.copy(alpha = 0.8f))
                            user.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Bone.copy(alpha = 0.8f)) }
                        }
                    }
                } else {
                    Text("Tu cuenta", style = MaterialTheme.typography.headlineSmall, color = Bone)
                    Text(
                        "Con una cuenta puedes guardar rutas y trayectos, reportar novedades y recibir avisos de tus rutas.",
                        style = MaterialTheme.typography.bodyMedium, color = Bone.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    RutaButton("Crear cuenta", navigator::register, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    RutaButton("Iniciar sesión", navigator::login, Modifier.fillMaxWidth(), style = ButtonStyle.OutlineOnDark)
                }
            }
        }
        if (user != null) {
            item { SectionHeader("Mi actividad", Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            item {
                MenuGroup {
                    MenuRow(Icons.Default.Favorite, "Rutas y trayectos guardados", onClick = navigator::favorites)
                    MenuRow(Icons.Default.Notifications, "Notificaciones", onClick = navigator::notifications)
                    MenuRow(Icons.Default.NotificationsActive, "Preferencias de avisos", onClick = navigator::notificationSettings)
                }
            }
            item { SectionHeader("Cuenta", Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
            item {
                MenuGroup {
                    MenuRow(Icons.Default.Edit, "Editar perfil", onClick = navigator::editProfile)
                    MenuRow(Icons.Default.Lock, if (user.hasPassword) "Cambiar contraseña" else "Crear contraseña", onClick = navigator::changePassword)
                    MenuRow(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", onClick = { confirmLogout = true })
                }
            }
        }
        item { SectionHeader("Preferencias", Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        item {
            MenuGroup {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.AutoMirrored.Filled.DirectionsWalk, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary, size = 38)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Distancia máxima a pie", style = MaterialTheme.typography.bodyLarge)
                            Text("Para buscar rutas cerca de tu origen y destino", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.padding(top = 8.dp, start = 50.dp)) {
                        listOf(400, 600, 900).forEach { meters ->
                            FilterChip(selected = maxWalk == meters, onClick = { vm.setMaxWalk(meters) }, label = { Text("$meters m") }, modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
                MenuRow(
                    Icons.Default.LocationOn,
                    "Permiso de ubicación",
                    subtitle = if (container.location.hasPermission()) "Concedido · solo se usa cuando lo pides" else "No concedido · se pide solo cuando lo necesites",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )
                MenuRow(Icons.Default.Info, "Acerca de Ruta705 y fuentes de datos", onClick = navigator::about)
            }
        }
        if (user != null) {
            item {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.DeleteForever, null, tint = DangerRed)
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar mi cuenta", color = DangerRed)
                }
            }
        }
        item {
            Text(
                "Ruta705 · versión ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Dejarás de recibir avisos de tus rutas en este teléfono hasta que vuelvas a iniciar sesión.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; vm.logout(navigator::signedOut) }) { Text("Cerrar sesión") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Text(
                    "Se borrarán tus datos personales, favoritos, trayectos y notificaciones. Tus reportes ya publicados " +
                        "quedarán anónimos. Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAccount { error ->
                        if (error == null) {
                            Toast.makeText(context, "Tu cuenta fue eliminada.", Toast.LENGTH_LONG).show()
                            navigator.signedOut()
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Eliminar definitivamente", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
fun MenuGroup(content: @Composable () -> Unit) {
    RutaCard(Modifier.padding(horizontal = 20.dp)) { Column { content() } }
}

@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String? = null, tint: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, MaterialTheme.colorScheme.surfaceContainer, tint ?: MaterialTheme.colorScheme.primary, size = 38)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
