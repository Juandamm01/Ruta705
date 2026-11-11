package com.example.ruta705.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ruta705.core.network.AppError

/** Esqueleto de carga liviano (solo anima opacidad, apto para gama baja). */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, height: Dp = 16.dp, widthFraction: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(0.45f, 0.9f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "skeleton-alpha")
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .alpha(alpha)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

@Composable
fun SkeletonCards(count: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            RutaCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(Modifier.size(48.dp), height = 48.dp, widthFraction = 0f)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(height = 16.dp, widthFraction = 0.7f)
                        SkeletonBlock(height = 12.dp, widthFraction = 0.45f)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconCircle(icon, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.primary, size = 64)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            RutaButton(actionLabel, onAction, style = ButtonStyle.Secondary)
        }
    }
}

@Composable
fun ErrorState(error: AppError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val icon = when {
        error.isOffline -> Icons.Default.CloudOff
        error.kind == AppError.Kind.RateLimited -> Icons.Default.Timer
        else -> Icons.Default.ErrorOutline
    }
    val title = when {
        error.isOffline -> "Sin conexión"
        error.kind == AppError.Kind.RateLimited -> "Un momento"
        error.kind == AppError.Kind.NotFound -> "No encontrado"
        else -> "Algo salió mal"
    }
    EmptyState(icon, title, error.message, modifier, actionLabel = onRetry?.let { "Reintentar" }, onAction = onRetry)
}

/** Mensaje de error compacto dentro de formularios o tarjetas. */
@Composable
fun InlineMessage(text: String, modifier: Modifier = Modifier, isError: Boolean = true, onRetry: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isError) colors.errorContainer else colors.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (isError) Icons.Default.ErrorOutline else Icons.Default.Timer, null, tint = if (isError) colors.onErrorContainer else colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (isError) colors.onErrorContainer else colors.onSurface, modifier = Modifier.weight(1f))
        if (onRetry != null) {
            androidx.compose.material3.IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "Reintentar", tint = if (isError) colors.onErrorContainer else colors.primary)
            }
        }
    }
}
