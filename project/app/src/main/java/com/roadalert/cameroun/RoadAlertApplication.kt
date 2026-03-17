package com.roadalert.cameroun

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class RoadAlertApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        // ── Canal 1 — Service de détection permanent ──────────
        val serviceChannel = NotificationChannel(
            CHANNEL_ID_SERVICE,
            "Protection RoadAlert",
            NotificationManager.IMPORTANCE_LOW
        )
        serviceChannel.description =
            "Indique que la détection d'accidents est active"
        serviceChannel.setShowBadge(false)
        serviceChannel.enableVibration(false)
        serviceChannel.enableLights(false)
        manager.createNotificationChannel(serviceChannel)

        // ── Canal 2 — Alertes d'accident ──────────────────────
        val alertChannel = NotificationChannel(
            CHANNEL_ID_ALERT,
            "Alertes d'accident",
            NotificationManager.IMPORTANCE_HIGH
        )
        alertChannel.description =
            "Notifications lors de la détection d'un accident"
        alertChannel.enableVibration(true)
        alertChannel.enableLights(true)
        manager.createNotificationChannel(alertChannel)

        // ── Canal 3 — Countdown ───────────────────────────────
        val countdownChannel = NotificationChannel(
            CHANNEL_ID_COUNTDOWN,
            "Compte à rebours",
            NotificationManager.IMPORTANCE_HIGH
        )
        countdownChannel.description =
            "Notification pendant le compte à rebours avant alerte"
        countdownChannel.enableVibration(true)
        manager.createNotificationChannel(countdownChannel)
    }

    companion object {
        const val CHANNEL_ID_SERVICE = "roadalert_service_channel"
        const val CHANNEL_ID_ALERT = "roadalert_alert_channel"
        const val CHANNEL_ID_COUNTDOWN = "roadalert_countdown_channel"

        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_COUNTDOWN = 1002
        const val NOTIFICATION_ID_ALERT = 1003
    }
}