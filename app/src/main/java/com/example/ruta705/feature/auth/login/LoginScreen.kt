package com.example.ruta705.feature.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ruta705.feature.auth.AuthFooterLink
import com.example.ruta705.feature.auth.AuthScaffold
import com.example.ruta705.feature.auth.GoogleButton
import com.example.ruta705.feature.auth.LoginViewModel
import com.example.ruta705.feature.auth.OrDivider
import com.example.ruta705.feature.auth.clickableNoRipple
import com.example.ruta705.feature.auth.googleLoginAvailable
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaTextField

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val vm = rutaViewModel { LoginViewModel(it) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current

    AuthScaffold(
        title = "Bienvenido de vuelta",
        subtitle = "Inicia sesión para ver tus rutas, favoritos y reportes",
        onBack = onBack,
    ) {
        RutaTextField(
            value = vm.identifier,
            onValueChange = { vm.identifier = it },
            label = "Correo o celular",
            leadingIcon = Icons.Default.Person,
            keyboardType = KeyboardType.Email,
            error = vm.fieldErrors["identifier"],
            maxLength = 160,
        )
        Spacer(Modifier.height(12.dp))
        RutaTextField(
            value = vm.password,
            onValueChange = { vm.password = it },
            label = "Contraseña",
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = { focus.clearFocus(); vm.submit(onLoggedIn) },
            isPassword = true,
            error = vm.fieldErrors["password"],
            maxLength = 128,
        )
        Text(
            "¿Olvidaste tu contraseña?",
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 6.dp)
                .clickableNoRipple(onForgotPassword)
                .padding(6.dp),
        )
        AnimatedVisibility(vm.error != null) {
            InlineMessage(vm.error.orEmpty(), Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        RutaButton(
            text = "Iniciar sesión",
            onClick = { focus.clearFocus(); vm.submit(onLoggedIn) },
            modifier = Modifier.fillMaxWidth(),
            loading = vm.loading,
        )
        if (googleLoginAvailable) {
            OrDivider()
            GoogleButton(loading = false) { vm.googleSignIn(context, onLoggedIn) }
        }
        AuthFooterLink("¿No tienes cuenta?", "Crear cuenta", onNavigateToRegister)
    }
}
