package com.example.ruta705

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.ruta705.core.LocalAppContainer
import com.example.ruta705.core.navigation.RutaNavGraph
import com.example.ruta705.core.notifications.NotificationSync
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.ui.theme.Ruta705Theme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingRouteId = mutableStateOf<String?>(null)
    private val startReady = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as Ruta705App).container
        splash.setKeepOnScreenCondition { startReady.value == null }
        pendingRouteId.value = intent?.getStringExtra(NotificationSync.EXTRA_ROUTE_ID)
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("ruta705.debug.syncNow", false) == true) NotificationSync.syncNowForDebug(this)

        lifecycleScope.launch {
            container.sessionStore.load()
            val onboarded = container.settings.isOnboardingDone() || container.sessionStore.session != null
            startReady.value = onboarded
            container.meta.load()
        }
        lifecycleScope.launch {
            container.sessionStore.state.collect { state ->
                when (state) {
                    is SessionState.Authenticated -> NotificationSync.schedule(this@MainActivity)
                    SessionState.Guest -> {
                        NotificationSync.cancel(this@MainActivity)
                        container.onSignedOut()
                    }
                    SessionState.Loading -> Unit
                }
            }
        }

        setContent {
            val onboarded by startReady
            val session by container.sessionStore.state.collectAsState()
            Ruta705Theme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    val ready = onboarded ?: return@CompositionLocalProvider
                    val navController = rememberNavController()
                    RutaNavGraph(
                        navController = navController,
                        startAtMain = ready,
                        session = session,
                        sessionExpired = container.apiClient.sessionExpired,
                    )
                    val routeId by pendingRouteId
                    LaunchedEffect(routeId) {
                        routeId?.let {
                            navController.navigate("route/$it")
                            pendingRouteId.value = null
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(NotificationSync.EXTRA_ROUTE_ID)?.let { pendingRouteId.value = it }
        if (BuildConfig.DEBUG && intent.getBooleanExtra("ruta705.debug.syncNow", false)) NotificationSync.syncNowForDebug(this)
    }
}
