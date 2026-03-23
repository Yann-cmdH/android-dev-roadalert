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
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.db.entity.TriggerType
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.ui.home.HomeActivity
import com.roadalert.cameroun.util.AppSettings
import com.roadalert.cameroun.util.Constants
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AccidentDetectionService : Service(),
    SensorEventListener,
    AccidentListener {

    // ── Capteurs ──────────────────────────────────────────
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var linearAcceleration: Sensor? = null

    // ── Composants détection ──────────────────────────────
    private lateinit var fusionEngine: SensorFusionEngine
    private lateinit var countdownManager: CountdownManager
    private lateinit var capabilityDetector: SensorCapabilityDetector

    // ── AlertManager ──────────────────────────────────────
    private lateinit var alertManager: AlertManager

    // ── Scope coroutines du Service ───────────────────────
    private val serviceScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )

    // ── État interne ──────────────────────────────────────
    private var isMonitoring = false

    // ── Heartbeat Watchdog ────────────────────────────────
    private var heartbeatJob: Job? = null

    // ── Receiver annulation countdown ─────────────────────
    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (intent.action ==
                ServiceActions.ACTION_CANCEL_COUNTDOWN) {
                handleCancellation()
            }
        }
    }
    private var isCancelReceiverRegistered = false

    // ── Lifecycle ─────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        capabilityDetector = SensorCapabilityDetector(this)

        // ── Lire les paramètres utilisateur ───────────────
        // AppSettings lit SharedPreferences
        // Si l'utilisateur a modifié la sensibilité
        // ou la durée du countdown dans Settings
        // le service utilise ces valeurs
        val appSettings = AppSettings(this)

        fusionEngine = SensorFusionEngine(
            listener = this,
            impactThreshold = appSettings.getSensitivityThreshold(),
            noMovementTimeout = Constants.NO_MOVEMENT_TIMEOUT
        )

        countdownManager = CountdownManager(
            durationMs = appSettings.getCountdownDuration()
        )

        // ── Initialiser AlertManager avec repositories ────
        val database = AppDatabase.getInstance(this)
        val accidentRepository = AccidentRepository(
            database.accidentEventDAO()
        )
        val userProfileRepository = UserProfileRepository(
            database,
            database.userDAO(),
            database.emergencyContactDAO()
        )
        alertManager = AlertManager(
            context = this,
            accidentRepository = accidentRepository,
            userProfileRepository = userProfileRepository
        )

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
        startHeartbeat()

        return START_STICKY
    }

    override fun onDestroy() {
        unregisterSensors()
        countdownManager.release()
        fusionEngine.stopMonitoring()
        isMonitoring = false
        broadcastServiceState(false)
        unregisterCancelReceiver()
        heartbeatJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification persistante ──────────────────────────

    private fun startForegroundWithNotification() {
        val homeIntent = Intent(
            this, HomeActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, homeIntent,
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

    // ── Enregistrement capteurs ───────────────────────────

    private fun registerSensors() {
        val mode = capabilityDetector.detectMode()

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
            gyroscope = sensorManager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            )
            gyroscope?.let {
                sensorManager.registerListener(
                    this, it,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

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
            // Ignoré
        }
    }

    // ── SensorEventListener ───────────────────────────────

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
    ) {}

    // ── AccidentListener ──────────────────────────────────

    override fun onAccidentDetected(confidence: Float) {
        countdownManager.start {
            onCountdownFinished(confidence)
        }

        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_DETECTED
        ).apply {
            putExtra(
                ServiceActions.EXTRA_CONFIDENCE,
                confidence
            )
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onConditionProgress(step: Int) {}

    // ── Countdown terminé — DÉCLENCHE ALERTMANAGER ────────

    private fun onCountdownFinished(
        confidence: Float = 0f
    ) {
        fusionEngine.stopMonitoring()
        isMonitoring = false
        unregisterSensors()

        alertManager.triggerAlert(
            triggerType = TriggerType.AUTO,
            gForceValue = confidence,
            scope = serviceScope
        )
    }

    // ── Annulation countdown ──────────────────────────────

    private fun handleCancellation() {
        countdownManager.cancel()

        fusionEngine.reset()
        fusionEngine.startMonitoring()
        isMonitoring = true

        val intent = Intent(
            ServiceActions.ACTION_ACCIDENT_CANCELLED
        ).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    // ── Reprendre surveillance après alerte ───────────────

    fun resumeMonitoring() {
        if (!isMonitoring) {
            registerSensors()
            fusionEngine.startMonitoring()
            isMonitoring = true
        }
    }

    // ── Broadcast état service ────────────────────────────

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

    // ── Heartbeat Watchdog ────────────────────────────────

    private fun startHeartbeat() {
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                AppSettings(this@AccidentDetectionService).writeHeartbeat()
                delay(30_000L)
            }
        }
    }

    // ── Register cancel receiver ──────────────────────────

    private fun registerCancelReceiver() {
        if (isCancelReceiverRegistered) return

        val filter = IntentFilter(
            ServiceActions.ACTION_CANCEL_COUNTDOWN
        )

        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU) {
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
            // Ignoré
        }
    }
}