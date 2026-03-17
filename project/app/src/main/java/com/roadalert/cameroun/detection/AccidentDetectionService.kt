package com.roadalert.cameroun.detection

import android.app.Notification
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

    // ── Moteur de détection ───────────────────────────────────
    private lateinit var fusionEngine: SensorFusionEngine
    private lateinit var countdownManager: CountdownManager
    private lateinit var capabilityDetector: SensorCapabilityDetector

    // ── Receiver pour annulation countdown ───────────────────
    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ServiceActions.ACTION_CANCEL_COUNTDOWN) {
                handleCancellation()
            }
        }
    }

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
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundWithNotification()
        registerSensors()
        fusionEngine.startMonitoring()
        broadcastServiceState(true)

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterSensors()
        countdownManager.release()
        fusionEngine.stopMonitoring()
        broadcastServiceState(false)
        unregisterReceiver(cancelReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification persistante ──────────────────────────────

    private fun startForegroundWithNotification() {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
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

        // Accéléromètre — Condition 1 — TOUJOURS
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
        sensorManager.unregisterListener(this)
    }

    // ── SensorEventListener ───────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        fusionEngine.onSensorData(event.sensor.type, event.values)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Non utilisé
    }

    // ── AccidentListener ──────────────────────────────────────

    override fun onAccidentDetected(confidence: Float) {
        countdownManager.start {
            onCountdownFinished()
        }

        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_DETECTED
        ).apply {
            putExtra(
                ServiceActions.EXTRA_CONFIDENCE,
                confidence
            )
        }
        sendBroadcast(intent)
    }

    override fun onConditionProgress(step: Int) {
        // Log progression pour debug
    }

    // ── Countdown terminé — accident confirmé ─────────────────

    private fun onCountdownFinished() {
        fusionEngine.stopMonitoring()
        unregisterSensors()

        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_CONFIRMED
        )
        sendBroadcast(intent)
    }

    // ── Annulation countdown ──────────────────────────────────

    private fun handleCancellation() {
        countdownManager.cancel()
        fusionEngine.reset()
        fusionEngine.startMonitoring()

        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_CANCELLED
        )
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
        }
        sendBroadcast(intent)
    }

    // ── Register cancel receiver ──────────────────────────────

    private fun registerCancelReceiver() {
        val filter = IntentFilter(
            ServiceActions.ACTION_CANCEL_COUNTDOWN
        )
        registerReceiver(cancelReceiver, filter)
    }
}