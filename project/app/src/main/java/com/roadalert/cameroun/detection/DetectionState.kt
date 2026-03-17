package com.roadalert.cameroun.detection

enum class DetectionState {
    IDLE,
    MONITORING,
    CONDITION_1_MET,
    CONDITION_2_MET,
    ACCIDENT_CONFIRMED,
    CANCELLED
}