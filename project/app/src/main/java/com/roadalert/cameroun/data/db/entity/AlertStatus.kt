package com.roadalert.cameroun.data.db.entity

enum class AlertStatus {
    PENDING,
    SENT,
    PARTIAL,
    PENDING_RETRY,
    FAILED
}

enum class TriggerType {
    AUTO,
    MANUAL
}

enum class SmsStatus {
    PENDING,
    SENT,
    FAILED
}