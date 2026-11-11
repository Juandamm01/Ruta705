# Ruta705 — App Android

App ciudadana de transporte público para **Villavicencio, Meta**: qué buseta tomar, recorridos,
paraderos, frecuencias estimadas y reportes de la comunidad (vi pasar la ruta, retrasos, desvíos…).
No rastrea busetas por GPS: combina información de rutas cargada y verificada con reportes
colaborativos que siempre muestran su hora, vigencia y confiabilidad.

- **Stack:** Kotlin 2.1 · Jetpack Compose (BOM 2025.06, Material 3) · Navigation Compose ·
  Retrofit + OkHttp + kotlinx.serialization · DataStore + Android Keystore · WorkManager · osmdroid (OpenStreetMap).
- **Toolchain:** Gradle 8.11.1 (wrapper) · Android Gradle Plugin 8.10.1 · JDK 17 · compileSdk 36 · minSdk 26.
- **Backend:** proyecto `C:\Documentos\Ruta705_Backend\backend` (Bun + NestJS). Ver su README.

## Ejecutar

1. Inicia el backend (`bun run dev` en el proyecto del backend; opcional `bun run seed:demo`).
2. Abre esta carpeta en Android Studio, o desde terminal:

```bash
./gradlew :app:installDebug
```

### Conectar el teléfono con el backend

La URL se define sin tocar código, en `local.properties` (no versionado):

| Dónde corre la app | `ruta705.apiBaseUrl` |
| --- | --- |
| Emulador | `http://10.0.2.2:3000/api/v1/` (por defecto) |
| Teléfono por USB | ejecutar `adb reverse tcp:3000 tcp:3000` y usar `http://127.0.0.1:3000/api/v1/` |
| Teléfono por Wi-Fi | `http://<IP-del-PC>:3000/api/v1/` (misma red; abrir el puerto 3000 en el firewall) |

`localhost` dentro del teléfono es el propio teléfono, por eso se usan estas direcciones.
Las compilaciones **debug** permiten HTTP (`src/debug/res/xml/network_security_config.xml`);
las **release** solo HTTPS y toman la URL de `ruta705.releaseApiBaseUrl`.

### Configuración opcional (`local.properties`)

| Clave | Para qué |
| --- | --- |
| `ruta705.apiBaseUrl` | URL del backend en debug |
| `ruta705.releaseApiBaseUrl` | URL HTTPS del backend en release |
| `ruta705.googleWebClientId` | Activa "Continuar con Google" (ID de cliente OAuth *Web*; el backend debe tener el mismo en `GOOGLE_CLIENT_IDS`). Si está vacío, el botón no aparece. |
| `ruta705.mapTileUrl` | Teselas de mapa propias para producción (plantilla `https://…/{z}/{x}/{y}.png`, p. ej. MapTiler/Stadia con su clave). Vacío = OpenStreetMap estándar, apto solo para uso moderado. |

### Nota sobre certificados en este equipo

En este PC, Java no confía en el certificado HTTPS que intercepta la red (antivirus/proxy) y Gradle
no podía descargar dependencias. Se resolvió **solo para la sesión de terminal** con:

```bash
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

Android Studio (JBR) normalmente no lo necesita.

## Estructura

```
app/src/main/java/com/example/ruta705/
  Ruta705App.kt, MainActivity.kt   arranque, splash, sesión, enlace desde notificaciones
  core/
    AppContainer.kt                dependencias (inyección manual)
    network/                       API Retrofit, modelos, errores en español, renovación de token
    session/                       sesión cifrada (Keystore AES-GCM) y preferencias locales
    data/Repositories.kt           repositorios con caché y deduplicación
    location/                      ubicación puntual bajo demanda (sin Play Services)
    notifications/                 WorkManager + notificaciones locales con eventos del backend
    navigation/NavGraph.kt         rutas y transiciones
  ui/theme, ui/components          sistema de diseño (paleta original, tipografía, estados, mapa)
  feature/
    onboarding, auth (login, registro, recuperación)
    main (barra inferior), home, search (planificador), routes (listado y detalle)
    reports (crear reporte), profile (perfil, favoritos, avisos, cuenta, acerca de)
```

## Pruebas

```bash
./gradlew :app:testDebugUnitTest      # lógica: teléfonos, contraseñas, formatos, errores, horas de silencio
./gradlew :app:assembleDebug
```
