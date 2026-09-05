package com.example.ruta705.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ruta705.feature.auth.login.LoginScreen
import com.example.ruta705.feature.auth.register.RegisterScreen
import com.example.ruta705.feature.onboarding.LandingScreen

object Routes {
    const val Landing = "landing"
    const val Login = "login"
    const val Register = "register"
    const val Home = "home"
}

@Composable
fun RutaNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Landing
    ) {
        composable(route = Routes.Landing) {
            LandingScreen(
                onLogin = { navController.navigate(Routes.Login) },
                onCreateAccount = { navController.navigate(Routes.Register) },
                onGoogleLogin = { /* TODO: conectar autenticación con Google */ }
            )
        }
        composable(route = Routes.Login) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginClick = { _, _ -> /* TODO: conectar con backend */ },
                onGoogleLogin = { /* TODO: conectar autenticación con Google */ },
                onForgotPassword = { /* TODO: conectar recuperación de contraseña */ },
                onNavigateToRegister = { navController.navigate(Routes.Register) }
            )
        }
        composable(route = Routes.Register) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterClick = { _, _, _, _ -> /* TODO: conectar con backend */ },
                onGoogleRegister = { /* TODO: conectar autenticación con Google */ },
                onNavigateToLogin = { navController.navigate(Routes.Login) }
            )
        }
    }
}
