package com.roadalert.cameroun.ui.settings

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.detection.AccidentDetectionService
import com.roadalert.cameroun.util.AppSettings
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    application: Application,
    private val userProfileRepository: UserProfileRepository,
    private val accidentRepository: AccidentRepository,
    private val appSettings: AppSettings,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _isServiceRunning = MutableStateFlow(false)

    private var restartJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                sensitivity = appSettings.getSensitivityLevel(),
                countdownSeconds = appSettings.getCountdownSeconds(),
                soundEnabled = appSettings.isSoundEnabled(),
                vibrationEnabled = appSettings.isVibrationEnabled()
            )
        }

        viewModelScope.launch {
            userProfileRepository.getUser()
                .flatMapLatest { user ->
                    _uiState.update { it.copy(user = user) }
                    if (user == null) flowOf(emptyList())
                    else userProfileRepository.getContacts(user.id)
                }
                .collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts) }
                }
        }
    }

    fun updateServiceState(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
        _uiState.update { it.copy(isServiceRunning = isRunning) }
    }

    fun setSensitivity(level: String) {
        appSettings.setSensitivitySync(level)
        _uiState.update { it.copy(sensitivity = level) }
        restartServiceDebounced()
    }

    fun setCountdown(seconds: Int) {
        appSettings.setCountdownSync(seconds)
        _uiState.update { it.copy(countdownSeconds = seconds) }
        restartServiceDebounced()
    }

    fun setSoundEnabled(enabled: Boolean) {
        appSettings.setSoundEnabled(enabled)
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        appSettings.setVibrationEnabled(enabled)
        _uiState.update { it.copy(vibrationEnabled = enabled) }
    }

    private fun restartServiceDebounced() {
        restartJob?.cancel()
        restartJob = viewModelScope.launch {
            delay(800L)
            if (_isServiceRunning.value) {
                val stopIntent = Intent(
                    getApplication(),
                    AccidentDetectionService::class.java
                ).apply {
                    action = ServiceActions.ACTION_STOP_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(stopIntent)
                } else {
                    getApplication<Application>().startService(stopIntent)
                }

                delay(500L)

                val startIntent = Intent(
                    getApplication(),
                    AccidentDetectionService::class.java
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(startIntent)
                } else {
                    getApplication<Application>().startService(startIntent)
                }

                _uiState.update {
                    it.copy(snackbarMessage = "Paramètres appliqués. Service redémarré.")
                }
            }
        }
    }

    fun onResetRequested() {
        _uiState.update { it.copy(showResetDialog = true, resetButtonEnabled = false) }
        viewModelScope.launch {
            delay(1500L)
            _uiState.update { it.copy(resetButtonEnabled = true) }
        }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(showResetDialog = false, resetButtonEnabled = false) }
    }

    fun confirmReset() {
        viewModelScope.launch {
            if (accidentRepository.hasPendingAlert()) {
                _uiState.update {
                    it.copy(snackbarMessage = "Alerte en cours. Attendez la fin.")
                }
                dismissResetDialog()
                return@launch
            }

            val stopIntent = Intent(
                getApplication(),
                AccidentDetectionService::class.java
            ).apply {
                action = ServiceActions.ACTION_STOP_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(stopIntent)
            } else {
                getApplication<Application>().startService(stopIntent)
            }

            delay(500L)
            database.clearAllTables()
            appSettings.reset()
            _navigationEvent.emit(NavigationEvent.GoToSplash)
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun navigateToProfileSetup(startStep: Int = 1) {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.GoToProfileSetup(startStep))
        }
    }
}
