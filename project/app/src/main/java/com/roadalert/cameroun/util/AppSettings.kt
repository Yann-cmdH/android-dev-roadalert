package com.roadalert.cameroun.util

import android.content.Context

class AppSettings(context: Context) {

    companion object {
        private const val PREFS_NAME = "roadalert_prefs"

        const val KEY_SENSITIVITY = "sensitivity_level"
        const val KEY_COUNTDOWN = "countdown_seconds"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_VIBRATION = "vibration_enabled"

        const val SENSITIVITY_LOW = "LOW"
        const val SENSITIVITY_MEDIUM = "MEDIUM"
        const val SENSITIVITY_HIGH = "HIGH"

        const val THRESHOLD_LOW = 29.4f
        const val THRESHOLD_MEDIUM = 24.5f
        const val THRESHOLD_HIGH = 19.6f

        const val COUNTDOWN_10 = 10
        const val COUNTDOWN_15 = 15
        const val COUNTDOWN_20 = 20
    }

    private val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ── Getters ───────────────────────────────────────────

    fun getSensitivityLevel(): String =
        prefs.getString(KEY_SENSITIVITY, SENSITIVITY_MEDIUM)
            ?: SENSITIVITY_MEDIUM

    fun getSensitivityThreshold(): Float =
        when (getSensitivityLevel()) {
            SENSITIVITY_LOW -> THRESHOLD_LOW
            SENSITIVITY_HIGH -> THRESHOLD_HIGH
            else -> THRESHOLD_MEDIUM
        }

    fun getCountdownSeconds(): Int =
        prefs.getInt(KEY_COUNTDOWN, COUNTDOWN_15)

    fun getCountdownDuration(): Long =
        getCountdownSeconds() * 1000L

    fun isSoundEnabled(): Boolean =
        prefs.getBoolean(KEY_SOUND, true)

    fun isVibrationEnabled(): Boolean =
        prefs.getBoolean(KEY_VIBRATION, true)

    // ── Setters ───────────────────────────────────────────

    fun setSensitivity(level: String) {
        prefs.edit().putString(KEY_SENSITIVITY, level).apply()
    }

    fun setCountdown(seconds: Int) {
        prefs.edit().putInt(KEY_COUNTDOWN, seconds).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}