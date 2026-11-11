package com.example.ruta705.feature.auth.forgot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ruta705.feature.auth.AuthScaffold
import com.example.ruta705.feature.auth.ForgotPasswordViewModel
import com.example.ruta705.ui.common.rutaViewModel
import com.example.ruta705.ui.components.ButtonStyle
import com.example.ruta705.ui.components.EmptyState
import com.example.ruta705.ui.components.InlineMessage
import com.example.ruta705.ui.components.RutaButton
import com.example.ruta705.ui.components.RutaTextField

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onGoToLogin: () -> Unit) {
    val vm = rutaViewModel { ForgotPasswordViewModel(it) }
    AuthScaffold(
        title = "Recupera tu acceso",
        subtitle = "Te enviaremos un código de 6 dígitos a tu correo",
        onBack = onBack,
    ) {
        AnimatedContent(targetState = when { vm.done -> 2; vm.codeSent -> 1; else -> 0 }, label = "forgot-step") { step ->
            Column {
                when (step) {
                    0 -> {
                        RutaTextField(
                            vm.email, { vm.email = it }, "Correo de tu cuenta",
                            leadingIcon = Icons.Default.Email, keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done, onImeAction = vm::requestCode, error = vm.fieldErrors["email"],
                        )
                        AnimatedVisibility(vm.error != null) { InlineMessage(vm.error.orEmpty(), Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(16.dp))
                        RutaButton("Enviar código", vm::requestCode, Modifier.fillMaxWidth(), loading = vm.loading)
                    }
                    1 -> {
                        vm.info?.let { InlineMessage(it, isError = false) }
                        Spacer(Modifier.height(12.dp))
                        RutaTextField(
                            vm.code, { vm.code = it.filter(Char::isDigit) }, "Código de 6 dígitos",
                            leadingIcon = Icons.Default.Pin, keyboardType = KeyboardType.NumberPassword,
                            error = vm.fieldErrors["code"], maxLength = 6,
                        )
                        Spacer(Modifier.height(10.dp))
                        RutaTextField(
                            vm.newPassword, { vm.newPassword = it }, "Nueva contraseña",
                            leadingIcon = Icons.Default.Lock, keyboardType = KeyboardType.Password, isPassword = true,
                            imeAction = ImeAction.Done, onImeAction = vm::reset, error = vm.fieldErrors["newPassword"],
                            supporting = "Mínimo 8 caracteres, con letras y números",
                        )
                        AnimatedVisibility(vm.error != null) { InlineMessage(vm.error.orEmpty(), Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(16.dp))
                        RutaButton("Cambiar contraseña", vm::reset, Modifier.fillMaxWidth(), loading = vm.loading)
                        Spacer(Modifier.height(8.dp))
                        RutaButton("Reenviar código", vm::requestCode, Modifier.fillMaxWidth(), style = ButtonStyle.Outline, enabled = !vm.loading)
                        Text(
                            "Revisa también la carpeta de spam. El código vence en 15 minutos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    else -> {
                        EmptyState(
                            Icons.Default.CheckCircle,
                            "Contraseña actualizada",
                            "Por seguridad cerramos tus otras sesiones. Inicia sesión con tu nueva contraseña.",
                            actionLabel = "Ir a iniciar sesión",
                            onAction = onGoToLogin,
                        )
                    }
                }
            }
        }
    }
}
