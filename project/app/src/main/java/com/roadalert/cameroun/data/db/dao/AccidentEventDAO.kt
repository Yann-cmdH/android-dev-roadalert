package com.roadalert.cameroun.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.AlertStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AccidentEventDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AccidentEvent)

    @Update
    suspend fun updateEvent(event: AccidentEvent)

    @Query("SELECT * FROM accident_event ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<AccidentEvent>>

    @Query("SELECT * FROM accident_event WHERE id = :eventId")
    suspend fun getEventById(eventId: String): AccidentEvent?

    @Query("UPDATE accident_event SET alertStatus = :status WHERE id = :eventId")
    suspend fun updateStatus(eventId: String, status: AlertStatus)

    @Query("SELECT * FROM accident_event WHERE alertStatus = 'PENDING_RETRY'")
    suspend fun getPendingRetries(): List<AccidentEvent>

    @Query("UPDATE accident_event SET retryCount = retryCount + 1 WHERE id = :eventId")
    suspend fun incrementRetryCount(eventId: String)
}