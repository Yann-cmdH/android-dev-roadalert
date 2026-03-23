package com.roadalert.cameroun

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.roadalert.cameroun.data.worker.ServiceWatchdogWorker

class RoadAlertApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
        ServiceWatchdogWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // ── Canal 1 — Service de detection permanent ──────
        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            "Protection RoadAlert",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description =
                "Indique que la detection d'accidents est active"
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        manager.createNotificationChannel(serviceChannel)

        // ── Canal 2 — Alertes d'accident ──────────────────
        val alertChannel = NotificationChannel(
            CHANNEL_ID_ALERT,
            "Alertes d'accident",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "Notifications lors de la detection d'un accident"
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(alertChannel)

        // ── Canal 3 — Countdown ───────────────────────────
        val countdownChannel = NotificationChannel(
            CHANNEL_ID_COUNTDOWN,
            "Compte a rebours",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "Notification pendant le compte a rebours avant alerte"
            enableVibration(true)
        }
        manager.createNotificationChannel(countdownChannel)
    }

    companion object {
        const val CHANNEL_ID_SERVICE =
            "roadalert_service_channel"
        const val CHANNEL_ID_ALERT =
            "roadalert_alert_channel"
        const val CHANNEL_ID_COUNTDOWN =
            "roadalert_countdown_channel"
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_COUNTDOWN = 1002
        const val NOTIFICATION_ID_ALERT = 1003
        const val NOTIFICATION_ID_WATCHDOG = 1004
    }
}