package com.roadalert.cameroun.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.db.entity.User
import com.roadalert.cameroun.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    private val _showSosDialog = MutableStateFlow(false)
    val showSosDialog: StateFlow<Boolean> = _showSosDialog

    fun loadUser() {
        viewModelScope.launch {
            repository.getUser().collect { user ->
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

    fun onSosConfirmed() {
        _showSosDialog.value = false
    }
}