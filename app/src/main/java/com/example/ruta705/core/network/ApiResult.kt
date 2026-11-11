package com.example.ruta705.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Error ya traducido a un mensaje comprensible para el usuario. */
data class AppError(
    val kind: Kind,
    val message: String,
    val code: String? = null,
    val status: Int? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
) {
    enum class Kind { Network, Timeout, Unauthorized, Validation, NotFound, Conflict, RateLimited, Unavailable, Server, Unexpected }

    val needsLogin: Boolean get() = kind == Kind.Unauthorized
    val isOffline: Boolean get() = kind == Kind.Network || kind == Kind.Timeout
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> = also { if (it is ApiResult.Success) block(it.value) }

inline fun <T> ApiResult<T>.onFailure(block: (AppError) -> Unit): ApiResult<T> = also { if (it is ApiResult.Failure) block(it.error) }

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.value

private val errorJson = Json { ignoreUnknownKeys = true }

fun parseErrorBody(status: Int, raw: String?): AppError {
    val body = raw?.let { runCatching { errorJson.decodeFromString(ApiErrorBody.serializer(), it) }.getOrNull() }
    val kind = when (status) {
        400, 422 -> AppError.Kind.Validation
        401 -> AppError.Kind.Unauthorized
        403 -> AppError.Kind.Validation
        404 -> AppError.Kind.NotFound
        409 -> AppError.Kind.Conflict
        429 -> AppError.Kind.RateLimited
        503 -> AppError.Kind.Unavailable
        in 500..599 -> AppError.Kind.Server
        else -> AppError.Kind.Unexpected
    }
    val fallback = when (kind) {
        AppError.Kind.Unauthorized -> "Inicia sesión para continuar."
        AppError.Kind.NotFound -> "No encontramos lo que buscas."
        AppError.Kind.RateLimited -> "Hiciste muchas solicitudes seguidas. Espera un momento."
        AppError.Kind.Server, AppError.Kind.Unavailable -> "El servicio no está disponible en este momento. Intenta de nuevo en unos minutos."
        else -> "No se pudo completar la solicitud."
    }
    return AppError(
        kind = kind,
        message = body?.error?.message ?: fallback,
        code = body?.error?.code,
        status = status,
        fieldErrors = body?.error?.details.orEmpty()
            .filter { !it.path.isNullOrBlank() }
            .associate { it.path!!.substringBefore('.') to it.message },
    )
}

fun Throwable.toAppError(): AppError = when (this) {
    is HttpException -> parseErrorBody(code(), response()?.errorBody()?.string())
    is SocketTimeoutException -> AppError(AppError.Kind.Timeout, "La conexión está muy lenta. Revisa tu señal e intenta de nuevo.")
    is UnknownHostException, is ConnectException ->
        AppError(AppError.Kind.Network, "No hay conexión con el servidor. Revisa tu internet e intenta de nuevo.")
    is IOException -> AppError(AppError.Kind.Network, "Se perdió la conexión. Revisa tu internet e intenta de nuevo.")
    is SerializationException, is IllegalArgumentException ->
        AppError(AppError.Kind.Unexpected, "Recibimos una respuesta inesperada del servidor. Actualiza la app o intenta más tarde.")
    else -> AppError(AppError.Kind.Unexpected, "Ocurrió un error inesperado. Intenta de nuevo.")
}

/** Ejecuta una llamada de red y convierte cualquier fallo en [ApiResult.Failure] sin lanzar excepciones. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (error: Throwable) {
    ApiResult.Failure(error.toAppError())
}

/** Variante para endpoints sin cuerpo (204). */
suspend fun apiCallUnit(block: suspend () -> Response<Unit>): ApiResult<Unit> = try {
    val response = block()
    if (response.isSuccessful) ApiResult.Success(Unit)
    else ApiResult.Failure(parseErrorBody(response.code(), response.errorBody()?.string()))
} catch (cancel: CancellationException) {
    throw cancel
} catch (error: Throwable) {
    ApiResult.Failure(error.toAppError())
}
