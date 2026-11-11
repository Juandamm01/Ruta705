package com.example.ruta705.feature.auth.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ruta705.feature.auth.AuthFooterLink
import com.example.ruta705.feature.auth.AuthScaffold
import com.example.ruta705.feature.auth.GoogleButton
import com.example.ruta705.feature.auth.OrDivider
import com.example.ruta705.feature.auth.RegisterViewModel
import com.example.ruta705.feature.auth.googleLoginAvailable
import com.example.ruta705.feature.auth.passwordProblems
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaTextField
import com.example.ruta705.ui.theme.SuccessGreen

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val vm = rutaViewModel { RegisterViewModel(it) }
    val focus = LocalFocusManager.current
    val context = LocalContext.current

    AuthScaffold(
        title = "Crea tu cuenta",
        subtitle = "Guarda tus rutas, recibe novedades y ayuda a la comunidad con tus reportes",
        onBack = onBack,
    ) {
        RutaTextField(vm.fullName, { vm.fullName = it }, "Nombre", leadingIcon = Icons.Default.Badge, error = vm.fieldErrors["fullName"], maxLength = 80)
        Spacer(Modifier.height(10.dp))
        RutaTextField(
            vm.email, { vm.email = it }, "Correo electrónico",
            leadingIcon = Icons.Default.Email, keyboardType = KeyboardType.Email, error = vm.fieldErrors["email"], maxLength = 160,
        )
        Spacer(Modifier.height(10.dp))
        RutaTextField(
            vm.phone, { vm.phone = it }, "Celular (opcional)",
            placeholder = "300 123 4567",
            leadingIcon = Icons.Default.PhoneAndroid, keyboardType = KeyboardType.Phone,
            error = vm.fieldErrors["phone"], supporting = "Te permitirá iniciar sesión con tu número", maxLength = 16,
        )
        Spacer(Modifier.height(10.dp))
        RutaTextField(
            vm.password, { vm.password = it }, "Contraseña",
            leadingIcon = Icons.Default.Lock, keyboardType = KeyboardType.Password, isPassword = true,
            error = vm.fieldErrors["password"], maxLength = 128,
        )
        PasswordChecklist(vm.password)
        Spacer(Modifier.height(10.dp))
        RutaTextField(
            vm.confirm, { vm.confirm = it }, "Confirmar contraseña",
            leadingIcon = Icons.Default.Lock, keyboardType = KeyboardType.Password, isPassword = true,
            imeAction = ImeAction.Done, onImeAction = { focus.clearFocus(); vm.submit(onRegistered) },
            error = vm.fieldErrors["confirm"], maxLength = 128,
        )
        AnimatedVisibility(vm.error != null) { InlineMessage(vm.error.orEmpty(), Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        RutaButton("Crear cuenta", { focus.clearFocus(); vm.submit(onRegistered) }, Modifier.fillMaxWidth(), loading = vm.loading)
        Text(
            "Tu nombre se mostrará abreviado (por ejemplo, \"Ana R.\") en los reportes que publiques.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (googleLoginAvailable) {
            OrDivider()
            GoogleButton(loading = false) { vm.googleSignIn(context, onRegistered) }
        }
        AuthFooterLink("¿Ya tienes cuenta?", "Inicia sesión", onNavigateToLogin)
    }
}

@Composable
private fun PasswordChecklist(password: String) {
    if (password.isEmpty()) return
    val problems = passwordProblems(password)
    Column(Modifier.padding(top = 6.dp, start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("Al menos 8 caracteres", "Al menos una letra", "Al menos un número").forEach { rule ->
            val ok = rule !in problems
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                    tint = if (ok) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(rule, style = MaterialTheme.typography.bodySmall, color = if (ok) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
