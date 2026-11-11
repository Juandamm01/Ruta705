package com.example.ruta705

import android.app.Application
import com.example.ruta705.core.AppContainer
import com.example.ruta705.core.notifications.NotificationSync
import org.osmdroid.config.Configuration
import java.io.File

class Ruta705App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationSync.createChannel(this)
        Configuration.getInstance().apply {
            // La política de uso de los mapas de OpenStreetMap exige identificar la app.
            userAgentValue = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
            tileFileSystemCacheMaxBytes = 60L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 45L * 1024 * 1024
        }
    }
}
