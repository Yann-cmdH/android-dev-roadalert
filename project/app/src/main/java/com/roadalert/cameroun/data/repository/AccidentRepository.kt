package com.roadalert.cameroun.data.repository

import com.roadalert.cameroun.data.db.dao.AccidentEventDAO
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.AlertStatus
import kotlinx.coroutines.flow.Flow

class AccidentRepository(
    private val accidentEventDAO: AccidentEventDAO
) {

    // ── Save a new accident event ─────────────────────────────

    suspend fun saveEvent(event: AccidentEvent) {
        accidentEventDAO.insertEvent(event)
    }

    // ── Get full history ordered by timestamp DESC ────────────

    fun getHistory(): Flow<List<AccidentEvent>> {
        return accidentEventDAO.getAllEvents()
    }

    // ── Get single event by id ────────────────────────────────

    suspend fun getEventById(eventId: String): AccidentEvent? {
        return accidentEventDAO.getEventById(eventId)
    }

    // ── Update alert status after dispatch ───────────────────

    suspend fun updateStatus(eventId: String, status: AlertStatus) {
        accidentEventDAO.updateStatus(eventId, status)
    }

    // ── Get all events pending retry (SAD NFR04) ──────────────

    suspend fun getPendingRetries(): List<AccidentEvent> {
        return accidentEventDAO.getPendingRetries()
    }

    // ── Increment retry count after failed attempt ────────────

    suspend fun incrementRetryCount(eventId: String) {
        accidentEventDAO.incrementRetryCount(eventId)
    }
}