package com.roadalert.cameroun.detection

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.roadalert.cameroun.R
import com.roadalert.cameroun.RoadAlertApplication
import com.roadalert.cameroun.ui.home.HomeActivity
import com.roadalert.cameroun.util.ServiceActions

class AccidentDetectionService : Service(),
    SensorEventListener,
    AccidentListener {

    // ── Capteurs ──────────────────────────────────────────────
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var linearAcceleration: Sensor? = null

    // ── Composants détection ──────────────────────────────────
    private lateinit var fusionEngine: SensorFusionEngine
    private lateinit var countdownManager: CountdownManager
    private lateinit var capabilityDetector: SensorCapabilityDetector

    // ── État interne ──────────────────────────────────────────
    private var isMonitoring = false

    // ── Receiver annulation countdown ─────────────────────────
    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ServiceActions.ACTION_CANCEL_COUNTDOWN) {
                handleCancellation()
            }
        }
    }
    private var isCancelReceiverRegistered = false

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        capabilityDetector = SensorCapabilityDetector(this)
        fusionEngine = SensorFusionEngine(this)
        countdownManager = CountdownManager()

        registerCancelReceiver()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ServiceActions.ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundWithNotification()
        registerSensors()
        fusionEngine.startMonitoring()
        isMonitoring = true
        broadcastServiceState(true)

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterSensors()
        countdownManager.release()
        fusionEngine.stopMonitoring()
        isMonitoring = false
        broadcastServiceState(false)
        unregisterCancelReceiver()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification persistante ──────────────────────────────

    private fun startForegroundWithNotification() {
        val homeIntent = Intent(
            this, HomeActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            homeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            RoadAlertApplication.CHANNEL_ID_SERVICE
        )
            .setContentTitle(
                getString(R.string.notification_service_title)
            )
            .setContentText(
                getString(R.string.notification_service_text)
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(
            RoadAlertApplication.NOTIFICATION_ID_SERVICE,
            notification
        )
    }

    // ── Enregistrement capteurs ───────────────────────────────

    private fun registerSensors() {
        val mode = capabilityDetector.detectMode()

        // Accéléromètre — Condition 1 — TOUJOURS requis
        accelerometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )
        accelerometer?.let {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        if (mode == DetectionMode.FULL_MODE) {
            // Gyroscope — Condition 2
            gyroscope = sensorManager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            )
            gyroscope?.let {
                sensorManager.registerListener(
                    this, it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

            // Linear Acceleration — Condition 3
            linearAcceleration = sensorManager.getDefaultSensor(
                Sensor.TYPE_LINEAR_ACCELERATION
            )
            linearAcceleration?.let {
                sensorManager.registerListener(
                    this, it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }
    }

    private fun unregisterSensors() {
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            // Ignoré — capteurs déjà désenregistrés
        }
    }

    // ── SensorEventListener ───────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        if (!isMonitoring) return
        fusionEngine.onSensorData(
            event.sensor.type,
            event.values
        )
    }

    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int
    ) {
        // Non utilisé
    }

    // ── AccidentListener ──────────────────────────────────────

    override fun onAccidentDetected(confidence: Float) {
        // Démarrer le countdown
        countdownManager.start {
            onCountdownFinished()
        }

        // Notifier l'UI — ouvrir CountdownActivity
        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_DETECTED
        ).apply {
            putExtra(
                ServiceActions.EXTRA_CONFIDENCE,
                confidence
            )
            // Package explicite pour sécurité Android 13+
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onConditionProgress(step: Int) {
        // Log progression pour debug — non utilisé en production
    }

    // ── Countdown terminé — accident confirmé ─────────────────

    private fun onCountdownFinished() {
        // Arrêter la détection pendant le traitement
        fusionEngine.stopMonitoring()
        isMonitoring = false
        unregisterSensors()

        // Notifier Sprint 4 — AlertManager
        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_CONFIRMED
        ).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    // ── Annulation countdown ──────────────────────────────────

    private fun handleCancellation() {
        // Annuler le countdown
        countdownManager.cancel()

        // Réinitialiser le moteur de détection
        fusionEngine.reset()
        fusionEngine.startMonitoring()
        isMonitoring = true

        // Notifier l'UI
        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_CANCELLED
        ).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    // ── Broadcast état service ────────────────────────────────

    private fun broadcastServiceState(isRunning: Boolean) {
        val intent = Intent(
            ServiceActions.ACTION_SERVICE_STATE_CHANGED
        ).apply {
            putExtra(
                ServiceActions.EXTRA_SERVICE_RUNNING,
                isRunning
            )
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    // ── Register cancel receiver — Android 13+ safe ───────────

    private fun registerCancelReceiver() {
        if (isCancelReceiverRegistered) return

        val filter = IntentFilter(
            ServiceActions.ACTION_CANCEL_COUNTDOWN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                cancelReceiver,
                filter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(cancelReceiver, filter)
        }

        isCancelReceiverRegistered = true
    }

    private fun unregisterCancelReceiver() {
        if (!isCancelReceiverRegistered) return
        try {
            unregisterReceiver(cancelReceiver)
            isCancelReceiverRegistered = false
        } catch (e: IllegalArgumentException) {
            // Ignoré — receiver déjà désenregistré
        }
    }
}