package com.roadalert.cameroun.util

object Constants {
    const val IMPACT_THRESHOLD = 24.5f
    const val NO_MOVEMENT_TIMEOUT = 10_000L
    const val COUNTDOWN_DURATION = 15_000L
    const val GPS_TIMEOUT = 10_000L
    const val MAX_SMS_LENGTH = 160
    const val MAX_RETRY_COUNT = 3
}

object IdGenerator {
    fun newId(): String = java.util.UUID.randomUUID().toString()
}