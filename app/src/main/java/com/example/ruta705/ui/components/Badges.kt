package com.example.ruta705.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruta705.core.network.Reliability
import com.example.ruta705.core.network.RouteStatus
import com.example.ruta705.ui.theme.DangerRed
import com.example.ruta705.ui.theme.DangerSoft
import com.example.ruta705.ui.theme.InfoBlue
import com.example.ruta705.ui.theme.InfoSoft
import com.example.ruta705.ui.theme.SuccessGreen
import com.example.ruta705.ui.theme.SuccessSoft
import com.example.ruta705.ui.theme.WarningAmber
import com.example.ruta705.ui.theme.WarningSoft

/** Placa con el código de la ruta, con el color propio de la ruta si existe. */
@Composable
fun RouteCodeBadge(code: String, color: String?, modifier: Modifier = Modifier, large: Boolean = false) {
    val background = parseHexColor(color)
    Box(
        modifier
            .defaultMinSize(minWidth = if (large) 64.dp else 52.dp)
            .clip(RoundedCornerShape(if (large) 14.dp else 10.dp))
            .background(background)
            .padding(horizontal = if (large) 12.dp else 8.dp, vertical = if (large) 10.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            code,
            color = contentColorFor(background),
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (large) 18.sp else 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun Pill(text: String, background: Color, content: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(background).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Indica la calidad de la información: verificada, pendiente de verificar o demostración. */
@Composable
fun VerificationBadge(verification: String, modifier: Modifier = Modifier) {
    when (verification) {
        "verificada" -> Pill("Verificada", SuccessSoft, SuccessGreen, modifier, Icons.Default.CheckCircle)
        "demo" -> Pill("Demostración", WarningSoft, WarningAmber, modifier, Icons.Default.Science)
        else -> Pill("Por verificar", InfoSoft, InfoBlue, modifier, Icons.AutoMirrored.Filled.HelpOutline)
    }
}

@Composable
fun StatusPill(status: RouteStatus?, modifier: Modifier = Modifier) {
    status ?: return
    when (status.level) {
        "aviso_oficial" -> Pill(status.label, InfoSoft, InfoBlue, modifier, Icons.Default.Campaign)
        "novedades" -> Pill(status.label, DangerSoft, DangerRed, modifier, Icons.Default.WarningAmber)
        "reportes_sin_confirmar" -> Pill(status.label, WarningSoft, WarningAmber, modifier, Icons.Default.Info)
        else -> Pill(status.label, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant, modifier)
    }
}

@Composable
fun ReliabilityPill(reliability: Reliability, modifier: Modifier = Modifier) {
    when (reliability.level) {
        "muy_confirmado", "confirmado" -> Pill(reliability.label, SuccessSoft, SuccessGreen, modifier, Icons.Default.CheckCircle)
        "en_duda" -> Pill(reliability.label, DangerSoft, DangerRed, modifier, Icons.AutoMirrored.Filled.HelpOutline)
        else -> Pill(reliability.label, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant, modifier, Icons.AutoMirrored.Filled.HelpOutline)
    }
}

/** Aviso persistente sobre la calidad del dataset (vacío, demostración o por verificar). */
@Composable
fun DatasetNotice(mode: String, notice: String?, modifier: Modifier = Modifier) {
    if (notice == null) return
    val (background, content, icon) = when (mode) {
        "demo", "mixed" -> Triple(WarningSoft, WarningAmber, Icons.Default.Science)
        "empty" -> Triple(InfoSoft, InfoBlue, Icons.Default.Info)
        else -> Triple(InfoSoft, InfoBlue, Icons.AutoMirrored.Filled.HelpOutline)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .border(1.dp, content.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(notice, color = content, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Int = 8) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color))
}
