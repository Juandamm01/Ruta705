package com.example.ruta705.feature.auth.login

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.ruta705.R
import com.example.ruta705.ui.theme.Bone
import com.example.ruta705.ui.theme.GoldAccent
import com.example.ruta705.ui.theme.InkDark
import com.example.ruta705.ui.theme.Ruta705Theme
import com.example.ruta705.ui.theme.TealDark

private val FieldBackground = Color(0xFFEDF1EF)
private val ForgotPasswordGold = Color(0xFFB8860B)
private val MediumGray = Color(0xFF68736F)

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginClick: (email: String, password: String) -> Unit,
    onGoogleLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TealDark)
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.fondo_llanero),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            PulsingSunGlow(Modifier.align(Alignment.Center))
            AnimatedBird(modifier = Modifier.align(Alignment.TopStart).padding(start = 164.dp, top = 50.dp), durationMillis = 2800)
            AnimatedBird(modifier = Modifier.align(Alignment.TopStart).padding(start = 226.dp, top = 72.dp), durationMillis = 3400)
            AnimatedBird(modifier = Modifier.align(Alignment.TopStart).padding(start = 197.dp, top = 94.dp), durationMillis = 4000)

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TealDark.copy(alpha = .4f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Bone)
            }

            Text(
                text = "VILLAVICENCIO",
                color = Bone,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Bone.copy(alpha = .12f))
                    .border(1.dp, Bone.copy(alpha = .2f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text("Bienvenido de vuelta", color = Bone, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Inicia sesión para ver tus rutas y favoritos",
                color = Bone.copy(alpha = .85f),
                fontSize = 14.sp
            )
        }

        LoginSheet(
            modifier = Modifier.fillMaxWidth(),
            email = email,
            password = password,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onLoginClick = { onLoginClick(email, password) },
            onGoogleLogin = onGoogleLogin,
            onForgotPassword = onForgotPassword,
            onNavigateToRegister = onNavigateToRegister
        )
    }
}

@Composable
internal fun PulsingSunGlow(modifier: Modifier, offsetX: Dp = 110.dp, offsetY: Dp = (-65).dp) {
    val transition = rememberInfiniteTransition(label = "login-sun-glow")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun-glow-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = .5f,
        targetValue = .9f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun-glow-alpha"
    )

    Box(
        modifier
            .offset(x = offsetX, y = offsetY)
            .size(180.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(GoldAccent.copy(alpha = .25f), Color.Transparent)
                )
            )
            .alpha(alpha)
    )
}

@Composable
internal fun AnimatedBird(modifier: Modifier = Modifier, durationMillis: Int = 3000) {
    val transition = rememberInfiniteTransition(label = "bird-$durationMillis")
    val horizontalOffset by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(durationMillis), RepeatMode.Reverse),
        label = "bird-horizontal"
    )
    val verticalOffset by transition.animateFloat(
        initialValue = 4f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(durationMillis + 500), RepeatMode.Reverse),
        label = "bird-vertical"
    )

    Canvas(
        modifier
            .offset(x = horizontalOffset.dp, y = verticalOffset.dp)
            .size(width = 48.dp, height = 24.dp)
    ) {
        val birdPath = Path().apply {
            moveTo(1.dp.toPx(), 12.dp.toPx())
            quadraticBezierTo(9.dp.toPx(), 2.dp.toPx(), 17.dp.toPx(), 12.dp.toPx())
            quadraticBezierTo(24.dp.toPx(), 2.dp.toPx(), 32.dp.toPx(), 12.dp.toPx())
        }
        drawPath(
            path = birdPath,
            color = Bone,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
@Composable
private fun LoginSheet(
    modifier: Modifier,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Bone)
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFD0D5D2))
        )
        Spacer(Modifier.height(16.dp))
        LoginField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Correo o celular",
            leadingIcon = { Icon(Icons.Default.Email, null, tint = InkDark.copy(alpha = .5f)) },
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(16.dp))
        LoginField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Contraseña",
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = InkDark.copy(alpha = .5f)) },
            keyboardType = KeyboardType.Password,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = InkDark.copy(alpha = .5f)
                    )
                }
            }
        )
        Text(
            "¿Olvidaste tu contraseña?",
            color = ForgotPasswordGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
                .clickable(onClick = onForgotPassword)
        )
        Spacer(Modifier.height(16.dp))
        ScaledButton(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            background = GoldAccent,
            contentColor = InkDark,
            label = "Iniciar sesión"
        )
        Spacer(Modifier.height(16.dp))
        OrDivider()
        Spacer(Modifier.height(16.dp))
        ScaledButton(
            onClick = onGoogleLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color(0xFFD5DBD8), RoundedCornerShape(16.dp)),
            background = Color.White,
            contentColor = InkDark,
            label = "Continuar con Google",
            leadingContent = {
                Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("¿No tienes cuenta? ", color = InkDark.copy(alpha = .6f), fontSize = 13.sp)
            Text(
                "Crear cuenta",
                color = ForgotPasswordGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onNavigateToRegister)
            )
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, color = MediumGray) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = FieldBackground,
            unfocusedContainerColor = FieldBackground,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = InkDark,
            focusedTextColor = InkDark,
            unfocusedTextColor = InkDark
        )
    )
}

@Composable
private fun ScaledButton(
    onClick: () -> Unit,
    modifier: Modifier,
    background: Color,
    contentColor: Color,
    label: String,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, label = "button-scale")

    Row(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(source, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingContent?.invoke()
        if (leadingContent != null) Spacer(Modifier.size(10.dp))
        Text(label, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD5DBD8)))
        Text("o continúa con", color = InkDark.copy(alpha = .55f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD5DBD8)))
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,dpi=420")
@Composable
private fun LoginScreenPreview() {
    Ruta705Theme(dynamicColor = false) {
        LoginScreen({}, { _, _ -> }, {}, {}, {})
    }
}
