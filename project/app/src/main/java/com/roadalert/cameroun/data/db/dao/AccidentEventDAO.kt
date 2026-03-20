package com.roadalert.cameroun.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AccidentEventDAO {

    // ── Insertion ─────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccidentEvent(event: AccidentEvent)

    // ── Mise à jour ───────────────────────────────────────

    @Update
    suspend fun updateAccidentEvent(event: AccidentEvent)

    // ── Lecture par ID ────────────────────────────────────
    // Utilisé par AlertSentActivity pour observer en temps réel

    @Query("SELECT * FROM accident_event WHERE id = :eventId")
    fun getAccidentEventById(
        eventId: String
    ): Flow<AccidentEvent?>

    // ── Lecture synchrone par ID ──────────────────────────
    // Utilisé par RetryWorker

    @Query("SELECT * FROM accident_event WHERE id = :eventId")
    suspend fun getAccidentEventByIdSync(
        eventId: String
    ): AccidentEvent?

    // ── Tous les accidents — ordre chronologique inverse ──
    // Utilisé par HistoryActivity Sprint 5

    @Query(
        "SELECT * FROM accident_event " +
                "WHERE userId = :userId " +
                "ORDER BY timestamp DESC"
    )
    fun getAllAccidentEvents(
        userId: String
    ): Flow<List<AccidentEvent>>

    // ── Accidents en attente de retry ─────────────────────
    // Utilisé par RetryWorker

    @Query(
        "SELECT * FROM accident_event " +
                "WHERE alertStatus = 'PENDING_RETRY' " +
                "AND retryCount < 3 " +
                "ORDER BY timestamp ASC"
    )
    suspend fun getPendingRetryEvents(): List<AccidentEvent>

    // ── Mise à jour statut global ─────────────────────────

    @Query(
        "UPDATE accident_event " +
                "SET alertStatus = :status " +
                "WHERE id = :eventId"
    )
    suspend fun updateAlertStatus(
        eventId: String,
        status: String
    )

    // ── Mise à jour position GPS ──────────────────────────

    @Query(
        "UPDATE accident_event " +
                "SET latitude = :lat, " +
                "longitude = :lng, " +
                "isPositionApproximate = :isApprox " +
                "WHERE id = :eventId"
    )
    suspend fun updateLocation(
        eventId: String,
        lat: Double,
        lng: Double,
        isApprox: Boolean
    )

    // ── Incrémenter le compteur retry ─────────────────────

    @Query(
        "UPDATE accident_event " +
                "SET retryCount = retryCount + 1 " +
                "WHERE id = :eventId"
    )
    suspend fun incrementRetryCount(eventId: String)

    // ── Compter les accidents ─────────────────────────────
    // Utilisé par HistoryActivity pour état vide

    @Query(
        "SELECT COUNT(*) FROM accident_event " +
                "WHERE userId = :userId"
    )
    suspend fun getAccidentCount(userId: String): Int
}