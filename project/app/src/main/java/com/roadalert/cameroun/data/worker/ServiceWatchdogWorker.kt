package com.roadalert.cameroun.data.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.roadalert.cameroun.R
import com.roadalert.cameroun.RoadAlertApplication
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.detection.AccidentDetectionService
import com.roadalert.cameroun.util.AppSettings
import java.util.concurrent.TimeUnit

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        fun schedule(context: Context) {
            val workRequest =
                PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
                    15, TimeUnit.MINUTES
                ).build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "service_watchdog",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        val db = AppDatabase.getInstance(context)
        val userProfileRepository = UserProfileRepository(
            db,
            db.userDAO(),
            db.emergencyContactDAO()
        )

        val profileComplete = userProfileRepository.isProfileComplete()
        if (!profileComplete) return Result.success()

        val appSettings = AppSettings(context)
        if (appSettings.isServiceAlive()) return Result.success()

        // Service mort → redémarrer
        val serviceIntent = Intent(
            context,
            AccidentDetectionService::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Notification silencieuse
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager

        val notification = NotificationCompat.Builder(
            context,
            RoadAlertApplication.CHANNEL_ID_SERVICE
        )
            .setContentTitle(context.getString(R.string.notification_service_title))
            .setContentText(
                "RoadAlert a réactivé votre protection automatiquement"
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(
            RoadAlertApplication.NOTIFICATION_ID_WATCHDOG,
            notification
        )

        return Result.success()
    }
}
