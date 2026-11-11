package com.example.ruta705.core.network

import com.example.ruta705.BuildConfig
import com.example.ruta705.core.session.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP central: URL según el entorno (BuildConfig), token Bearer automático,
 * renovación transparente del access token y cierre de sesión si la renovación falla.
 */
class ApiClient(private val sessionStore: SessionStore, json: Json) {

    /** Emite cuando la sesión expiró definitivamente y el usuario debe volver a iniciar sesión. */
    val sessionExpired: SharedFlow<Unit> get() = _sessionExpired
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val baseUrl: String = BuildConfig.API_BASE_URL
    val isConfigured: Boolean = baseUrl.startsWith("http")

    private val refreshLock = Any()
    private val converter = json.asConverterFactory("application/json".toMediaType())

    private val baseHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "Ruta705-Android/${BuildConfig.VERSION_NAME}")
                    .build()
            )
        })
        .apply {
            if (BuildConfig.DEBUG) {
                // Solo encabezados básicos: nunca se registran cuerpos (contraseñas/tokens).
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                })
            }
        }
        .build()

    /** Cliente sin autenticador, usado únicamente para renovar tokens (evita recursión). */
    private val refreshApi: Ruta705Api = retrofit(baseHttp).create(Ruta705Api::class.java)

    private val authedHttp: OkHttpClient = baseHttp.newBuilder()
        .addInterceptor(Interceptor { chain ->
            val token = sessionStore.session?.tokens?.accessToken
            val request = chain.request()
            chain.proceed(if (token != null && request.header("Authorization") == null) request.withToken(token) else request)
        })
        .authenticator(TokenAuthenticator())
        .build()

    val api: Ruta705Api = retrofit(authedHttp).create(Ruta705Api::class.java)

    private fun retrofit(client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(if (isConfigured) baseUrl else "http://invalid.localhost/")
        .client(client)
        .addConverterFactory(converter)
        .build()

    private fun Request.withToken(token: String) = newBuilder().header("Authorization", "Bearer $token").build()

    private inner class TokenAuthenticator : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
            if (response.request.url.encodedPath.contains("/auth/")) return null
            if (responseCount(response) > 1) return null
            val code = runCatching { response.peekBody(2048).string() }.getOrNull()
                ?.let { parseErrorBody(401, it).code }
            if (code != "TOKEN_EXPIRED" && code != "TOKEN_INVALID") return null

            synchronized(refreshLock) {
                val current = sessionStore.session ?: return null
                // Otro hilo ya renovó el token mientras esperábamos.
                if (current.tokens.accessToken != sentToken) return response.request.withToken(current.tokens.accessToken)

                val refreshed = runBlocking {
                    apiCall { refreshApi.refresh(RefreshRequest(current.tokens.refreshToken)).data }
                }
                return when (refreshed) {
                    is ApiResult.Success -> {
                        runBlocking { sessionStore.save(refreshed.value) }
                        response.request.withToken(refreshed.value.tokens.accessToken)
                    }
                    is ApiResult.Failure -> {
                        if (refreshed.error.kind == AppError.Kind.Unauthorized) {
                            runBlocking { sessionStore.clear() }
                            _sessionExpired.tryEmit(Unit)
                        }
                        null
                    }
                }
            }
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }
}
