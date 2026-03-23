package com.roadalert.cameroun.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.util.AppSettings

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val db = AppDatabase.getInstance(context)
            val userProfileRepository = UserProfileRepository(
                db,
                db.userDAO(),
                db.emergencyContactDAO()
            )
            val accidentRepository = AccidentRepository(
                db.accidentEventDAO()
            )
            val appSettings = AppSettings(context)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                application = context.applicationContext as android.app.Application,
                userProfileRepository = userProfileRepository,
                accidentRepository = accidentRepository,
                appSettings = appSettings,
                database = db
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
