package com.roadalert.cameroun.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadalert.cameroun.data.db.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM user LIMIT 1")
    fun getUser(): Flow<User?>

    @Query("SELECT COUNT(*) FROM user")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUserSync(): User?
}