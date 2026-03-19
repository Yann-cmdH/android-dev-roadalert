package com.roadalert.cameroun.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _isProfileComplete =
        MutableStateFlow<Boolean?>(null)
    val isProfileComplete: StateFlow<Boolean?> =
        _isProfileComplete

    fun checkProfile() {
        viewModelScope.launch {
            try {
                _isProfileComplete.value =
                    repository.isProfileComplete()
            } catch (e: Exception) {
                // DB corrompue ou inaccessible
                // → traiter comme profil incomplet
                // → navigation vers Onboarding
                _isProfileComplete.value = false
            }
        }
    }
}