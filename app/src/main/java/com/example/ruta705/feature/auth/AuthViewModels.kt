package com.example.ruta705.feature.auth

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ruta705.BuildConfig
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppError
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
private val PHONE_REGEX = Regex("^3\\d{9}$")

fun normalizePhone(raw: String) = raw.replace(Regex("[\\s\\-()]"), "").removePrefix("+57").removePrefix("57").let {
    if (it.length == 12 && it.startsWith("57")) it.drop(2) else it
}

fun passwordProblems(password: String): List<String> = buildList {
    if (password.length < 8) add("Al menos 8 caracteres")
    if (!password.any { it.isLetter() }) add("Al menos una letra")
    if (!password.any { it.isDigit() }) add("Al menos un número")
}

val googleLoginAvailable: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

/** Obtiene un ID token de Google con Credential Manager. Devuelve null si el usuario cancela. */
suspend fun requestGoogleIdToken(context: Context): Result<String?> = runCatching {
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setFilterByAuthorizedAccounts(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            error("Credencial no soportada")
        }
    } catch (cancel: GetCredentialCancellationException) {
        null
    }
}

abstract class FormViewModel(protected val container: AppContainer) : ViewModel() {
    var loading by mutableStateOf(false)
        protected set
    var error by mutableStateOf<String?>(null)
        protected set
    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        protected set

    protected fun fail(appError: AppError) {
        error = appError.message.takeIf { appError.fieldErrors.isEmpty() } ?: "Revisa los campos marcados."
        fieldErrors = appError.fieldErrors
    }

    fun googleSignIn(context: Context, onDone: () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            val token = requestGoogleIdToken(context)
            token.onFailure { error = "No se pudo usar tu cuenta de Google en este teléfono." }
            token.getOrNull()?.let { idToken ->
                when (val result = container.auth.loginWithGoogle(idToken)) {
                    is ApiResult.Success -> onDone()
                    is ApiResult.Failure -> fail(result.error)
                }
            }
            loading = false
        }
    }
}

class LoginViewModel(container: AppContainer) : FormViewModel(container) {
    var identifier by mutableStateOf("")
    var password by mutableStateOf("")

    fun submit(onSuccess: () -> Unit) {
        if (loading) return
        val errors = buildMap {
            if (identifier.isBlank()) put("identifier", "Escribe tu correo o celular")
            if (password.isBlank()) put("password", "Escribe tu contraseña")
        }
        fieldErrors = errors
        error = null
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            loading = true
            when (val result = container.auth.login(identifier, password)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Failure -> fail(result.error)
            }
            loading = false
        }
    }
}

class RegisterViewModel(container: AppContainer) : FormViewModel(container) {
    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var password by mutableStateOf("")
    var confirm by mutableStateOf("")

    fun submit(onSuccess: () -> Unit) {
        if (loading) return
        val normalizedPhone = normalizePhone(phone)
        val errors = buildMap {
            if (fullName.trim().length < 2) put("fullName", "Escribe tu nombre")
            if (!EMAIL_REGEX.matches(email.trim())) put("email", "Escribe un correo válido")
            if (phone.isNotBlank() && !PHONE_REGEX.matches(normalizedPhone)) put("phone", "Celular colombiano de 10 dígitos (inicia en 3)")
            passwordProblems(password).takeIf { it.isNotEmpty() }?.let { put("password", it.joinToString(" · ")) }
            if (confirm != password) put("confirm", "Las contraseñas no coinciden")
        }
        fieldErrors = errors
        error = null
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            loading = true
            when (val result = container.auth.register(fullName, email, normalizedPhone.ifBlank { null }, password)) {
                is ApiResult.Success -> onSuccess()
                is ApiResult.Failure -> fail(result.error)
            }
            loading = false
        }
    }
}

class ForgotPasswordViewModel(container: AppContainer) : FormViewModel(container) {
    var email by mutableStateOf("")
    var code by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var codeSent by mutableStateOf(false)
        private set
    var info by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf(false)
        private set

    fun requestCode() {
        if (loading) return
        if (!EMAIL_REGEX.matches(email.trim())) {
            fieldErrors = mapOf("email" to "Escribe un correo válido")
            return
        }
        fieldErrors = emptyMap()
        error = null
        viewModelScope.launch {
            loading = true
            when (val result = container.auth.forgotPassword(email)) {
                is ApiResult.Success -> {
                    codeSent = true
                    info = result.value
                }
                is ApiResult.Failure -> fail(result.error)
            }
            loading = false
        }
    }

    fun reset() {
        if (loading) return
        val errors = buildMap {
            if (!Regex("^\\d{6}$").matches(code.trim())) put("code", "El código tiene 6 dígitos")
            passwordProblems(newPassword).takeIf { it.isNotEmpty() }?.let { put("newPassword", it.joinToString(" · ")) }
        }
        fieldErrors = errors
        error = null
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            loading = true
            when (val result = container.auth.resetPassword(email, code, newPassword)) {
                is ApiResult.Success -> done = true
                is ApiResult.Failure -> fail(result.error)
            }
            loading = false
        }
    }
}
