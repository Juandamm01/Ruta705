package com.example.ruta705.core.navigation

import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ruta705.core.session.SessionState
import com.example.ruta705.feature.auth.LoginViewModel
import com.example.ruta705.feature.auth.forgot.ForgotPasswordScreen
import com.example.ruta705.feature.auth.login.LoginScreen
import com.example.ruta705.feature.auth.register.RegisterScreen
import com.example.ruta705.feature.main.MainScreen
import com.example.ruta705.feature.onboarding.LandingScreen
import com.example.ruta705.feature.profile.AboutScreen
import com.example.ruta705.feature.profile.ChangePasswordScreen
import com.example.ruta705.feature.profile.EditProfileScreen
import com.example.ruta705.feature.profile.FavoritesScreen
import com.example.ruta705.feature.profile.NotificationSettingsScreen
import com.example.ruta705.feature.profile.NotificationsScreen
import com.example.ruta705.feature.reports.ReportScreen
import com.example.ruta705.feature.routes.RouteDetailScreen
import com.example.ruta705.ui.common.rutaViewModel
import kotlinx.coroutines.flow.SharedFlow
import com.example.ruta705.core.LocalAppContainer
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

object Routes {
    const val Landing = "landing"
    const val Login = "login"
    const val Register = "register"
    const val Forgot = "forgot"
    const val Main = "main"
    const val RouteDetail = "route/{id}"
    const val Report = "report?routeId={routeId}"
    const val Notifications = "notifications"
    const val Favorites = "favorites"
    const val EditProfile = "profile/edit"
    const val ChangePassword = "profile/password"
    const val NotificationSettings = "profile/notifications"
    const val About = "about"

    fun route(id: String) = "route/$id"
    fun report(routeId: String? = null) = if (routeId == null) "report" else "report?routeId=$routeId"
}

/** Acciones de navegación compartidas por las pantallas. */
class AppNavigator(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun login() = nav.navigate(Routes.Login) { launchSingleTop = true }
    fun register() = nav.navigate(Routes.Register) { launchSingleTop = true }
    fun route(id: String) = nav.navigate(Routes.route(id)) { launchSingleTop = true }
    fun report(routeId: String? = null) = nav.navigate(Routes.report(routeId))
    fun notifications() = nav.navigate(Routes.Notifications) { launchSingleTop = true }
    fun favorites() = nav.navigate(Routes.Favorites) { launchSingleTop = true }
    fun editProfile() = nav.navigate(Routes.EditProfile)
    fun changePassword() = nav.navigate(Routes.ChangePassword)
    fun notificationSettings() = nav.navigate(Routes.NotificationSettings)
    fun about() = nav.navigate(Routes.About)

    /** Tras iniciar sesión o registrarse: vuelve al inicio limpiando las pantallas de cuenta. */
    fun enterApp() = nav.navigate(Routes.Main) {
        popUpTo(nav.graph.id) { inclusive = true }
        launchSingleTop = true
    }

    fun signedOut() = nav.navigate(Routes.Main) {
        popUpTo(nav.graph.id) { inclusive = true }
    }
}

private const val ANIM_MS = 260

@Composable
fun RutaNavGraph(
    navController: NavHostController,
    startAtMain: Boolean,
    session: SessionState,
    sessionExpired: SharedFlow<Unit>,
) {
    val navigator = AppNavigator(navController)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        sessionExpired.collect {
            Toast.makeText(context, "Tu sesión expiró. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (startAtMain) Routes.Main else Routes.Landing,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_MS)) + fadeIn(tween(ANIM_MS)) },
        exitTransition = { fadeOut(tween(ANIM_MS / 2)) },
        popEnterTransition = { fadeIn(tween(ANIM_MS)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_MS)) + fadeOut(tween(ANIM_MS)) },
    ) {
        composable(Routes.Landing) {
            val loginVm = rutaViewModel { LoginViewModel(it) }
            val container = LocalAppContainer.current
            val scope = rememberCoroutineScope()
            LandingScreen(
                onCreateAccount = navigator::register,
                onLogin = navigator::login,
                onExplore = { scope.launch { container.auth.continueAsGuest(); navigator.enterApp() } },
                onGoogleLogin = { loginVm.googleSignIn(context, navigator::enterApp) },
            )
        }
        composable(Routes.Login) {
            LoginScreen(
                onBack = navigator::back,
                onLoggedIn = navigator::enterApp,
                onForgotPassword = { navController.navigate(Routes.Forgot) },
                onNavigateToRegister = { navController.navigate(Routes.Register) { popUpTo(Routes.Login) { inclusive = true } } },
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                onBack = navigator::back,
                onRegistered = navigator::enterApp,
                onNavigateToLogin = { navController.navigate(Routes.Login) { popUpTo(Routes.Register) { inclusive = true } } },
            )
        }
        composable(Routes.Forgot) {
            ForgotPasswordScreen(
                onBack = navigator::back,
                onGoToLogin = { navController.navigate(Routes.Login) { popUpTo(Routes.Forgot) { inclusive = true } } },
            )
        }
        composable(Routes.Main) {
            MainScreen(navigator = navigator, session = session)
        }
        composable(Routes.RouteDetail, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            RouteDetailScreen(routeId = entry.arguments?.getString("id").orEmpty(), navigator = navigator, session = session)
        }
        composable(
            Routes.Report,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            ReportScreen(routeId = entry.arguments?.getString("routeId"), navigator = navigator, session = session)
        }
        composable(Routes.Notifications) { NotificationsScreen(navigator) }
        composable(Routes.Favorites) { FavoritesScreen(navigator) }
        composable(Routes.EditProfile) { EditProfileScreen(navigator) }
        composable(Routes.ChangePassword) { ChangePasswordScreen(navigator) }
        composable(Routes.NotificationSettings) { NotificationSettingsScreen(navigator) }
        composable(Routes.About) { AboutScreen(navigator) }
    }
}
