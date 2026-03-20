package com.roadalert.cameroun.detection

import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.roadalert.cameroun.util.Constants
import kotlin.math.sqrt

class SensorFusionEngine(
    private val listener: AccidentListener
) {

    // ── État machine ──────────────────────────────────────────
    @Volatile
    private var state: DetectionState = DetectionState.IDLE

    // ── Conditions ────────────────────────────────────────────
    @Volatile
    private var condition1Met: Boolean = false
    @Volatile
    private var condition2Met: Boolean = false
    @Volatile
    private var condition3Met: Boolean = false

    // ── Timestamps ────────────────────────────────────────────
    // Moment où C1 a été détectée
    @Volatile
    private var condition1Timestamp: Long = 0L

    // Moment où C3 a commencé (immobilité)
    @Volatile
    private var immobilityStartTime: Long = 0L

    // ── Seuils — SAD section 6 ────────────────────────────────
    // Seuil G-force impact — 24.5 m/s²
    // On compare magnitude² pour éviter sqrt coûteux
    // 24.5² = 600.25
    private val impactThresholdSquared =
        Constants.IMPACT_THRESHOLD * Constants.IMPACT_THRESHOLD

    // Seuil gyroscope — téléphone horizontal
    // rad/s — mouvement angulaire faible = horizontal
    private val gyroThreshold = 1.0f

    // Seuil immobilité — Linear Acceleration
    // m/s² — excluant gravité
    private val immobilityThreshold = 0.5f

    // Fenêtre de temps pour confirmer C2 après C1 — 5 secondes
    private val conditionWindow = 5_000L

    // ── Monitoring actif ──────────────────────────────────────
    @Volatile
    private var isMonitoring: Boolean = false

    // ── API publique ──────────────────────────────────────────

    fun startMonitoring() {
        reset()
        isMonitoring = true
        state = DetectionState.MONITORING
    }

    fun stopMonitoring() {
        isMonitoring = false
        state = DetectionState.IDLE
    }

    // ── Point d'entrée données capteurs ──────────────────────

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

    // ── Traitement Accelerometer — Condition 1 ────────────────

    private fun processAccelerometer(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        // Magnitude² — évite calcul sqrt
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

    // ── Traitement Gyroscope — Condition 2 ───────────────────

    private fun processGyroscope(values: FloatArray) {
        if (!condition1Met) return

        // Vérifier fenêtre de temps — C2 doit arriver
        // dans les 5 secondes après C1
        val elapsed = System.currentTimeMillis() - condition1Timestamp
        if (elapsed > conditionWindow) {
            // C1 trop ancienne — réinitialiser
            reset()
            return
        }

        val rotX = values[0]
        val rotY = values[1]

        // Téléphone horizontal si rotation faible sur X et Y
        val isHorizontal = Math.abs(rotX) < gyroThreshold &&
                Math.abs(rotY) < gyroThreshold

        if (isHorizontal && !condition2Met) {
            condition2Met = true
            state = DetectionState.CONDITION_2_MET
            listener.onConditionProgress(2)
        }
    }

    // ── Traitement Linear Acceleration — Condition 3 ─────────

    private fun processLinearAcceleration(values: FloatArray) {
        if (!condition1Met || !condition2Met) return

        val x = values[0]
        val y = values[1]
        val z = values[2]

        // Magnitude mouvement net sans gravité
        val magnitude = sqrt(x * x + y * y + z * z)

        val isImmobile = magnitude < immobilityThreshold

        if (isImmobile) {
            if (immobilityStartTime == 0L) {
                // Début de la période d'immobilité
                immobilityStartTime = System.currentTimeMillis()
            } else {
                // Vérifier durée d'immobilité
                val immobilityDuration =
                    System.currentTimeMillis() - immobilityStartTime

                if (immobilityDuration >= Constants.NO_MOVEMENT_TIMEOUT) {
                    condition3Met = true
                    listener.onConditionProgress(3)
                }
            }
        } else {
            // Mouvement détecté — réinitialiser le timer C3
            immobilityStartTime = 0L
        }
    }

    // ── Évaluation finale des 3 conditions ───────────────────

    private fun evaluate() {
        if (condition1Met && condition2Met && condition3Met) {
            if (state != DetectionState.ACCIDENT_CONFIRMED) {
                state = DetectionState.ACCIDENT_CONFIRMED
                // Confiance basée sur les 3 conditions réunies
                val confidence = calculateConfidence()
                listener.onAccidentDetected(confidence)
            }
        }
    }

    // ── Calcul confiance ──────────────────────────────────────

    private fun calculateConfidence(): Float {
        var confidence = 0f
        if (condition1Met) confidence += 0.4f
        if (condition2Met) confidence += 0.3f
        if (condition3Met) confidence += 0.3f
        return confidence
    }

    // ── Vérification état ─────────────────────────────────────

    fun isAccidentConfirmed(): Boolean {
        return state == DetectionState.ACCIDENT_CONFIRMED
    }

    fun getCurrentState(): DetectionState = state

    // ── Reset complet ─────────────────────────────────────────

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

    // ── Cancel ────────────────────────────────────────────────

    fun cancel() {
        state = DetectionState.CANCELLED
        reset()
        state = DetectionState.CANCELLED
    }
}