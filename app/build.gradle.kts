import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Configuración por entorno sin secretos en el código: se lee de local.properties (no versionado)
 * y, si no existe allí, de gradle.properties o de variables de entorno.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun config(key: String, default: String = ""): String =
    localProperties.getProperty(key)
        ?: (project.findProperty(key) as String?)
        ?: System.getenv(key.uppercase().replace('.', '_'))
        ?: default

val apiBaseUrl = config("ruta705.apiBaseUrl", "http://10.0.2.2:3000/api/v1/").let { if (it.endsWith("/")) it else "$it/" }
val releaseApiBaseUrl = config("ruta705.releaseApiBaseUrl", "")
val googleWebClientId = config("ruta705.googleWebClientId", "")
val mapTileUrl = config("ruta705.mapTileUrl", "")

android {
    namespace = "com.example.ruta705"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ruta705"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "MAP_TILE_URL", "\"$mapTileUrl\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // En release solo se permite HTTPS. Si no se configura, la app lo indicará al iniciar.
            val releaseUrl = releaseApiBaseUrl.takeIf { it.startsWith("https://") } ?: ""
            buildConfigField("String", "API_BASE_URL", "\"$releaseUrl\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Datos y red
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    // Notificaciones locales a partir de la bandeja del backend
    implementation(libs.androidx.work.runtime.ktx)

    // Mapas OpenStreetMap (sin API key)
    implementation(libs.osmdroid)

    // Inicio de sesión con Google (solo se activa si se configura ruta705.googleWebClientId)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
