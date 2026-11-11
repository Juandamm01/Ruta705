package com.example.ruta705.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ruta705.R
import com.example.ruta705.feature.auth.GoogleButton
import com.example.ruta705.feature.auth.clickableNoRipple
import com.example.ruta705.feature.auth.googleLoginAvailable
import com.example.ruta705.ui.components.ButtonStyle
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.theme.Bone
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.Ruta705Theme
import com.example.ruta705.ui.theme.TealDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onExplore: () -> Unit,
    onGoogleLogin: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "landing")
    val phase by transition.animateFloat(0f, 64f, infiniteRepeatable(tween(12000)), label = "phase")
    val ping by transition.animateFloat(.15f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "ping")
    val glowScale by transition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow-scale")
    val floatOffset by transition.animateFloat(0f, -7f, infiniteRepeatable(tween(2250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val rotation by transition.animateFloat(0f, .8f, infiniteRepeatable(tween(2250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "rotation")
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    Box(Modifier.fillMaxSize().background(TealDark)) {
        Image(
            painter = painterResource(R.drawable.paisaje_llanero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(440.dp).align(Alignment.BottomCenter).alpha(0.8f),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to TealDark, 0.45f to TealDark, 0.66f to TealDark.copy(alpha = 0.35f), 1f to TealDark.copy(alpha = 0.7f))
            )
        )
        RouteBackground(phase, Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            LocationPill(ping)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(120.dp).scale(glowScale).alpha(.25f).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(GoldAccent.copy(.5f), GoldAccent.copy(.15f), Color.Transparent)))
                )
                Image(
                    painterResource(R.drawable.ic_logo_ruta705), "Logo Ruta705",
                    Modifier.size(128.dp).offset(y = floatOffset.dp).rotate(rotation),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(appeared, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tu ruta en Villavo, más fácil", color = Bone, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Descubre qué buseta tomar, consulta recorridos y paraderos, y entérate de lo que reporta la comunidad sobre tus rutas.",
                        color = Bone.copy(.85f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeatureChip(Icons.Default.NearMe, "¿Qué buseta tomo?")
                        FeatureChip(Icons.Default.Route, "Recorridos y paraderos")
                        FeatureChip(Icons.Default.Groups, "Reportes de la comunidad")
                    }
                }
            }
            Spacer(Modifier.height(48.dp))
            RutaButton("Crear cuenta", onCreateAccount, Modifier.fillMaxWidth(), icon = Icons.Default.PersonAdd)
            Spacer(Modifier.height(12.dp))
            RutaButton("Iniciar sesión", onLogin, Modifier.fillMaxWidth(), style = ButtonStyle.Secondary, icon = Icons.AutoMirrored.Filled.Login)
            if (googleLoginAvailable) {
                Spacer(Modifier.height(12.dp))
                GoogleButton(loading = false, onDark = true, onClick = onGoogleLogin)
            }
            Text(
                "Explorar sin cuenta",
                color = GoldAccent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 14.dp).clickableNoRipple(onExplore).padding(10.dp),
            )
            Text(
                "Ruta705 no es un canal oficial de las empresas de transporte. Los reportes son de la comunidad y pueden no ser exactos.",
                color = Bone.copy(.65f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun RouteBackground(phase: Float, modifier: Modifier) {
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 18f), phase)
        fun route(points: List<Pair<Float, Float>>, color: Color) {
            val path = Path().apply {
                moveTo(points.first().first * size.width, points.first().second * size.height)
                points.drop(1).forEach { lineTo(it.first * size.width, it.second * size.height) }
            }
            drawPath(path, color, style = Stroke(1.dp.toPx(), pathEffect = dash))
        }
        route(listOf(-.12f to .12f, .04f to .04f, .16f to .12f, .28f to .05f, .42f to .1f), GoldAccent.copy(.14f))
        route(listOf(.62f to .08f, .78f to .03f, .92f to .12f, 1.12f to .05f), Bone.copy(.1f))
        drawCircle(GoldAccent.copy(.16f), 3.dp.toPx(), Offset(size.width * .16f, size.height * .12f))
        drawCircle(Bone.copy(.12f), 3.dp.toPx(), Offset(size.width * .92f, size.height * .12f))
    }
}

@Composable
private fun LocationPill(ping: Float) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Bone.copy(.1f)).border(1.dp, Bone.copy(.18f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).scale(1f + ping * .35f).clip(CircleShape).background(GoldAccent))
        Spacer(Modifier.width(7.dp))
        Text("VILLAVICENCIO, META", color = Bone, style = MaterialTheme.typography.labelSmall)
        Text("  •  ", color = Bone.copy(.45f), style = MaterialTheme.typography.labelSmall)
        Text("Transporte urbano", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Bone.copy(.1f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = GoldAccent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Bone, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun LandingScreenPreview() {
    Ruta705Theme { LandingScreen({}, {}, {}, {}) }
}
