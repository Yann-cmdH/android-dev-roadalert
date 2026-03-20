package com.roadalert.cameroun.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.db.entity.TriggerType
import com.roadalert.cameroun.data.db.entity.User
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.detection.AlertManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val userProfileRepository: UserProfileRepository,
    private val alertManager: AlertManager
) : AndroidViewModel(application) {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _showSosDialog = MutableStateFlow(false)
    val showSosDialog: StateFlow<Boolean> = _showSosDialog

    fun loadUser() {
        viewModelScope.launch {
            userProfileRepository.getUser().collect { user ->
                _user.value = user
            }
        }
    }

    fun updateServiceState(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }

    fun onSosButtonPressed() {
        _showSosDialog.value = true
    }

    fun onSosCancelled() {
        _showSosDialog.value = false
    }

    // ── SOS confirmé — déclenche AlertManager MANUAL ──────
    fun onSosConfirmed() {
        _showSosDialog.value = false
        alertManager.triggerAlert(
            triggerType = TriggerType.MANUAL,
            gForceValue = 0f,
            scope = viewModelScope
        )
    }
}