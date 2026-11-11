package com.example.ruta705.feature.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ruta705.R
import com.example.ruta705.ui.components.ButtonStyle
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.theme.Bone
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.TealDark

/**
 * Estructura común de las pantallas de cuenta: ilustración llanera con sol y aves animadas
 * (diseño original de Ruta705) y una hoja inferior con el formulario.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            // Fondo del color de la hoja: en pantallas altas la hoja "continúa" hasta el borde inferior.
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth().background(TealDark)) {
            Image(
                painter = painterResource(R.drawable.fondo_llanero),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to TealDark))
            )
            PulsingSunGlow(Modifier.align(Alignment.Center))
            AnimatedBird(Modifier.align(Alignment.TopStart).padding(start = 164.dp, top = 50.dp), 2800)
            AnimatedBird(Modifier.align(Alignment.TopStart).padding(start = 226.dp, top = 72.dp), 3400)
            AnimatedBird(Modifier.align(Alignment.TopStart).padding(start = 197.dp, top = 94.dp), 4000)

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TealDark.copy(alpha = .45f)),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Bone)
            }
            Text(
                "VILLAVICENCIO",
                color = Bone,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Bone.copy(alpha = .12f))
                    .border(1.dp, Bone.copy(alpha = .2f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }

        Column(Modifier.fillMaxWidth().background(TealDark).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(title, color = Bone, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Bone.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium)
        }

        Box(Modifier.fillMaxWidth().background(TealDark)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(18.dp))
            content()
        }
        }
    }
}

@Composable
fun OrDivider(label: String = "o continúa con") {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline))
    }
}

@Composable
fun GoogleButton(loading: Boolean, onDark: Boolean = false, onClick: () -> Unit) {
    RutaButton(
        text = "Continuar con Google",
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        style = if (onDark) ButtonStyle.OutlineOnDark else ButtonStyle.Outline,
        loading = loading,
    )
}

@Composable
fun AuthFooterLink(question: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Text("$question ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(
            action,
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickableNoRipple(onClick),
        )
    }
}

@Composable
internal fun PulsingSunGlow(modifier: Modifier, offsetX: Dp = 110.dp, offsetY: Dp = (-65).dp) {
    val transition = rememberInfiniteTransition(label = "sun-glow")
    val scale by transition.animateFloat(1f, 1.12f, infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sun-scale")
    val alpha by transition.animateFloat(.5f, .9f, infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sun-alpha")
    Box(
        modifier
            .offset(x = offsetX, y = offsetY)
            .size(180.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(GoldAccent.copy(alpha = .25f), Color.Transparent)))
    )
}

@Composable
internal fun AnimatedBird(modifier: Modifier = Modifier, durationMillis: Int = 3000) {
    val transition = rememberInfiniteTransition(label = "bird-$durationMillis")
    val dx by transition.animateFloat(-8f, 8f, infiniteRepeatable(tween(durationMillis), RepeatMode.Reverse), label = "bird-x")
    val dy by transition.animateFloat(4f, -4f, infiniteRepeatable(tween(durationMillis + 500), RepeatMode.Reverse), label = "bird-y")
    Canvas(modifier.offset(x = dx.dp, y = dy.dp).size(width = 34.dp, height = 18.dp)) {
        val path = Path().apply {
            moveTo(1.dp.toPx(), 12.dp.toPx())
            quadraticTo(9.dp.toPx(), 2.dp.toPx(), 17.dp.toPx(), 12.dp.toPx())
            quadraticTo(24.dp.toPx(), 2.dp.toPx(), 32.dp.toPx(), 12.dp.toPx())
        }
        drawPath(path, color = Bone, style = Stroke(width = 2.5.dp.toPx()))
    }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}
