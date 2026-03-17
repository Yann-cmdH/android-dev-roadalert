package com.roadalert.cameroun.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.UserProfileRepository

class ProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val db = AppDatabase.getInstance(context)
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                UserProfileRepository(
                    db.userDAO(),
                    db.emergencyContactDAO()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}