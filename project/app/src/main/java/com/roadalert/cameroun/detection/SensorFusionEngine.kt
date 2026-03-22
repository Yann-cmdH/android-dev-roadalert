package com.roadalert.cameroun.detection

import com.roadalert.cameroun.util.Constants
import kotlin.math.abs
import kotlin.math.sqrt

class SensorFusionEngine(
    private val listener: AccidentListener,
    private val impactThreshold: Float =
        Constants.IMPACT_THRESHOLD,
    private val noMovementTimeout: Long =
        Constants.NO_MOVEMENT_TIMEOUT
) {

    // ── État machine ──────────────────────────────────────
    @Volatile
    private var state: DetectionState = DetectionState.IDLE

    // ── Conditions ────────────────────────────────────────
    @Volatile private var condition1Met: Boolean = false
    @Volatile private var condition2Met: Boolean = false
    @Volatile private var condition3Met: Boolean = false

    // ── Timestamps ────────────────────────────────────────
    @Volatile private var condition1Timestamp: Long = 0L
    @Volatile private var immobilityStartTime: Long = 0L

    // ── Seuils calculés depuis paramètres ─────────────────
    // On compare magnitude² pour éviter sqrt coûteux
    private val impactThresholdSquared =
        impactThreshold * impactThreshold

    private val gyroThreshold = 1.0f
    private val immobilityThreshold = 0.5f
    private val conditionWindow = 5_000L

    // ── Monitoring actif ──────────────────────────────────
    @Volatile
    private var isMonitoring: Boolean = false

    // ── API publique ──────────────────────────────────────

    fun startMonitoring() {
        reset()
        isMonitoring = true
        state = DetectionState.MONITORING
    }

    fun stopMonitoring() {
        isMonitoring = false
        state = DetectionState.IDLE
    }

    // ── Point d'entrée données capteurs ───────────────────

    fun onSensorData(sensorType: Int, values: FloatArray) {
        if (!isMonitoring) return
        if (state == DetectionState.ACCIDENT_CONFIRMED) return
        if (state == DetectionState.CANCELLED) return

        when (sensorType) {
            android.hardware.Sensor.TYPE_ACCELEROMETER ->
                processAccelerometer(values)
            android.hardware.Sensor.TYPE_GYROSCOPE ->
                processGyroscope(values)
            android.hardware.Sensor.TYPE_LINEAR_ACCELERATION ->
                processLinearAcceleration(values)
        }

        evaluate()
    }

    // ── Condition 1 — Impact ──────────────────────────────

    private fun processAccelerometer(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val magnitudeSquared = x * x + y * y + z * z

        if (magnitudeSquared > impactThresholdSquared) {
            if (!condition1Met) {
                condition1Met = true
                condition1Timestamp = System.currentTimeMillis()
                state = DetectionState.CONDITION_1_MET
                listener.onConditionProgress(1)
            }
        }
    }

    // ── Condition 2 — Gyroscope horizontal ────────────────

    private fun processGyroscope(values: FloatArray) {
        if (!condition1Met) return

        val elapsed = System.currentTimeMillis() -
                condition1Timestamp
        if (elapsed > conditionWindow) {
            reset()
            return
        }

        val rotX = values[0]
        val rotY = values[1]

        val isHorizontal =
            abs(rotX) < gyroThreshold &&
                    abs(rotY) < gyroThreshold

        if (isHorizontal && !condition2Met) {
            condition2Met = true
            state = DetectionState.CONDITION_2_MET
            listener.onConditionProgress(2)
        }
    }

    // ── Condition 3 — Immobilité ──────────────────────────

    private fun processLinearAcceleration(
        values: FloatArray
    ) {
        if (!condition1Met || !condition2Met) return

        val x = values[0]
        val y = values[1]
        val z = values[2]

        val magnitude = sqrt(x * x + y * y + z * z)
        val isImmobile = magnitude < immobilityThreshold

        if (isImmobile) {
            if (immobilityStartTime == 0L) {
                immobilityStartTime =
                    System.currentTimeMillis()
            } else {
                val immobilityDuration =
                    System.currentTimeMillis() -
                            immobilityStartTime

                if (immobilityDuration >= noMovementTimeout) {
                    condition3Met = true
                    listener.onConditionProgress(3)
                }
            }
        } else {
            immobilityStartTime = 0L
        }
    }

    // ── Évaluation finale ─────────────────────────────────

    private fun evaluate() {
        if (condition1Met && condition2Met && condition3Met) {
            if (state != DetectionState.ACCIDENT_CONFIRMED) {
                state = DetectionState.ACCIDENT_CONFIRMED
                val confidence = calculateConfidence()
                listener.onAccidentDetected(confidence)
            }
        }
    }

    private fun calculateConfidence(): Float {
        var confidence = 0f
        if (condition1Met) confidence += 0.4f
        if (condition2Met) confidence += 0.3f
        if (condition3Met) confidence += 0.3f
        return confidence
    }

    fun isAccidentConfirmed(): Boolean {
        return state == DetectionState.ACCIDENT_CONFIRMED
    }

    fun getCurrentState(): DetectionState = state

    fun reset() {
        condition1Met = false
        condition2Met = false
        condition3Met = false
        condition1Timestamp = 0L
        immobilityStartTime = 0L
        state = if (isMonitoring)
            DetectionState.MONITORING
        else
            DetectionState.IDLE
    }

    fun cancel() {
        state = DetectionState.CANCELLED
        reset()
        state = DetectionState.CANCELLED
    }
}