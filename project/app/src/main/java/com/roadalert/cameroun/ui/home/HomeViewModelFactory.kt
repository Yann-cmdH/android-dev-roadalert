package com.roadalert.cameroun.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.UserProfileRepository

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = AppDatabase.getInstance(context)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                UserProfileRepository(
                    db.userDAO(),
                    db.emergencyContactDAO()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}