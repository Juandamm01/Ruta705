package com.example.ruta705.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ruta705.MainActivity
import com.example.ruta705.R
import com.example.ruta705.Ruta705App
import com.example.ruta705.core.network.ApiResult
import com.example.ruta705.core.session.SessionState
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Notificaciones respaldadas por eventos reales del backend: cada ~15 minutos (mínimo de Android)
 * se consulta la bandeja del usuario y se muestran las novedades nuevas como notificaciones locales.
 * No requiere Firebase. La entrega push inmediata (FCM) está documentada como siguiente paso.
 */
object NotificationSync {
    const val CHANNEL_ID = "route_updates"
    const val EXTRA_ROUTE_ID = "ruta705.routeId"
    private const val WORK_NAME = "ruta705-notification-sync"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_route_updates),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_route_updates_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Solo depuración: sincroniza de inmediato (adb shell am start -n …/.MainActivity --ez ruta705.debug.syncNow true). */
    fun syncNowForDebug(context: Context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<NotificationWorker>().build())
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        NotificationManagerCompat.from(context).cancelAll()
    }

    fun isQuiet(quiet: Pair<String, String>?, now: LocalTime = LocalTime.now(ZoneId.of("America/Bogota"))): Boolean {
        if (quiet == null) return false
        val start = runCatching { LocalTime.parse(quiet.first) }.getOrNull() ?: return false
        val end = runCatching { LocalTime.parse(quiet.second) }.getOrNull() ?: return false
        return if (start <= end) now in start..end else now >= start || now <= end
    }
}

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as Ruta705App).container
        // Si Android inició el proceso solo para este trabajo, la sesión aún no se ha leído del disco.
        if (container.sessionStore.state.value is SessionState.Loading) container.sessionStore.load()
        if (container.sessionStore.session == null) return Result.success()

        val page = when (val result = container.notifications.list()) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return if (result.error.isOffline) Result.retry() else Result.success()
        }
        container.notifications.refreshUnread()
        val lastSeen = container.settings.lastNotifiedId()
        val fresh = page.data.filter { !it.read && (lastSeen == null || it.id > lastSeen) }
        page.data.maxOfOrNull { it.id }?.let { container.settings.setLastNotifiedId(maxOf(it, lastSeen ?: "")) }
        // En la primera sincronización no se muestran avisos viejos de golpe.
        if (lastSeen == null || fresh.isEmpty()) return Result.success()
        if (!NotificationSync.canPost(applicationContext) || NotificationSync.isQuiet(container.settings.quietHours())) {
            return Result.success()
        }

        fresh.take(3).forEach { item ->
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                item.routeId?.let { putExtra(NotificationSync.EXTRA_ROUTE_ID, it) }
            }
            val pending = PendingIntent.getActivity(
                applicationContext,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(applicationContext, NotificationSync.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ruta705)
                .setColor(0xFF0D3B36.toInt())
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()
            runCatching { NotificationManagerCompat.from(applicationContext).notify(item.id.hashCode(), notification) }
        }
        return Result.success()
    }
}
