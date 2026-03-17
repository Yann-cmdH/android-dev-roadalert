package com.roadalert.cameroun.data.db.entity

enum class AlertStatus {
    SENT,
    FAILED,
    CANCELLED,
    PENDING_RETRY,
    PARTIAL
}