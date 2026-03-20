package com.roadalert.cameroun.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accident_event",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"])
    ]
)
data class AccidentEvent(

    @PrimaryKey
    val id: String,

    val userId: String,

    val timestamp: Long = System.currentTimeMillis(),

    val latitude: Double? = null,
    val longitude: Double? = null,
    val isPositionApproximate: Boolean = false,

    val gForceValue: Float = 0f,

    val alertStatus: String = AlertStatus.PENDING.name,

    val triggerType: String = TriggerType.AUTO.name,

    val cancelledByUser: Boolean = false,

    val smsContact1Status: String = SmsStatus.PENDING.name,
    val smsContact2Status: String? = null,
    val smsContact3Status: String? = null,

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
        return alertStatus == AlertStatus.PENDING_RETRY.name
    }

    fun isFullySent(): Boolean {
        return alertStatus == AlertStatus.SENT.name
    }

    fun isFailed(): Boolean {
        return alertStatus == AlertStatus.FAILED.name
    }

    fun isPartial(): Boolean {
        return alertStatus == AlertStatus.PARTIAL.name
    }

    fun toAlertStatus(): AlertStatus {
        return AlertStatus.valueOf(alertStatus)
    }

    fun toTriggerType(): TriggerType {
        return TriggerType.valueOf(triggerType)
    }
}