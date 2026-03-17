package com.roadalert.cameroun.detection

interface AccidentListener {
    fun onAccidentDetected(confidence: Float)
    fun onConditionProgress(step: Int)
}