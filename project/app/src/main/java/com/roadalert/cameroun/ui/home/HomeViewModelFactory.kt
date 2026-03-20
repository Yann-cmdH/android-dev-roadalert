package com.roadalert.cameroun.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.AccidentRepository
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.detection.AlertManager

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = AppDatabase.getInstance(context)

            val userProfileRepository = UserProfileRepository(
                db.userDAO(),
                db.emergencyContactDAO()
            )

            val accidentRepository = AccidentRepository(
                db.accidentEventDAO()
            )

            val alertManager = AlertManager(
                context = context,
                accidentRepository = accidentRepository,
                userProfileRepository = userProfileRepository
            )

            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                application = context.applicationContext
                        as android.app.Application,
                userProfileRepository = userProfileRepository,
                alertManager = alertManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}