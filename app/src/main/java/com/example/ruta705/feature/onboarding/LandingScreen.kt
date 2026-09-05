package com.example.ruta705.feature.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruta705.R
import com.example.ruta705.ui.theme.Bone
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.InkDark
import com.example.ruta705.ui.theme.Ruta705Theme
import com.example.ruta705.ui.theme.TealDark

@Composable
fun LandingScreen(onCreateAccount: () -> Unit, onLogin: () -> Unit, onGoogleLogin: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "landing")
    val phase by transition.animateFloat(0f, 48f, infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing)), label = "phase")
    val ping by transition.animateFloat(.15f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "ping")
    val glowScale by transition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow-scale")
    val glowAlpha by transition.animateFloat(.15f, .28f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow-alpha")
    val floatOffset by transition.animateFloat(0f, -7f, infiniteRepeatable(tween(2250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val rotation by transition.animateFloat(0f, .8f, infiniteRepeatable(tween(2250, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "rotation")
    Box(Modifier.fillMaxSize().background(TealDark)) {
        RouteBackground(phase, ping, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets(0, 0, 0, 0)).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(14.dp))
                LocationPill(ping)
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(120.dp).scale(glowScale).alpha(glowAlpha).clip(CircleShape).background(Brush.radialGradient(listOf(GoldAccent.copy(.35f), GoldAccent.copy(.15f), Color.Transparent))))
                    Image(painterResource(R.drawable.ic_logo_ruta705), "Logo Ruta705", Modifier.size(128.dp).offset(y = floatOffset.dp).rotate(rotation), contentScale = ContentScale.Fit)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Text("Tu ruta en Villavo, más fácil y rápida", color = Bone, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Text("Encuentra qué buseta tomar en segundos. Horarios, recorridos y paraderos actualizados en tiempo real.", color = Bone.copy(.85f), fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureChip(Icons.Default.NearMe, "Todas las rutas")
                    FeatureChip(Icons.Default.Bolt, "Tiempo real")
                    FeatureChip(Icons.Default.Explore, "Sin perderte")
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                ActionButton("Crear cuenta", Icons.Default.PersonAdd, GoldAccent, InkDark, onCreateAccount, true)
                Spacer(Modifier.height(14.dp))
                ActionButton("Iniciar sesión", Icons.AutoMirrored.Filled.Login, Bone, InkDark, onLogin, false)
                Spacer(Modifier.height(24.dp))
                OrDivider()
                Spacer(Modifier.height(16.dp))
                GoogleButton(onGoogleLogin)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Text("Al continuar aceptas los Términos de Servicio y la Política de Privacidad de Ruta705.", color = Bone.copy(.6f), fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RouteBackground(phase: Float, ping: Float, modifier: Modifier) {
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 18f), phase)
        fun route(points: List<Pair<Float, Float>>, color: Color) {
            val path = Path().apply { moveTo(points.first().first * size.width, points.first().second * size.height); points.drop(1).forEach { lineTo(it.first * size.width, it.second * size.height) } }
            drawPath(path, color, style = Stroke(1.dp.toPx(), pathEffect = dash))
        }
        route(listOf(-.12f to .12f, .04f to .04f, .16f to .12f, .28f to .05f, .42f to .1f), GoldAccent.copy(.12f))
        route(listOf(.62f to .08f, .78f to .03f, .92f to .12f, 1.12f to .05f), Bone.copy(.1f))
        route(listOf(-.1f to .9f, .06f to .84f, .18f to .94f, .3f to .86f, .42f to .92f), GoldAccent.copy(.1f))
        route(listOf(.72f to .9f, .86f to .84f, 1.1f to .94f), Bone.copy(.08f))
        drawCircle(GoldAccent.copy(.12f), 3.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .12f))
        drawCircle(Bone.copy(.1f), 3.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .12f))
    }
}

@Composable private fun PingDot(ping: Float) = Box(Modifier.size(7.dp).scale(1f + ping * .35f).clip(CircleShape).background(GoldAccent))
@Composable private fun LocationPill(ping: Float) = Row(Modifier.clip(RoundedCornerShape(50)).background(Bone.copy(.1f)).border(1.dp, Bone.copy(.18f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { PingDot(ping); Spacer(Modifier.width(7.dp)); androidx.compose.material3.Text("VILLAVICENCIO, META", color = Bone, fontSize = 10.sp, fontWeight = FontWeight.Bold); androidx.compose.material3.Text("  •  ", color = Bone.copy(.45f), fontSize = 10.sp); androidx.compose.material3.Text("Transporte Urbano", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
@Composable private fun FeatureChip(icon: ImageVector, label: String) = Row(Modifier.clip(RoundedCornerShape(50)).background(Bone.copy(.1f)).padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Bone, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); androidx.compose.material3.Text(label, color = Bone, fontSize = 12.sp) }
@Composable private fun ActionButton(label: String, icon: ImageVector, background: Color, contentColor: Color, onClick: () -> Unit, primary: Boolean) { val source = remember { MutableInteractionSource() }; val pressed by source.collectIsPressedAsState(); val scale by animateFloatAsState(if (pressed) .98f else 1f, label = "button-scale"); Row(Modifier.fillMaxWidth().height(56.dp).scale(scale).clip(RoundedCornerShape(16.dp)).background(background).clickable(source, indication = null, onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = contentColor, modifier = Modifier.size(21.dp)); androidx.compose.material3.Text(label, Modifier.weight(1f), color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center); Box(Modifier.size(32.dp).clip(CircleShape).background(InkDark.copy(if (primary) .12f else .08f)), contentAlignment = Alignment.Center) { Icon(if (primary) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.ChevronRight, null, tint = contentColor, modifier = Modifier.size(19.dp)) } } }
@Composable private fun OrDivider() = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f).height(1.dp).background(Bone.copy(.15f))); androidx.compose.material3.Text("o continúa con", Modifier.padding(horizontal = 12.dp), color = Bone.copy(.65f), fontSize = 12.sp); Box(Modifier.weight(1f).height(1.dp).background(Bone.copy(.15f))) }
@Composable private fun GoogleButton(onClick: () -> Unit) { val source = remember { MutableInteractionSource() }; val pressed by source.collectIsPressedAsState(); val scale by animateFloatAsState(if (pressed) .98f else 1f, label = "google-scale"); Row(Modifier.fillMaxWidth().height(48.dp).scale(scale).clip(RoundedCornerShape(16.dp)).background(Bone.copy(.1f)).border(1.dp, Bone.copy(.18f), RoundedCornerShape(16.dp)).clickable(source, indication = null, onClick = onClick), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); androidx.compose.material3.Text("Continuar con Google", color = Bone, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable private fun LandingScreenPreview() { Ruta705Theme(dynamicColor = false) { LandingScreen({}, {}, {}) } }