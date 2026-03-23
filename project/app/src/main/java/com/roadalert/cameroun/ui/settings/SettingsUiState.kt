package com.roadalert.cameroun.ui.settings

import com.roadalert.cameroun.data.db.entity.EmergencyContact
import com.roadalert.cameroun.data.db.entity.User

data class SettingsUiState(
    val user: User? = null,
    val contacts: List<EmergencyContact> = emptyList(),
    val sensitivity: String = "MEDIUM",
    val countdownSeconds: Int = 15,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isServiceRunning: Boolean = false,
    val snackbarMessage: String? = null,
    val showResetDialog: Boolean = false,
    val resetButtonEnabled: Boolean = false
)
