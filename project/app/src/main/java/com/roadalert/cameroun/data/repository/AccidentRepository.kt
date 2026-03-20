package com.roadalert.cameroun.data.repository

import com.roadalert.cameroun.data.db.dao.AccidentEventDAO
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.AlertStatus
import kotlinx.coroutines.flow.Flow

class AccidentRepository(
    private val accidentEventDAO: AccidentEventDAO
) {

    // ── Sauvegarde d'un nouvel accident ───────────────────

    suspend fun saveAccidentEvent(event: AccidentEvent) {
        accidentEventDAO.insertAccidentEvent(event)
    }

    // ── Mise à jour complète d'un accident ────────────────

    suspend fun updateAccidentEvent(event: AccidentEvent) {
        accidentEventDAO.updateAccidentEvent(event)
    }

    // ── Observer un accident en temps réel ───────────────
    // AlertSentViewModel l'utilise pour mettre à jour l'UI

    fun observeAccidentEvent(
        eventId: String
    ): Flow<AccidentEvent?> {
        return accidentEventDAO.getAccidentEventById(eventId)
    }

    // ── Lecture synchrone — RetryWorker ───────────────────

    suspend fun getAccidentEventById(
        eventId: String
    ): AccidentEvent? {
        return accidentEventDAO.getAccidentEventByIdSync(eventId)
    }

    // ── Tous les accidents — HistoryActivity Sprint 5 ─────

    fun getAllAccidentEvents(
        userId: String
    ): Flow<List<AccidentEvent>> {
        return accidentEventDAO.getAllAccidentEvents(userId)
    }

    // ── Accidents en attente de retry ─────────────────────

    suspend fun getPendingRetryEvents(): List<AccidentEvent> {
        return accidentEventDAO.getPendingRetryEvents()
    }

    // ── Mise à jour statut global ─────────────────────────

    suspend fun updateAlertStatus(
        eventId: String,
        status: AlertStatus
    ) {
        accidentEventDAO.updateAlertStatus(
            eventId,
            status.name
        )
    }

    // ── Mise à jour position GPS ──────────────────────────

    suspend fun updateLocation(
        eventId: String,
        latitude: Double,
        longitude: Double,
        isApproximate: Boolean
    ) {
        accidentEventDAO.updateLocation(
            eventId,
            latitude,
            longitude,
            isApproximate
        )
    }

    // ── Incrémenter retry count ───────────────────────────

    suspend fun incrementRetryCount(eventId: String) {
        accidentEventDAO.incrementRetryCount(eventId)
    }

    // ── Nombre total d'accidents ──────────────────────────

    suspend fun getAccidentCount(userId: String): Int {
        return accidentEventDAO.getAccidentCount(userId)
    }
}