package com.roadalert.cameroun.detection

import com.roadalert.cameroun.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CountdownManager(
    private val durationMs: Long =
        Constants.COUNTDOWN_DURATION
) {

    // ── Scope coroutine dédié ─────────────────────────────
    private val scope = CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    )

    private var countdownJob: Job? = null

    // ── Temps restant exposé à l'UI ───────────────────────
    private val _remainingTime = MutableStateFlow(durationMs)
    val remainingTime: StateFlow<Long> = _remainingTime

    // ── État running ──────────────────────────────────────
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    // ── Démarrer le countdown ─────────────────────────────

    fun start(onFinished: () -> Unit) {
        if (_isRunning.value) return

        _remainingTime.value = durationMs
        _isRunning.value = true

        countdownJob = scope.launch {
            var remaining = durationMs

            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1_000L
                _remainingTime.value = remaining
            }

            _isRunning.value = false
            onFinished()
        }
    }

    // ── Annuler le countdown ──────────────────────────────

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        _isRunning.value = false
        _remainingTime.value = durationMs
    }

    // ── Libérer les ressources ────────────────────────────

    fun release() {
        cancel()
        scope.cancel()
    }
}