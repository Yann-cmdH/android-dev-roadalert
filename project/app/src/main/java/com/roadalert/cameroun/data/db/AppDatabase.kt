package com.roadalert.cameroun.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.roadalert.cameroun.data.db.dao.AccidentEventDAO
import com.roadalert.cameroun.data.db.dao.EmergencyContactDAO
import com.roadalert.cameroun.data.db.dao.UserDAO
import com.roadalert.cameroun.data.db.entity.AccidentEvent
import com.roadalert.cameroun.data.db.entity.EmergencyContact
import com.roadalert.cameroun.data.db.entity.User

@Database(
    entities = [
        User::class,
        EmergencyContact::class,
        AccidentEvent::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDAO(): UserDAO
    abstract fun emergencyContactDAO(): EmergencyContactDAO
    abstract fun accidentEventDAO(): AccidentEventDAO

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roadalert.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
