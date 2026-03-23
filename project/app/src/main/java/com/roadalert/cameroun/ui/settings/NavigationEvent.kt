package com.roadalert.cameroun.ui.settings

sealed class NavigationEvent {
    object GoToSplash : NavigationEvent()
    data class GoToProfileSetup(val startStep: Int = 1) : NavigationEvent()
}
