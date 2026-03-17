package com.roadalert.cameroun.ui.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OnboardingViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    private val _isLocationGranted = MutableStateFlow(false)
    val isLocationGranted: StateFlow<Boolean> = _isLocationGranted

    private val _isSmsGranted = MutableStateFlow(false)
    val isSmsGranted: StateFlow<Boolean> = _isSmsGranted

    private val _isBatteryExempted = MutableStateFlow(false)
    val isBatteryExempted: StateFlow<Boolean> = _isBatteryExempted

    val canProceed: Boolean
        get() = _isLocationGranted.value && _isSmsGranted.value

    fun onLocationGranted() {
        _isLocationGranted.value = true
        _currentStep.value = 2
    }

    fun onSmsGranted() {
        _isSmsGranted.value = true
    }

    fun onBatteryExempted() {
        _isBatteryExempted.value = true
        _currentStep.value = 3
    }

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
        }
    }
}