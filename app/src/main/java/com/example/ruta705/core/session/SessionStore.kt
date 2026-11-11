package com.example.ruta705.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ruta705.core.network.AuthSession
import com.example.ruta705.core.network.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.sessionDataStore by preferencesDataStore(name = "session")

sealed interface SessionState {
    data object Loading : SessionState
    data object Guest : SessionState
    data class Authenticated(val user: User) : SessionState
}

/**
 * Cifra los tokens con una clave AES-GCM que vive en Android Keystore (no extraíble).
 * Si la clave se pierde (restauración en otro equipo, borrado de credenciales), la sesión se descarta.
 */
private object TokenCipher {
    private const val KEY_ALIAS = "ruta705_session_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }
}

class SessionStore(private val context: Context, private val json: Json) {
    private val sessionKey = stringPreferencesKey("session_v1")

    private val current = MutableStateFlow<AuthSession?>(null)
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** Acceso síncrono para el interceptor HTTP (se lee en hilos de OkHttp). */
    val session: AuthSession? get() = current.value

    suspend fun load() {
        val stored = context.sessionDataStore.data.first()[sessionKey]
        val session = stored?.let {
            runCatching { json.decodeFromString(AuthSession.serializer(), TokenCipher.decrypt(it)) }
                .onFailure { error -> Log.w("SessionStore", "No se pudo leer la sesión guardada: ${error.javaClass.simpleName}") }
                .getOrNull()
        }
        if (stored != null && session == null) clearStorage()
        publish(session)
    }

    suspend fun save(session: AuthSession) {
        val encrypted = TokenCipher.encrypt(json.encodeToString(AuthSession.serializer(), session))
        context.sessionDataStore.edit { it[sessionKey] = encrypted }
        publish(session)
    }

    suspend fun updateUser(user: User) {
        current.value?.let { save(it.copy(user = user)) }
    }

    suspend fun clear() {
        clearStorage()
        publish(null)
    }

    private suspend fun clearStorage() {
        context.sessionDataStore.edit { it.remove(sessionKey) }
    }

    private fun publish(session: AuthSession?) {
        current.value = session
        _state.value = session?.let { SessionState.Authenticated(it.user) } ?: SessionState.Guest
    }

    @Suppress("unused")
    private fun Preferences.debugKeys() = asMap().keys.joinToString()
}
