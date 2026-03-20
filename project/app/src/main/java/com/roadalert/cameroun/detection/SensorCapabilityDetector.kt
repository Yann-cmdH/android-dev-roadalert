package com.roadalert.cameroun.detection

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class SensorCapabilityDetector(context: Context) {

    private val sensorManager = context
        .getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // ── Vérification capteurs disponibles ────────────────────

    fun hasAccelerometer(): Boolean {
        return sensorManager
            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    fun hasGyroscope(): Boolean {
        return sensorManager
            .getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    fun hasLinearAcceleration(): Boolean {
        return sensorManager
            .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }

    // ── Détermine le mode de détection ───────────────────────

    fun detectMode(): DetectionMode {
        return if (hasAccelerometer() && hasGyroscope()) {
            DetectionMode.FULL_MODE
        } else {
            DetectionMode.DEGRADED_MODE
        }
    }

    // ── Log des capteurs disponibles ─────────────────────────

    fun getAvailableSensors(): String {
        return buildString {
            append("Accelerometer: ${hasAccelerometer()}\n")
            append("Gyroscope: ${hasGyroscope()}\n")
            append("LinearAcceleration: ${hasLinearAcceleration()}\n")
            append("Mode: ${detectMode()}")
        }
    }
}