package com.roadalert.cameroun.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository

class HistoryViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                HistoryViewModel::class.java
            )) {
            val db = AppDatabase.getInstance(context)
            val accidentRepository = AccidentRepository(
                db.accidentEventDAO()
            )
            val userProfileRepository = UserProfileRepository(
                db,
                db.userDAO(),
                db.emergencyContactDAO()
            )
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(
                accidentRepository = accidentRepository,
                userProfileRepository = userProfileRepository
            ) as T
        }
        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}
