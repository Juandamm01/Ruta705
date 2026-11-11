package com.example.ruta705.feature.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.network.Announcement
import com.example.ruta705.core.network.Report
import com.example.ruta705.core.network.RouteSummary
import com.example.ruta705.ui.components.IconCircle
import com.example.ruta705.ui.components.ReliabilityPill
import com.example.ruta705.ui.components.RouteCodeBadge
import com.example.ruta705.ui.components.RutaCard
import com.example.ruta705.ui.components.StatusPill
import com.example.ruta705.ui.components.VerificationBadge
import com.example.ruta705.ui.components.dateText
import com.example.ruta705.ui.components.minutesAgoText
import com.example.ruta705.ui.components.minutesUntil
import com.example.ruta705.ui.theme.DangerRed
import com.example.ruta705.ui.theme.DangerSoft
import com.example.ruta705.ui.theme.GoldDark
import com.example.ruta705.ui.theme.InfoBlue
import com.example.ruta705.ui.theme.InfoSoft
import com.example.ruta705.ui.theme.SuccessGreen
import com.example.ruta705.ui.theme.SuccessSoft
import com.example.ruta705.ui.theme.WarningAmber
import com.example.ruta705.ui.theme.WarningSoft

fun reportVisual(type: String): Triple<ImageVector, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (type) {
    "vi_pasar" -> Triple(Icons.Default.Visibility, SuccessSoft, SuccessGreen)
    "retraso" -> Triple(Icons.Default.Schedule, WarningSoft, WarningAmber)
    "no_pasa" -> Triple(Icons.Default.Block, DangerSoft, DangerRed)
    "desvio" -> Triple(Icons.AutoMirrored.Filled.AltRoute, InfoSoft, InfoBlue)
    else -> Triple(Icons.Default.Info, WarningSoft, GoldDark)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteRow(
    route: RouteSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
) {
    RutaCard(modifier, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            RouteCodeBadge(route.code, route.color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (route.from != null && route.to != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Text(route.from, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(horizontal = 4.dp).size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(route.to, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                }
                Text(route.frequencyLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (route.status != null && route.status.level != "sin_reportes") StatusPill(route.status)
                    if (route.verification != "verificada") VerificationBadge(route.verification)
                    route.status?.lastSightingMinutesAgo?.let {
                        com.example.ruta705.ui.components.Pill("Vista ${minutesAgoText(it)}", SuccessSoft, SuccessGreen, icon = Icons.Default.Visibility)
                    }
                }
            }
            if (onToggleFavorite != null) {
                FavoriteButton(route.isFavorite, onToggleFavorite)
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (isFavorite) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "fav-scale")
    val tint by animateColorAsState(if (isFavorite) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant, label = "fav-tint")
    IconButton(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }) {
        Icon(
            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Quitar de favoritas" else "Guardar en favoritas",
            tint = tint,
            modifier = Modifier.scale(scale),
        )
    }
}

/**
 * Reporte comunitario: siempre muestra que es de la comunidad, cuándo se publicó,
 * cuánto le queda de vigencia y su nivel de confirmación.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportCard(
    report: Report,
    modifier: Modifier = Modifier,
    showRoute: Boolean = true,
    onOpenRoute: (() -> Unit)? = null,
    onVote: ((confirm: Boolean) -> Unit)? = null,
    onFlag: ((reason: String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val (icon, background, tint) = reportVisual(report.type)
    var menu by remember { mutableStateOf(false) }
    RutaCard(modifier, onClick = onOpenRoute) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconCircle(icon, background, tint, size = 40)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showRoute) {
                            RouteCodeBadge(report.route.code, report.route.color)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(report.typeLabel, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    val remaining = minutesUntil(report.expiresAt)
                    Text(
                        buildString {
                            append("Comunidad · ${report.author} · ${minutesAgoText(report.minutesAgo)}")
                            if (!report.isActive) append(" · vencido")
                            else if (remaining != null && remaining <= 30) append(" · vence en $remaining min")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (onFlag != null || onDelete != null) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Más opciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (report.isMine && onDelete != null) {
                                DropdownMenuItem(text = { Text("Eliminar mi reporte") }, onClick = { menu = false; onDelete() })
                            }
                            if (!report.isMine && onFlag != null) {
                                listOf("falso" to "Es falso", "spam" to "Es spam", "ofensivo" to "Es ofensivo").forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text("Denunciar: $label") },
                                        leadingIcon = { Icon(Icons.Default.Flag, null) },
                                        onClick = { menu = false; onFlag(key) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            report.description?.let {
                Text("“$it”", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
            }
            report.stop?.name?.let {
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WrongLocation, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Cerca de $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ReliabilityPill(report.reliability, Modifier.padding(top = 10.dp))
            if (onVote != null && !report.isMine && report.isActive) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Te consta?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    VoteButton("Sigue así", Icons.Outlined.ThumbUp, report.myVote == "confirma") { onVote(true) }
                    VoteButton("Ya no aplica", Icons.Outlined.ThumbDown, report.myVote == "desmiente") { onVote(false) }
                }
            }
        }
    }
}

@Composable
private fun VoteButton(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(if (active) Icons.Default.CheckCircle else icon, null, Modifier.size(16.dp), tint = if (active) SuccessGreen else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) SuccessGreen else MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AnnouncementCard(announcement: Announcement, modifier: Modifier = Modifier) {
    RutaCard(modifier, color = InfoSoft) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, null, tint = InfoBlue)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (announcement.routeCode != null) "Aviso oficial · Ruta ${announcement.routeCode}" else "Aviso oficial",
                    style = MaterialTheme.typography.labelMedium, color = InfoBlue,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(announcement.title, style = MaterialTheme.typography.titleSmall, color = com.example.ruta705.ui.theme.InkDark)
            Text(announcement.body, style = MaterialTheme.typography.bodyMedium, color = com.example.ruta705.ui.theme.InkDark, modifier = Modifier.padding(top = 4.dp))
            Text(
                "Fuente: ${announcement.source} · ${dateText(announcement.startsAt)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = InfoBlue,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
