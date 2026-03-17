package com.roadalert.cameroun.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accident_event")
data class AccidentEvent(
    @PrimaryKey
    val id: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gForceValue: Float,
    val alertStatus: AlertStatus = AlertStatus.PENDING_RETRY,
    val cancelledByUser: Boolean = false,
    val smsContactsCount: Int = 0,
    val retryCount: Int = 0
) {
    fun hasLocation(): Boolean {
        return latitude != null && longitude != null
    }

    fun getMapsUrl(): String? {
        if (!hasLocation()) return null
        return "https://maps.google.com/?q=$latitude,$longitude"
    }

    fun isPendingRetry(): Boolean {
        return alertStatus == AlertStatus.PENDING_RETRY
    }
}