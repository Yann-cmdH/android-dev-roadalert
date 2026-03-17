package com.roadalert.cameroun.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadalert.cameroun.data.db.entity.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)

    @Query("SELECT * FROM emergency_contact WHERE userId = :userId ORDER BY priority ASC")
    fun getContactsByUser(userId: String): Flow<List<EmergencyContact>>

    @Query("SELECT COUNT(*) FROM emergency_contact WHERE userId = :userId")
    suspend fun getContactCount(userId: String): Int

    @Query("DELETE FROM emergency_contact WHERE id = :contactId")
    suspend fun deleteById(contactId: String)
}