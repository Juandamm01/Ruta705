package com.example.ruta705.ui.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.LocalAppContainer
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.network.AppError

/** Estado de una carga remota: cargando, con datos (posiblemente refrescando) o con error recuperable. */
sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T, val refreshing: Boolean = false) : Loadable<T>
    data class Failed(val error: AppError) : Loadable<Nothing>
}

fun <T> ApiResult<T>.toLoadable(): Loadable<T> = when (this) {
    is ApiResult.Success -> Loadable.Ready(value)
    is ApiResult.Failure -> Loadable.Failed(error)
}

val <T> Loadable<T>.valueOrNull: T? get() = (this as? Loadable.Ready<T>)?.value

fun <T> Loadable<T>.refreshing(): Loadable<T> = if (this is Loadable.Ready) copy(refreshing = true) else Loadable.Loading

@Composable
inline fun <reified VM : ViewModel> rutaViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
